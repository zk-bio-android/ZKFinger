package com.zkteco.zkfingersdkdemo.fingerprint

import android.content.Context
import com.zkteco.android.biometric.FingerprintExceptionListener
import com.zkteco.android.biometric.core.device.ParameterHelper
import com.zkteco.android.biometric.core.device.TransportType
import com.zkteco.android.biometric.core.utils.HHDeviceControl
import com.zkteco.android.biometric.core.utils.LogHelper
import com.zkteco.android.biometric.core.utils.ToolUtils
import com.zkteco.android.biometric.module.fingerprint.FingerprintCaptureListener
import com.zkteco.android.biometric.module.fingerprint.FingerprintSensor
import com.zkteco.android.biometric.module.fingerprint.exception.FingerprintSensorException
import com.zkteco.android.biometric.module.fingerprintreader.FingerprintFactory
import com.zkteco.android.biometric.module.fingerprintreader.ZKFingerService
import com.zkteco.android.biometric.module.fingerprintreader.exception.FingerprintException
import com.zkteco.android.biometric.nidfpsensor.NIDFPFactory
import com.zkteco.android.biometric.nidfpsensor.NIDFPSensor
import com.zkteco.android.biometric.nidfpsensor.exception.NIDFPException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

class ZKFingerManager {

    private var nidfpSensor: NIDFPSensor? = null
    private var fingerprintSensor: FingerprintSensor? = null
    private var fingerprintReader: com.zkteco.android.biometric.module.fingerprintreader.FingerprintSensor? = null
    private var bOpened = false
    private var zkFingerListener: ZKFingerListener? = null
    private var mbStop = true
    private var countdownLatch: CountDownLatch? = null
    private var captureThread: CaptureThread? = null

    private fun createFingerprintReader(context: Context, VID: Int, PID: Int): com.zkteco.android.biometric.module.fingerprintreader.FingerprintSensor? {
        var sensor: com.zkteco.android.biometric.module.fingerprintreader.FingerprintSensor? = null
        // Start fingerprint sensor
        //val fingerprintParams: MutableMap<*, *> = HashMap<Any?, Any?>()
        val fingerprintParams: MutableMap<String, Any?> =HashMap<String,Any?>()
        //set vid
        fingerprintParams[ParameterHelper.PARAM_KEY_VID] = VID
        //set pid
        fingerprintParams[ParameterHelper.PARAM_KEY_PID] = PID
        sensor = FingerprintFactory.createFingerprintSensor(context, TransportType.USB, fingerprintParams)
        return sensor
    }

    private fun createFingerprintSensor(context: Context, VID: Int, PID: Int): FingerprintSensor? {
        var sensor: FingerprintSensor? = null
        // Start fingerprint sensor
        val fingerprintParams: MutableMap<String, Any?> =HashMap<String,Any?>()
        //set vid
        fingerprintParams[ParameterHelper.PARAM_KEY_VID] = VID
        //set pid
        fingerprintParams[ParameterHelper.PARAM_KEY_PID] = PID
        sensor = com.zkteco.android.biometric.module.fingerprint.FingerprintFactory.createFingerprintSensor(context, TransportType.USB, fingerprintParams)
        return sensor
    }

    private fun createIDFPFingerSensor(context: Context, VID: Int, PID: Int): NIDFPSensor? {
        var sensor: NIDFPSensor? = null
        // Start fingerprint sensor
        val fingerprintParams: MutableMap<String, Any?> =HashMap<String,Any?>()
        //set vid
        fingerprintParams[ParameterHelper.PARAM_KEY_VID] = VID
        //set pid
        fingerprintParams[ParameterHelper.PARAM_KEY_PID] = PID
        sensor = NIDFPFactory.createNIDFPSensor(context, TransportType.USBSCSI, fingerprintParams)
        return sensor
    }

