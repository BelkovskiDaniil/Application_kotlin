package com.example.application_kotlin

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.widget.Toast

object Utils {

    fun hexToString(data: ByteArray?): String {
        try {
            val sb = StringBuilder(data!!.size)
            for (byteChar in data) {
                sb.append(String.format("%02X ", byteChar))
            }
            return sb.toString()
        }
        catch (e: Exception) {
            Log.d("My", "Empty string")
            return "aboba"
        }
    }

    fun toast(context: Context?, string: String?) {
        val toast = Toast.makeText(context, string, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.CENTER or Gravity.BOTTOM, 0, 0)
        toast.show()
    }

    fun getTemperature(str: String?): String {
        val temperature: String
        val lowByte = str!!.substring(39, 41)
        val highByte = str.substring(42, 44)
        val forHex = highByte + lowByte
        val decimal = Integer.valueOf(forHex, 16).toShort().toInt()
        temperature = (decimal / 100).toString()
        return temperature
    }

    fun getExcess(str: String?): String {
        val lowByte = str!!.substring(33, 35)
        val highByte = str.substring(36, 38)
        val forHex = highByte + lowByte
        val fractional = Integer.valueOf(forHex, 16).toShort().toFloat()
        return String.format("%.2f", fractional / 100)
    }

    fun getAccelerationBased(str: String?): String {
        val acceleration: String
        val lowByte = str!!.substring(27, 29)
        val highByte = str.substring(30, 32)
        val forHex = highByte + lowByte
        val fractional = Integer.valueOf(forHex, 16).toShort().toFloat()
        val result = String.format("%.1f", fractional / 100)
        acceleration = result
        return acceleration
    }

    fun getVelocityBasedLong(str: String?): String {
        val velocity: String
        val lowByte = str!!.substring(21, 23)
        val highByte = str.substring(24, 26)
        val forHex = highByte + lowByte
        val fractional = Integer.valueOf(forHex, 16).toShort().toFloat()
        val result = String.format("%.1f", fractional / 100)
        velocity = result
        return velocity
    }
}