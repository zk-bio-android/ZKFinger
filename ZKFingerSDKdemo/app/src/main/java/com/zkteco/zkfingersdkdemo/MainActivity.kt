package com.zkteco.zkfingersdkdemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zkteco.zkfingersdkdemo.document.PdfBrowser
import com.zkteco.zkfingersdkdemo.utils.PermissionsUtil
import kotlinx.android.synthetic.main.activity_main.*

/**
 * @author Administrator
 * Created by：Administrator on 2020/3/16 16:33
 * Email：qiang.xiao@zkteco.com
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //检查写入文件权限
        PermissionsUtil.identifyStoragePermissions(this)
        init()
    }

    private fun init() {
        pdf?.setOnClickListener { startActivity(Intent(this, PdfBrowser::class.java)) }
    }


}