    private fun destoryFingerprintSensor(sensor: FingerprintSensor?) {
        var sensor = sensor
        com.zkteco.android.biometric.module.fingerprint.FingerprintFactory.destroy(sensor)
        sensor = null
    }

    private fun destoryIDFPSensor(sensor: NIDFPSensor?) {
        var sensor = sensor
        NIDFPFactory.destroy(sensor)
        sensor = null
    }

    private fun destoryFingerprintReader(sensor: com.zkteco.android.biometric.module.fingerprintreader.FingerprintSensor?) {
        var sensor = sensor
        FingerprintFactory.destroy(sensor)
        sensor = null
    }

    fun setLevel(level: Int) {
        LogHelper.setLevel(level)
    }

    fun openDevice(context: Context, vid: Int, pid: Int): Boolean {
        return if (bOpened) {
            false
        } else try {
            bOpened = false
            if (vid != ZK_VID) {
                bOpened
            } else if (0x0300 <= pid && 0x03FF >= pid) {
                nidfpSensor = createIDFPFingerSensor(context, vid, pid)
                nidfpSensor!!.setIDFPSupport(false) //不支持15.0
                try {
                    nidfpSensor!!.open(0)
                    nidfpSensor!!.setParameter(0, 5, 0)
                    if (0 != ZKFingerService.init()) //初始化10.0 算法
                    {
                        nidfpSensor!!.close(0)
                        return bOpened
                    }
                    if (ZKFingerService.getLicenseType() == ZKFingerService.LIC_TYPE_LIMIT) {
                        //limit license, not support extract
                        ZKFingerService.free()
                        nidfpSensor!!.close(0)
                        return bOpened
                    }
                    bOpened = true
                    bOpened
                } catch (e: NIDFPException) {
                    e.printStackTrace()
                    bOpened
                }
            } else if (0x0121 == pid) {
                fingerprintSensor = createFingerprintSensor(context, vid, pid)
                try {
                    fingerprintSensor!!.open(0)
                    if (0 != ZKFingerService.init()) {
                        fingerprintSensor!!.close(0)
                        return false
                    }
                    bOpened = true
                    bOpened
                } catch (e: FingerprintSensorException) {
                    e.printStackTrace()
                    bOpened
                }
            } else if (0x0120 == pid || 0x0124 == pid) {
                fingerprintReader = createFingerprintReader(context, vid, pid)
                try {
                    fingerprintReader!!.setManualReleaseZKFinger(true)
                    fingerprintReader!!.open(0)
                    if (ZKFingerService.getLicenseType() == ZKFingerService.LIC_TYPE_LIMIT) {
                        //limit license, not support extract
                        ZKFingerService.free()
                        fingerprintReader!!.close(0)
                        return bOpened
                    }
                    bOpened = true
                    bOpened
                } catch (e: FingerprintException) {
                    e.printStackTrace()
                    bOpened
                }
            } else {
                bOpened
            }
        } finally {
            if (!bOpened) {
                destoryFingerprintReader(fingerprintReader)
                destoryFingerprintSensor(fingerprintSensor)
                destoryIDFPSensor(nidfpSensor)
            }
        }
    }

    fun closeDevice() {
        if (!bOpened) {
            return
        }
        stopCapture()
        if (null != nidfpSensor) {
            try {
                nidfpSensor!!.close(0)
            } catch (e: NIDFPException) {
                e.printStackTrace()
            }
            destoryIDFPSensor(nidfpSensor)
            bOpened = false
        } else if (null != fingerprintSensor) {
            try {
                fingerprintSensor!!.close(0)
            } catch (e: FingerprintSensorException) {
                e.printStackTrace()
            }
            destoryFingerprintSensor(fingerprintSensor)
            bOpened = false
        } else if (null != fingerprintReader) {
            try {
                fingerprintReader!!.close(0)
            } catch (e: FingerprintException) {
                e.printStackTrace()
            }
            destoryFingerprintReader(fingerprintReader)
            bOpened = false
        }
    }

