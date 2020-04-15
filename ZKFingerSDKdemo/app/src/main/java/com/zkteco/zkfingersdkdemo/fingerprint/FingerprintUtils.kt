package com.zkteco.zkfingersdkdemo.fingerprint

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zkteco.android.biometric.core.utils.LogHelper
import com.zkteco.android.biometric.core.utils.ToolUtils
import com.zkteco.android.biometric.module.fingerprintreader.ZKFingerService
import com.zkteco.zkfingersdkdemo.R
import com.zkteco.zkfingersdkdemo.document.PdfBrowser
import com.zkteco.zkfingersdkdemo.utils.PermissionsUtil
import kotlinx.android.synthetic.main.activity_main.*

/**
 * @author Administrator
 */
class FingerprintUtils : AppCompatActivity() {

    private val ACTION_USB_PERMISSION = "com.zkteco.biometric.fpdemo.USB_PERMISSION"
    private var mUsbManager: UsbManager? = null
    private var isStart = false
    private val strUidPrefix = "uid_"
    private var uid_id = 1
    private var enroll_index = 0
    private val reg_temp_array = Array(3) { ByteArray(2048) }
    private var isRegister = false
    private val zkFingerManager: ZKFingerManager = ZKFingerManager()
    private var listener: BatteryListener? = null


    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    //UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openDevice()
                    } else {
                        Toast.makeText(getApplicationContext(), "USB unauthorized", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun findDevice(): UsbDevice? {
        mUsbManager = this.getSystemService(Context.USB_SERVICE) as UsbManager?
        for (device in mUsbManager!!.deviceList.values) {
            LogHelper.d("usb device:$device")
            val device_pid = device.productId
            if (device.vendorId == ZK_VID && (device_pid == 0x0121 || device_pid == 0x0120)) {
                PID = device_pid
                return device
            }
        }
        return null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        PermissionsUtil.identifyStoragePermissions(this)
        initUI()
    }

    private fun initUI() {
        pdf?.setOnClickListener { startActivity(Intent(this, PdfBrowser::class.java)) }
        //设置日志等级
        zkFingerManager.setLevel(Log.VERBOSE)
        //USB检测
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(mUsbReceiver, filter)
        addBatteryListener()
    }

    private fun addBatteryListener() {
        listener = BatteryListener(this)
        listener!!.register(object : BatteryListener.BatteryStateListener {
            override fun onStateChanged() {
            }
            override fun onStateLow() {
                Toast.makeText(this@FingerprintUtils, "onStateLow", Toast.LENGTH_SHORT).show()
            }

            override fun onStateOkay() {
                Toast.makeText(this@FingerprintUtils, "onStateOkay", Toast.LENGTH_SHORT).show()
            }

            override fun onStatePowerConnected() {
                Toast.makeText(this@FingerprintUtils, "onStatePowerConnected", Toast.LENGTH_SHORT).show()
            }

            override fun onStatePowerDisconnected() {
                Toast.makeText(this@FingerprintUtils, "onStatePowerDisconnected", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroy() {
        listener?.unregister()
        super.onDestroy()
        closeDevice()
        unregisterReceiver(mUsbReceiver)
    }

    private fun OpenDeviceAndRequestDevice() {
        if (isStart) {
            txtResult.text = "Device had connected!"
            return
        }
        run {

            //非ID510/ID500 下面函数不用调用
            ZKFingerManager.ID5XXFPPowerOff()
            ToolUtils.sleep(50)
            ZKFingerManager.ID5XXFPPowerOn()
        }


        //等待模块上电
        val lTickStart = System.currentTimeMillis()
        var usbDevice: UsbDevice? = null
        while (System.currentTimeMillis() - lTickStart < 10 * 1000) {
            if (findDevice().also { usbDevice = it } != null) {
                break
            }
            try {
                Thread.sleep(50)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        if (null == usbDevice) {
            txtResult.text = "device not found"
            return
        }
        val intent = Intent(ACTION_USB_PERMISSION)
        val pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0)
        mUsbManager!!.requestPermission(usbDevice, pendingIntent)
    }

    private fun openDevice() {
        if (isStart) {
            //mTxtReport.setText("设备已连接");
            return
        }
        val retVal = 0
        isRegister = false
        enroll_index = 0
        if (!zkFingerManager.openDevice(getApplicationContext(), ZK_VID, PID)) {
            txtResult.text = "open device failed!"
            zkFingerManager.rebootDevice(getApplicationContext(), ZK_VID, PID)
            return
        }
        isStart = true
        val zkFingerListener: ZKFingerListener = object : ZKFingerListener {
            override fun onCapture(bitmap: Bitmap?) {
                runOnUiThread(Runnable { imgFP.setImageBitmap(bitmap) })
            }

            override fun onExtract(tmplate: ByteArray?, templateSize: Int) {
                val templateLen = templateSize
                if (isRegister) {
                    doRegister(tmplate)
                } else {
                    doIdentify(tmplate)
                }
            }

            override fun onException() {
            }
        }
        zkFingerManager.setZKFingerListener(zkFingerListener)
        zkFingerManager.startCapture()
        txtResult.text = "Open device succ, fwversion:" + zkFingerManager.fwVersion
    }

    private fun closeDevice() {
        if (isStart) {
            zkFingerManager.stopCapture()
            zkFingerManager.closeDevice()
            ZKFingerService.free()
        }
        run {
            //非ID510/ID500 下面函数不用调用
            ZKFingerManager.ID5XXFPPowerOff()
        }
        isStart = false
    }

    fun onBnConnect(view: View?) {
        OpenDeviceAndRequestDevice()
    }

    fun onBnRegister(view: View?) {
        if (!isStart) {
            txtResult.text = "Device not opened!"
            return
        }
        if (isRegister) {
            return
        }
        enroll_index = 0
        isRegister = true
        txtResult.text = "Please press your finger 3 times(same finger.)"
    }

    fun onBnDisconnect(view: View?) {
        closeDevice()
        txtResult.text = "device closed"
    }

    fun showUIMessage(strMsg: String) {
        runOnUiThread(Runnable { txtResult.text = strMsg })
    }

    fun doRegister(template: ByteArray?) {
        val bufids = ByteArray(256)
        var ret = ZKFingerService.identify(template, bufids, 70, 1)
        if (ret > 0) {
            val strRes = String(bufids).split("\t").toTypedArray()
            showUIMessage("the finger already enroll by " + strRes[0] + ",cancel enroll")
            isRegister = false
            enroll_index = 0
            return
        }
        if (enroll_index > 0 && ZKFingerService.verify(reg_temp_array[enroll_index - 1], template) <= 0) {
            showUIMessage("please press the same finger 3 times for the enrollment, cancel enroll")
            isRegister = false
            enroll_index = 0
            return
        }
        System.arraycopy(template, 0, reg_temp_array[enroll_index], 0, 2048)
        enroll_index++
        if (enroll_index == ENROLL_COUNT) {
            val regTemp = ByteArray(2048)
            if (0 < ZKFingerService.merge(reg_temp_array[0], reg_temp_array[1], reg_temp_array[2], regTemp).also { ret = it }) {
                val retVal = ZKFingerService.save(regTemp, strUidPrefix + uid_id++)
                if (0 == retVal) {
                    showUIMessage("enroll succ")
                } else {
                    showUIMessage("enroll fail, add template fail!")
                }
            } else {
                showUIMessage("enroll fail")
            }
            isRegister = false
        } else {
            showUIMessage("You need to press the " + (3 - enroll_index) + "time fingerprint")
        }
    }

    fun doIdentify(template: ByteArray?) {
        val bufids = ByteArray(256)
        val ret = ZKFingerService.identify(template, bufids, 70, 1)
        if (ret > 0) {
            val strRes = String(bufids).split("\t").toTypedArray()
            showUIMessage("identify succ, userid:" + strRes[0].trim { it <= ' ' } + ", score:" + strRes[1].trim { it <= ' ' })
        } else {
            showUIMessage("identify fail")
        }
    }

    companion object {
        private const val ZK_VID = 6997
        private var PID = 0
        private const val ENROLL_COUNT = 3
    }
}