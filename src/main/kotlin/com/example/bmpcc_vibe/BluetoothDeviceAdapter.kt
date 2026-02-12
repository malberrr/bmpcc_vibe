import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BluetoothDeviceAdapter(
    private var devices: List<BluetoothDevice>,
    private val scanner: BluetoothDeviceScanner,
    private val onDeviceClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder>() {

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val deviceNameTextView: TextView = itemView.findViewById(R.id.deviceName)
        private val deviceAddressTextView: TextView = itemView.findViewById(R.id.deviceAddress)

        fun bind(device: BluetoothDevice) {
            val deviceName = scanner.getDeviceName(device)
            deviceNameTextView.text = deviceName
            deviceAddressTextView.text = device.address

            itemView.setOnClickListener {
                onDeviceClick(device)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_list_item, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size

    fun updateDevices(newDevices: List<BluetoothDevice>) {
        devices = newDevices
        notifyDataSetChanged()
    }
}