    val fwVersion: String
        get() {
            if (!bOpened) {
                return ""
            }
            return if (nidfpSensor != null) {
                nidfpSensor!!.firmwareVersion
            } else if (fingerprintSensor != null) {
                fingerprintSensor!!.firmwareVersion
            } else if (fingerprintReader != null) {
                fingerprintReader!!.firmwareVersion
            } else {
                ""
            }
        }

    val serialNumber: String
        get() {
            if (!bOpened) {
                return ""
            }
            return if (nidfpSensor != null) {
                nidfpSensor!!.serialNumber
            } else if (fingerprintSensor != null) {
                fingerprintSensor!!.serialNumber
            } else if (fingerprintReader != null) {
                fingerprintReader!!.strSerialNumber
            } else {
                ""
            }
        }

    private fun AcquireImageAndTemplate(index: Int, fpImage: ByteArray, fpTemplate: ByteArray, quality: IntArray): Int {
        var ret = 0
        try {
            val status = nidfpSensor!!.getDeviceStatus(0)
            println("status=$status")
            nidfpSensor!!.GetFPRawData(0, fpImage)
        } catch (e: NIDFPException) {
            e.printStackTrace()
            return -2
        }
        ret = ZKFingerService.extract(fpImage, nidfpSensor!!.fpImgWidth, nidfpSensor!!.fpImgHeight, fpTemplate, quality)
        return ret
    }

