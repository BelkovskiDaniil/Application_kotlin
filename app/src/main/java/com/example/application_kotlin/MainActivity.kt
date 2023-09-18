package com.example.application_kotlin

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.Arrays
import java.util.Timer


class MainActivity : AppCompatActivity(){
    private var listView: ListView? = null
    private var mBTStateUpdateReceiver: BroadcastReceiverBTState? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var receiver: BroadcastReceiver? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var ACTION_GATT_SERVICES = "com.example.bluetooth.le.ACTION_GATT_SERVICES"
    private var gattLocal: BluetoothGatt? = null
    var characteristic0: BluetoothGattCharacteristic? = null
    var characteristic1: BluetoothGattCharacteristic? = null
    var characteristic2: BluetoothGattCharacteristic? = null
    var characteristic3: BluetoothGattCharacteristic? = null
    private var address: String? = null
    private var connectionTimeoutTimer: Timer? = null
    private var descriptorWritten = 0
    private var onWork = 0
    private val REQUEST_ENABLE_BLUETOOTH = 1
    private var bluetoothMy = false
    private val handlerKiller = Handler()
    var isScanning = false
    var type_of_button = 0

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_PRIVILEGED,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.VIBRATE
        )

        val permissionsToRequest = mutableListOf<String>()

        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Utils.toast(applicationContext, "BLE not supported")
            finish()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkPermissions()
        }
        listView = ListView(this)
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        bluetoothMy = bluetoothAdapter.isEnabled
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(
            mBTStateUpdateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (BluetoothAdapter.ACTION_STATE_CHANGED == intent?.action) {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_OFF -> {
                            val builder = AlertDialog.Builder(this@MainActivity)

                            // Устанавливаем заголовок и сообщение для диалогового окна
                            builder.setTitle("Dialog window")
                            builder.setMessage("Bluetooth is turned off")

                            // Устанавливаем положительную кнопку и обработчик её нажатия
                            builder.setPositiveButton("OK"
                            ) { _, _ ->
                            }

                            // Создаем и показываем AlertDialog
                            val dialog = builder.create()
                            dialog.show()
                            bluetoothMy = false
                            onWork = 0
                            runOnUiThread {
                                val textView = findViewById<View>(R.id.textView) as TextView
                                textView.text = ""
                            }
                        }

                        BluetoothAdapter.STATE_ON -> {
                            val builder = AlertDialog.Builder(this@MainActivity)

                            // Устанавливаем заголовок и сообщение для диалогового окна
                            builder.setTitle("Dialog window")
                            builder.setMessage("Bluetooth is turned on")

                            // Устанавливаем положительную кнопку и обработчик её нажатия
                            builder.setPositiveButton("OK"
                            ) { _, _ ->
                            }

                            // Создаем и показываем AlertDialog
                            val dialog = builder.create()
                            dialog.show()
                            bluetoothMy = true
                        }
                    }
                }
            }
        }
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(receiver, filter)
    }

    override fun onPause() {
        super.onPause()
        //unregisterReceiver(receiver)
    }

    override fun onStop() {
        super.onStop()
        //unregisterReceiver(mBTStateUpdateReceiver)
    }

    public override fun onDestroy() {
        super.onDestroy()
    }


    private fun broadcastUpdate(action: String) {
        val intent = Intent(ACTION_GATT_SERVICES)
        Log.d("My", action)
        sendBroadcast(intent)
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceAddress: String) {
        val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        if (device != null) {
            try {
                bluetoothGatt = device.connectGatt(this, false, mGattCallback)
            }
            catch (e: IOException ) {
                Log.e("My", "create() failed", e);
            }
        }
    }

    @SuppressLint("HandlerLeak")
    var handlerConnected: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            connectionTimeoutTimer?.cancel()
        }
    }

    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    broadcastUpdate("connected")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return
                        }
                    } else {
                        if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                            return
                        }
                    }
                    handlerConnected.sendEmptyMessage(0)
                    gatt.requestMtu(517)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    broadcastUpdate("disconnected")
                    onWork = 0
                    gatt.close()
                }
            } else {
                broadcastUpdate("crashed")
                onWork = 0
                gatt.close()
                isScanning = false
                runOnUiThread {
                    val textView = findViewById<View>(R.id.textView) as TextView
                    textView.text = ""
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            gattLocal = gatt
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate("discovered")
                val services = gatt.services
                val service = services[2]
                val characteristics = service.characteristics
                val characteristic = characteristics[3]
                characteristic0 = characteristics[0]
                characteristic1 = characteristics[1]
                characteristic2 = characteristics[2]
                characteristic3 = characteristics[3]
                //val value: ByteArray;
                val value: ByteArray
                val properties = characteristic.properties

                value =
                    if (properties != 0) {
                        BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    } else {
                        return
                    }
                val descriptors = characteristic3!!.descriptors
                val descriptor =
                    descriptors[0] // UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                } else {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    broadcastUpdate("writeDescriptor " + gatt.writeDescriptor(descriptor, value))
                } else {
                    if (!descriptor.setValue(value)) broadcastUpdate("descriptor.setValue error")
                    if (!gatt.writeDescriptor(descriptor)) broadcastUpdate("gatt.writeDescriptor error")
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
        }

        // if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
        @Deprecated("Deprecated in Java", ReplaceWith(
            "super.onCharacteristicChanged(gatt, characteristic)",
            "android.bluetooth.BluetoothGattCallback"
        )
        )
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
        }

        @SuppressLint("HandlerLeak")
        var handlerReaderVelocity: Handler = object : Handler(Looper.getMainLooper()) {
            @SuppressLint("SetTextI18n")
            override fun handleMessage(msg: Message) {
                descriptorWritten = 0
                var dataLocal = byteArrayOf(1, 0)
                commands(dataLocal)
                sleepingLong()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                } else {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                }
                gattLocal!!.readCharacteristic(characteristic0)
                sleeping()
                val value = characteristic0!!.value
                try {
                    val a = value.isEmpty()
                    Log.d("My", Arrays.toString(value))
                    sleeping()
                    dataLocal = byteArrayOf(2, 0)
                    commands(dataLocal)
                    val textViewVelocity =
                        findViewById<View>(R.id.textView_velocity) as TextView
                    textViewVelocity.text = "Velocity: " + Utils.getVelocityBasedLong(
                        Utils.hexToString(value)
                    ).replace(',', '.')
                    //gattLocal!!.close()
                    gattLocal!!.disconnect()
                    val textView = findViewById<View>(R.id.textView) as TextView
                    textView.text = ""
                }
                catch (e: Exception) {
                    Log.d("My", "Fall")
                    gattLocal!!.disconnect()
                    val textView = findViewById<View>(R.id.textView) as TextView
                    textView.text = ""
                    val builder = AlertDialog.Builder(this@MainActivity)

                    // Устанавливаем заголовок и сообщение для диалогового окна
                    builder.setTitle("Dialog window")
                    builder.setMessage("Bring the device closer")

                    // Устанавливаем положительную кнопку и обработчик её нажатия
                    builder.setPositiveButton("OK"
                    ) { _, _ ->
                    }

                    // Создаем и показываем AlertDialog
                    val dialog = builder.create()
                    dialog.show()
                }
            }
        }

        @SuppressLint("HandlerLeak")
        var handlerReaderAcceleration: Handler = object : Handler(Looper.getMainLooper()) {
            @SuppressLint("SetTextI18n")
            override fun handleMessage(msg: Message) {
                descriptorWritten = 0
                var dataLocal = byteArrayOf(1, 0)
                commands(dataLocal)
                sleepingLong()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                } else {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                }
                gattLocal!!.readCharacteristic(characteristic0)
                sleeping()
                val value = characteristic0!!.value
                try {
                    val a = value.isEmpty()
                    Log.d("My", Arrays.toString(value))
                    sleeping()
                    dataLocal = byteArrayOf(2, 0)
                    commands(dataLocal)
                    val textViewAcceleration =
                        findViewById<View>(R.id.textView_acceleration) as TextView
                    textViewAcceleration.text = "Acceleration: " + Utils.getAccelerationBased(
                        Utils.hexToString(value)
                    ).replace(',', '.')
                    //gattLocal!!.close()
                    gattLocal!!.disconnect()
                    val textView = findViewById<View>(R.id.textView) as TextView
                    textView.text = ""
                }
                catch (_: Exception) {
                    Log.d("My", "Fall")
                    gattLocal!!.disconnect()
                    val textView = findViewById<View>(R.id.textView) as TextView
                    textView.text = ""
                    val builder = AlertDialog.Builder(this@MainActivity)

                    // Устанавливаем заголовок и сообщение для диалогового окна
                    builder.setTitle("Dialog window")
                    builder.setMessage("Bring the device closer")

                    // Устанавливаем положительную кнопку и обработчик её нажатия
                    builder.setPositiveButton("OK"
                    ) { _, _ ->
                    }

                    // Создаем и показываем AlertDialog
                    val dialog = builder.create()
                    dialog.show()
                }
            }
        }

        @SuppressLint("HandlerLeak")
        var handlerReaderExcess: Handler = object : Handler(Looper.getMainLooper()) {
            @SuppressLint("SetTextI18n")
            override fun handleMessage(msg: Message) {
                descriptorWritten = 0
                var dataLocal = byteArrayOf(1, 0)
                commands(dataLocal)
                sleepingLong()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                } else {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                }
                gattLocal!!.readCharacteristic(characteristic0)
                sleeping()
                val value = characteristic0!!.value
                try {
                    val a = value.isEmpty()
                    Log.d("My", Arrays.toString(value))
                    sleeping()
                    dataLocal = byteArrayOf(2, 0)
                    commands(dataLocal)
                    val textViewExcess =
                        findViewById<View>(R.id.textView_excess) as TextView
                    textViewExcess.text = "Excess: " + Utils.getExcess(
                        Utils.hexToString(value)
                    ).replace(',', '.')
                    //gattLocal!!.close()
                    gattLocal!!.disconnect()
                    val textView = findViewById<View>(R.id.textView) as TextView
                    textView.text = ""
                }
                catch (_: Exception) {
                    Log.d("My", "Fall")
                    gattLocal!!.disconnect()
                    val textView = findViewById<View>(R.id.textView) as TextView
                    textView.text = ""
                    val builder = AlertDialog.Builder(this@MainActivity)

                    // Устанавливаем заголовок и сообщение для диалогового окна
                    builder.setTitle("Dialog window")
                    builder.setMessage("Bring the device closer")

                    // Устанавливаем положительную кнопку и обработчик её нажатия
                    builder.setPositiveButton("OK"
                    ) { _, _ ->
                    }

                    // Создаем и показываем AlertDialog
                    val dialog = builder.create()
                    dialog.show()
                }
            }
        }

        @SuppressLint("HandlerLeak")
        var handlerReaderTemperature: Handler = object : Handler(Looper.getMainLooper()) {
            @SuppressLint("SetTextI18n")
            override fun handleMessage(msg: Message) {
                descriptorWritten = 0
                var dataLocal = byteArrayOf(1, 0)
                commands(dataLocal)
                sleepingLong()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                } else {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                }
                gattLocal!!.readCharacteristic(characteristic0)
                sleeping()
                val value = characteristic0!!.value
                try {
                    val a = value.isEmpty()
                    Log.d("My", Arrays.toString(value))
                    sleeping()
                    dataLocal = byteArrayOf(2, 0)
                    commands(dataLocal)
                    val textViewTemperature =
                        findViewById<View>(R.id.textView_temperature) as TextView
                    textViewTemperature.text = "Temperature: " + Utils.getTemperature(
                        Utils.hexToString(value)
                    ) + "°"
                    //gattLocal!!.close()
                    gattLocal!!.disconnect()
                    val textView = findViewById<View>(R.id.textView) as TextView
                    textView.text = ""
                }
                catch (_: Exception) {
                    Log.d("My", "Fall")
                    gattLocal!!.disconnect()
                    val textView = findViewById<View>(R.id.textView) as TextView
                    textView.text = ""
                    val builder = AlertDialog.Builder(this@MainActivity)

                    // Устанавливаем заголовок и сообщение для диалогового окна
                    builder.setTitle("Dialog window")
                    builder.setMessage("Bring the device closer")

                    // Устанавливаем положительную кнопку и обработчик её нажатия
                    builder.setPositiveButton("OK"
                    ) { _, _ ->
                    }

                    // Создаем и показываем AlertDialog
                    val dialog = builder.create()
                    dialog.show()
                }
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate("descriptor written")
                descriptorWritten = 1
                if (type_of_button == 1) handlerReaderVelocity.sendEmptyMessage(0)
                else if (type_of_button == 2) handlerReaderAcceleration.sendEmptyMessage(0)
                else if (type_of_button == 3) handlerReaderExcess.sendEmptyMessage(0)
                else if (type_of_button == 4) handlerReaderTemperature.sendEmptyMessage(0)
            } else broadcastUpdate("descriptor write error")
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate("MTU changed")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                } else {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                }
                gatt.discoverServices()
            } else broadcastUpdate("MTU change error")
        }
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            } else {
                if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }
            val gatt = result.device.connectGatt(applicationContext, false, mGattCallback, BluetoothDevice.TRANSPORT_AUTO)
        }
    }

    private fun commands(data: ByteArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        } else {
            if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gattLocal!!.writeCharacteristic(
                characteristic1!!,
                data,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
        } else {
            characteristic1!!.value = data
            characteristic1!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            gattLocal!!.writeCharacteristic(characteristic1)
        }
    }

    private fun sleeping() {
        try {
            Thread.sleep(250)
        } catch (e: InterruptedException) {
            Log.d("My", "To short time")
        }
    }

    private fun sleepingLong() {
        try {
            Thread.sleep(700)
        } catch (e: InterruptedException) {
            Log.d("My", "To short time")
        }
    }

    private fun sleepingVeryLong() {
        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            Log.d("My", "To short time")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            bluetoothMy = resultCode == RESULT_OK
        }
    }

    @SuppressLint("SetTextI18n")
    fun onVelocityClick(view: View) {
        if (onWork == 0) {
            sleepingLong()
            if (!bluetoothMy) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                } else {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                }
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
            }
            else {
                type_of_button = 1
                val textView = findViewById<View>(R.id.textView) as TextView
                textView.text = "Trying to connect..."
                onWork = 1
                val editTextAddress = findViewById<View>(R.id.editText_address) as EditText
                address = editTextAddress.text.toString()
                //val myRunnable = MyRunnableVelocity(this)
                isScanning = true
                //handlerKiller.post(myRunnable)
                try {
                    //address?.let { connectToDeviceDialog(it) }
                    connectToDevice(address!!)
                } catch (e: Exception) {
                    textView.text = ""
                    //handlerKiller.removeCallbacks(myRunnable)
                    onWork = 0
                    isScanning = false
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun onAccelerationClick(view: View) {
        if (onWork == 0) {
            sleepingLong()
            if (!bluetoothMy) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                } else {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                }
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
            }
            else {
                type_of_button = 2
                onWork = 1
                isScanning = true
                val textView = findViewById<View>(R.id.textView) as TextView
                textView.text = "Trying to connect..."
                val editTextAddress = findViewById<View>(R.id.editText_address) as EditText
                address = editTextAddress.text.toString()
                try {
                    connectToDevice(address!!)
                }
                catch (e: Exception) {
                    textView.text = ""
                    onWork = 0
                    isScanning = false
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun onExcessClick(view: View) {
        if (onWork == 0) {
            sleepingLong()
            if (!bluetoothMy) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                } else {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                }
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
            }
            else {
                type_of_button = 3
                onWork = 1
                isScanning = true
                val textView = findViewById<View>(R.id.textView) as TextView
                textView.text = "Trying to connect..."
                val editTextAddress = findViewById<View>(R.id.editText_address) as EditText
                address = editTextAddress.text.toString()
                try {
                    connectToDevice(address!!)
                }
                catch (e: Exception) {
                    textView.text = ""
                    onWork = 0
                    isScanning = false
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun onTemperatureClick(view: View) {
        if (onWork == 0) {
            sleepingLong()
            if (!bluetoothMy) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                } else {
                    if (applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                }
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
            }
            else {
                type_of_button = 4
                onWork = 1
                isScanning = true
                val textView = findViewById<View>(R.id.textView) as TextView
                textView.text = "Trying to connect..."
                val editTextAddress = findViewById<View>(R.id.editText_address) as EditText
                address = editTextAddress.text.toString()
                try {
                    connectToDevice(address!!)
                }
                catch (e: Exception) {
                    textView.text = ""
                    onWork = 0
                    isScanning = false
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
    }
}