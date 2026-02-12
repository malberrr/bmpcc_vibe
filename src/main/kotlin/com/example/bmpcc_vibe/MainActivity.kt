import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothScanner: BluetoothDeviceScanner
    private lateinit var deviceAdapter: BluetoothDeviceAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var scanButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var pairedDevicesButton: Button

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startScanning()
        } else {
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothScanner = BluetoothDeviceScanner(this)
        initializeUI()
        setupRecyclerView()
        checkBluetoothStatus()
    }

    private fun initializeUI() {
        scanButton = findViewById(R.id.scanButton)
        stopButton = findViewById(R.id.stopButton)
        statusTextView = findViewById(R.id.statusTextView)
        recyclerView = findViewById(R.id.devicesRecyclerView)
        pairedDevicesButton = findViewById(R.id.pairedDevicesButton)

        scanButton.setOnClickListener {
            requestPermissionsAndScan()
        }

        stopButton.setOnClickListener {
            bluetoothScanner.stopScanning()
            stopButton.isEnabled = false
            scanButton.isEnabled = true
            statusTextView.text = "Scanning stopped"
        }

        pairedDevicesButton.setOnClickListener {
            showPairedDevices()
        }
    }

    private fun setupRecyclerView() {
        deviceAdapter = BluetoothDeviceAdapter(emptyList(), bluetoothScanner) { device ->
            onDeviceSelected(device)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = deviceAdapter
    }

    private fun checkBluetoothStatus() {
        when {
            !bluetoothScanner.isBluetoothAvailable() -> {
                statusTextView.text = "Bluetooth not available"
                scanButton.isEnabled = false
            }
            !bluetoothScanner.isBluetoothEnabled() -> {
                statusTextView.text = "Bluetooth disabled"
                scanButton.isEnabled = false
            }
            else -> {
                statusTextView.text = "Ready to scan"
                scanButton.isEnabled = true
            }
        }
    }

    private fun requestPermissionsAndScan() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val needsPermissions = permissions.any { permission ->
            ActivityCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (needsPermissions) {
            permissionLauncher.launch(permissions)
        } else {
            startScanning()
        }
    }

    private fun startScanning() {
        try {
            statusTextView.text = "Scanning for devices..."
            scanButton.isEnabled = false
            stopButton.isEnabled = true
            deviceAdapter.updateDevices(emptyList())

            bluetoothScanner.startScanning { devices ->
                runOnUiThread {
                    deviceAdapter.updateDevices(devices)
                    statusTextView.text = "Found ${'$'}{devices.size} device(s)"
                }
            }
        } catch (e: SecurityException) {
            statusTextView.text = "Permission error: ${'$'}{e.message}"
            scanButton.isEnabled = true
            stopButton.isEnabled = false
        } catch (e: IllegalStateException) {
            statusTextView.text = "Error: ${'$'}{e.message}"
            scanButton.isEnabled = true
            stopButton.isEnabled = false
        }
    }

    private fun showPairedDevices() {
        try {
            val pairedDevices = bluetoothScanner.getPairedDevices()
            deviceAdapter.updateDevices(pairedDevices)
            statusTextView.text = "Showing ${'$'}{pairedDevices.size} paired device(s)"
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${'$'}{e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onDeviceSelected(device: BluetoothDevice) {
        val deviceName = bluetoothScanner.getDeviceName(device)
        val deviceAddress = device.address
        
        Toast.makeText(
            this,
            "Selected: ${'$'}{deviceName} (${'$'}{deviceAddress})",
            Toast.LENGTH_SHORT
        ).show()

        // Optionally attempt to connect
        if (bluetoothScanner.connectToDevice(device)) {
            statusTextView.text = "Connecting to ${'$'}{deviceName}..."
        } else {
            statusTextView.text = "Failed to connect"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothScanner.stopScanning()
    }
}