    private inner class CaptureThread : Thread() {
        override fun run() {
            super.run()
            mbStop = false
            val fpImage = ByteArray(nidfpSensor!!.fpImgWidth * nidfpSensor!!.fpImgHeight)
            var exceptCnt = 0
            while (!mbStop) {
                try {
                    sleep(100)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                val fpTemplate = ByteArray(2048)
                val quality = IntArray(1)
                val ret = AcquireImageAndTemplate(0, fpImage, fpTemplate, quality)
                if (ret <= 0) {
                    val status = nidfpSensor!!.getDeviceStatus(0)
                    if (0 == status) //状态恢复后重置状态
                    {
                        exceptCnt = 0
                    } else {
                        exceptCnt++
                    }
                    if (exceptCnt >= 10) //连续10次异常后，每次异常都通知上层
                    {
                        if (zkFingerListener != null) {
                            zkFingerListener!!.onException()
                        }
                    }
                    continue
                }
                if (zkFingerListener != null) {
                    val bitmap = ToolUtils.renderCroppedGreyScaleBitmap(fpImage, nidfpSensor!!.fpImgWidth, nidfpSensor!!.fpImgHeight)
                    zkFingerListener!!.onCapture(bitmap)
                    zkFingerListener!!.onExtract(fpTemplate, ret)
                }
            }
            countdownLatch!!.countDown()
        }
    }

    fun setZKFingerListener(listener: ZKFingerListener?) {
        zkFingerListener = listener
    }

    fun startCapture() {
        if (!bOpened) {
            return
        }
        if (nidfpSensor != null) {
            mbStop = true
            if (null != countdownLatch) {
                try {
                    countdownLatch!!.await(10, TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                countdownLatch = null
            }
            countdownLatch = CountDownLatch(1)
            captureThread = CaptureThread()
            captureThread!!.start()
        } else if (fingerprintSensor != null) {
            val listener: FingerprintCaptureListener = object : FingerprintCaptureListener {
                override fun captureOK(i: Int, bytes: ByteArray, ints: IntArray, bytes1: ByteArray) {
                    if (zkFingerListener != null) {
                        val bitmap = ToolUtils.renderCroppedGreyScaleBitmap(bytes, ints[0], ints[1])
                        zkFingerListener!!.onCapture(bitmap)
                        zkFingerListener!!.onExtract(bytes1, fingerprintSensor!!.lastTempLen)
                    }
                }

                override fun captureError(e: FingerprintSensorException) {
                    if (fingerprintSensor!!.isClosed) {
                        zkFingerListener!!.onException()
                    }
                }
            }
            fingerprintSensor!!.setFingerprintCaptureListener(0, null)
            fingerprintSensor!!.setFingerprintCaptureListener(0, listener)
            try {
                fingerprintSensor!!.startCapture(0)
            } catch (e: FingerprintSensorException) {
                e.printStackTrace()
            }
        } else if (fingerprintReader != null) {
            val listener: com.zkteco.android.biometric.module.fingerprintreader.FingerprintCaptureListener = object : com.zkteco.android.biometric.module.fingerprintreader.FingerprintCaptureListener {
                override fun captureOK(bytes: ByteArray) {
                    if (zkFingerListener != null) {
                        val bitmap = ToolUtils.renderCroppedGreyScaleBitmap(bytes, fingerprintReader!!.imageWidth, fingerprintReader!!.imageHeight)
                        zkFingerListener!!.onCapture(bitmap)
                    }
                }

                override fun captureError(e: FingerprintException) {}
                override fun extractOK(bytes: ByteArray) {
                    if (zkFingerListener != null) {
                        zkFingerListener!!.onExtract(bytes, fingerprintReader!!.lastTempLen)
                    }
                }

                override fun extractError(i: Int) {}
            }
            val exceptionListener = FingerprintExceptionListener {
                if (zkFingerListener != null) {
                    zkFingerListener!!.onException()
                }
            }
            fingerprintReader!!.setFingerprintCaptureListener(0, null)
            fingerprintReader!!.setFingerprintCaptureListener(0, listener)
            try {
                fingerprintReader!!.startCapture(0)
            } catch (e: FingerprintException) {
                e.printStackTrace()
            }
        } else {
            return
        }
    }

    fun stopCapture() {
        if (!bOpened) {
            return
        }
        if (nidfpSensor != null) {
            mbStop = true
            if (null != countdownLatch) {
                try {
                    countdownLatch!!.await(10, TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                countdownLatch = null
            }
            captureThread = null
        } else if (fingerprintSensor != null) {
            try {
                fingerprintSensor!!.stopCapture(0)
            } catch (e: FingerprintSensorException) {
                e.printStackTrace()
            }
        } else if (fingerprintReader != null) {
            try {
                fingerprintReader!!.stopCapture(0)
            } catch (e: FingerprintException) {
                e.printStackTrace()
            }
        } else {
            return
        }
    }

    fun rebootDevice(context: Context, vid: Int, pid: Int) {
        if (vid != ZK_VID) {
            return
        } else if (0x0300 <= pid && 0x03FF >= pid) {
            val sensor = createIDFPFingerSensor(context, vid, pid)
            try {
                sensor!!.rebootDevice(0)
            } catch (e: NIDFPException) {
                e.printStackTrace()
            }
            destoryIDFPSensor(sensor)
        } else if (0x0121 == pid) {
            val sensor = createFingerprintSensor(context, vid, pid)
            try {
                sensor!!.rebootDeviceEx(0)
            } catch (e: FingerprintSensorException) {
                e.printStackTrace()
            }
            destoryFingerprintSensor(sensor)
        } else if (0x0120 == pid || 0x0124 == pid) {
            val sensor = createFingerprintReader(context, vid, pid)
            sensor!!.openAndReboot(0)
            destoryFingerprintReader(sensor)
        }
    }

    companion object {
        private const val ZK_VID = 0x1b55
        fun ID5XXFPPowerOn() {
            HHDeviceControl.HHDevicePowerOn("5V")
            HHDeviceControl.HHDeviceGpioLow(141)
        }

        fun ID5XXFPPowerOff() {
            HHDeviceControl.HHDeviceGpioHigh(141)
            HHDeviceControl.HHDevicePowerOff("5V")
        }
    }
}