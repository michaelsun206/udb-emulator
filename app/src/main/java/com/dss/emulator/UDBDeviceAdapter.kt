package com.dss.emulator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dss.emulator.udb.R

class UDBDeviceAdapter(
    private val onDeviceSelected: (UDBDevice) -> Unit
) : RecyclerView.Adapter<UDBDeviceAdapter.DeviceViewHolder>() {

    private val devices = mutableListOf<UDBDevice>()

    fun addDevice(device: UDBDevice) {
        devices.add(device)
        notifyItemInserted(devices.size - 1)
    }

    fun updateDeviceList(newDevices: List<UDBDevice>) {
        devices.clear()
        devices.addAll(newDevices)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view, onDeviceSelected)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size

    class DeviceViewHolder(
        itemView: View, private val onDeviceSelected: (UDBDevice) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val deviceName: TextView = itemView.findViewById(R.id.deviceName)
        private val deviceAddress: TextView = itemView.findViewById(R.id.deviceAddress)
        private val deviceRssi: TextView = itemView.findViewById(R.id.deviceRssi)
        fun bind(device: UDBDevice) {
            deviceName.text = device.name
            deviceAddress.text = device.address
            deviceRssi.text = "RSSI: ${device.rssi}"
            itemView.setOnClickListener {
                onDeviceSelected(device)
            }
        }
    }
}