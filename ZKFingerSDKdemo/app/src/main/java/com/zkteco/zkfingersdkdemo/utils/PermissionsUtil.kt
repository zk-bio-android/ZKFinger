package com.zkteco.zkfingersdkdemo.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

/**
 * @author Administrator
 * Created by：Administrator on 2020/3/2 16:01
 * Email：Qiang.Xiao@zkteco.com
 */
class PermissionsUtil {
    /**
     * 写入外部存储权限
     */
    companion object {

        const val REQUEST_EXTERNAL_STORAGE = 1

        /**
         * 检查应用程序是否有权写入设备存储
         * 如果应用程序没有权限，则会提示用户授予权限
         *
         * @param activity 所在的Activity
         */
        @JvmStatic
        fun identifyStoragePermissions(activity: Activity?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_EXTERNAL_STORAGE)
            }
        }
    }
}