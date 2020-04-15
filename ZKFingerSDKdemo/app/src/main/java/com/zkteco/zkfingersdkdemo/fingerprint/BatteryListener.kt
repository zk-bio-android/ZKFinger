package com.zkteco.zkfingersdkdemo.fingerprint

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

/**
 * @author Administrator
 * Created by：Administrator on 2019/11/22 15:04
 * Email：Qiang.Xiao@zkteco.com
 */
class BatteryListener(private val mContext: Context) {
    private val receiver: BatteryBroadcastReceiver?
    private var mBatteryStateListener: BatteryStateListener? = null

    fun register(listener: BatteryStateListener?) {
        mBatteryStateListener = listener
        if (receiver != null) {
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_BATTERY_CHANGED)
            filter.addAction(Intent.ACTION_BATTERY_LOW)
            filter.addAction(Intent.ACTION_BATTERY_OKAY)
            filter.addAction(Intent.ACTION_POWER_CONNECTED)
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
            mContext.registerReceiver(receiver, filter)
        }
    }

    fun unregister() {
        if (receiver != null) {
            mContext.unregisterReceiver(receiver)
        }
    }

    private inner class BatteryBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent != null) {
                val action = intent.action
                when (action) {
                    Intent.ACTION_BATTERY_CHANGED -> if (mBatteryStateListener != null) {
                        Log.e("xiao", "BatteryBroadcastReceiver --> onReceive--> ACTION_BATTERY_CHANGED")
                        mBatteryStateListener!!.onStateChanged()
                    }
                    Intent.ACTION_BATTERY_LOW -> if (mBatteryStateListener != null) {
                        Log.e("xiao", "BatteryBroadcastReceiver --> onReceive--> ACTION_BATTERY_LOW")
                        mBatteryStateListener!!.onStateLow()
                    }
                    Intent.ACTION_BATTERY_OKAY -> if (mBatteryStateListener != null) {
                        Log.e("xiao", "BatteryBroadcastReceiver --> onReceive--> ACTION_BATTERY_OKAY")
                        mBatteryStateListener!!.onStateOkay()
                    }
                    Intent.ACTION_POWER_CONNECTED -> if (mBatteryStateListener != null) {
                        Log.e("xiao", "BatteryBroadcastReceiver --> onReceive--> ACTION_POWER_CONNECTED")
                        mBatteryStateListener!!.onStatePowerConnected()
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> if (mBatteryStateListener != null) {
                        Log.e("xiao", "BatteryBroadcastReceiver --> onReceive--> ACTION_POWER_DISCONNECTED")
                        mBatteryStateListener!!.onStatePowerDisconnected()
                    }
                }
            }
        }
    }

    interface BatteryStateListener {
        fun onStateChanged()
        fun onStateLow()
        fun onStateOkay()
        fun onStatePowerConnected()
        fun onStatePowerDisconnected()
    }

    init {
        receiver = BatteryBroadcastReceiver()
    }
}