package com.cyclistassist.display

import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity(), BluetoothMenuInterface {

    private val bluetoothDeviceArray = ArrayList<BluetoothDevice>()
    private val bluetoothStatus: TextView by lazy { findViewById<TextView>(R.id.status) }
    private val leDeviceListAdapter : BluetoothConnectionRecyclerAdapter by lazy { BluetoothConnectionRecyclerAdapter(bluetoothDeviceArray, this) }
    lateinit var bluetoothScanner: BluetoothScanner

    val scanButton : Button by lazy {findViewById<Button>(R.id.btn_scan)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
        bluetoothScanner = BluetoothScanner(this, bluetoothAdapter, this)

        // Bluetooth Permissions
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
        }
        requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), 1)

        val recycler: RecyclerView = findViewById(R.id.recyclerView)
        recycler.adapter = leDeviceListAdapter

        val llm = LinearLayoutManager(this)
        llm.orientation = RecyclerView.VERTICAL
        recycler.layoutManager = llm

        scanButton.setOnClickListener {
            leDeviceListAdapter.clearDevices()
            bluetoothScanner.scanLeDevice()
        }

        val settingsButton : Button = findViewById(R.id.btn_settings)
        settingsButton.setOnClickListener {
            val intent = Intent(this, Settings::class.java)

            startActivity(intent)
        }
    }

    override fun setStatus(message: String) {
        runOnUiThread {
            bluetoothStatus.text = message
        }
    }

    override fun scanMessage(message: String) {
        scanButton.text = message
    }

    override fun foundDevice(device: BluetoothDevice) {
        leDeviceListAdapter.addDevice(device)
    }

    override fun connectDevice(device: BluetoothDevice) {
        if (device.name == "CYCLISTASSIST") {
            val intent = Intent(this, Control::class.java)
            intent.putExtra("bluetoothDevice", device)

            startActivity(intent)
        } else {
            setStatus("Invalid Device")
        }
    }

    override fun disconnectDevice() {
        bluetoothScanner.disconnectDevice()
    }

    override fun checkConnection(): Boolean {
        return bluetoothScanner.CheckConnection()
    }

    override fun checkDevice(device: BluetoothDevice) : Boolean {
        return bluetoothScanner.checkDevice(device)
    }

    override fun NotifyChange() {
        leDeviceListAdapter.notifyDataSetChanged()
    }
}

class BluetoothConnectionRecyclerAdapter(private val bluetoothDeviceArray:ArrayList<BluetoothDevice>, private val bluetoothMenuInterface: BluetoothMenuInterface) : RecyclerView.Adapter<BluetoothConnectionRecyclerAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameView: TextView = view.findViewById(R.id.device_name)
        val addressView: TextView = view.findViewById(R.id.device_address)
        val connectButton: Button = view.findViewById(R.id.btn_connect)
        val disconnectButton: Button = view.findViewById(R.id.btn_disconnect)
    }

    fun addDevice(device: BluetoothDevice){
        if (!bluetoothDeviceArray.contains(device)) {
            bluetoothDeviceArray.add(device)
            this.notifyDataSetChanged()
        }
    }

    fun clearDevices() {
        bluetoothDeviceArray.clear()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.listitem_device, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.nameView.text = bluetoothDeviceArray[position].name
        viewHolder.addressView.text = bluetoothDeviceArray[position].address

        if (bluetoothMenuInterface.checkDevice(bluetoothDeviceArray[position])) {
            viewHolder.connectButton.visibility = View.INVISIBLE
            viewHolder.disconnectButton.visibility = View.VISIBLE
        } else {
            viewHolder.connectButton.visibility = View.VISIBLE
            viewHolder.disconnectButton.visibility = View.INVISIBLE
        }

        viewHolder.connectButton.setOnClickListener {
            if (!bluetoothMenuInterface.checkConnection()){
                bluetoothMenuInterface.disconnectDevice()
            }

            bluetoothMenuInterface.connectDevice(bluetoothDeviceArray[position])
        }

        viewHolder.disconnectButton.setOnClickListener {
            bluetoothMenuInterface.disconnectDevice()
        }
    }

    override fun getItemCount() = bluetoothDeviceArray.size

}
