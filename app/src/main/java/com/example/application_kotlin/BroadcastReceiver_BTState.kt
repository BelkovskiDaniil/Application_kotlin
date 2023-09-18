package com.example.application_kotlin

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BroadcastReceiverBTState : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_OFF -> {}
                BluetoothAdapter.STATE_TURNING_OFF -> {}
                BluetoothAdapter.STATE_ON -> {}
                BluetoothAdapter.STATE_TURNING_ON -> {}
            }
        }
    }
}