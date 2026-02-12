import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager

class BluetoothDeviceScanner(private val context: Context) {
    
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val discoveredDevices = mutableListOf<BluetoothDevice>()
    private var scanCallback: ((List<BluetoothDevice>) -> Unit)? = null
    
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = 
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    device?.let {
                        if (it !in discoveredDevices) {
                            discoveredDevices.add(it)
                            scanCallback?.invoke(discoveredDevices.toList())
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    stopScanning()
                }
            }
        }
    }
    
    /**
     * Check if Bluetooth is available on this device
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null
    }
    
    /**
     * Check if Bluetooth is currently enabled
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * Start scanning for Bluetooth devices
     * @param onDevicesFound Callback that receives list of discovered devices
     */
    fun startScanning(onDevicesFound: (List<BluetoothDevice>) -> Unit) {
        if (!isBluetoothAvailable()) {
            throw IllegalStateException("Bluetooth is not available on this device")
        }
        
        if (!isBluetoothEnabled()) {
            throw IllegalStateException("Bluetooth is not enabled")
        }
        
        // Check for required permissions
        if (!hasRequiredPermissions()) {
            throw SecurityException("Required Bluetooth permissions are not granted")
        }
        
        discoveredDevices.clear()
        scanCallback = onDevicesFound
        
        // Register broadcast receiver
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        
        ContextCompat.registerReceiver(
            context,
            discoveryReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        
        // Start discovery
        bluetoothAdapter?.startDiscovery()
    }
    
    /**
     * Stop scanning for Bluetooth devices
     */
    fun stopScanning() {
        if (hasRequiredPermissions()) {
            bluetoothAdapter?.cancelDiscovery()
        }
        context.unregisterReceiver(discoveryReceiver)
        scanCallback = null
    }
    
    /**
     * Get paired (bonded) devices
     */
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasRequiredPermissions()) {
            return emptyList()
        }
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }
    
    /**
     * Check if all required permissions are granted
     */
    private fun hasRequiredPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            listOf(Manifest.permission.BLUETOOTH)
        }
        
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get device name safely
     */
    fun getDeviceName(device: BluetoothDevice): String {
        return if (hasRequiredPermissions()) {
            device.name ?: device.address
        } else {
            device.address
        }
    }
    
    /**
     * Connect to a specific Bluetooth device
     */
    fun connectToDevice(device: BluetoothDevice): Boolean {
        return if (hasRequiredPermissions()) {
            try {
                bluetoothAdapter?.cancelDiscovery()
                device.createRfcommSocketToServiceRecord(
                    java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
                )
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } else {
            false
        }
    }
}