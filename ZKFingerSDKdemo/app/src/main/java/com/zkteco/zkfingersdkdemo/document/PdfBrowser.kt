package com.zkteco.zkfingersdkdemo.document

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.shockwave.pdfium.PdfDocument.Bookmark
import com.zkteco.zkfingersdkdemo.R
import kotlinx.android.synthetic.main.activity_pdf.*

/**
 * @author Administrator
 * Created by：Administrator on 2020/3/16 16:33
 * Email：qiang.xiao@zkteco.com
 */

class PdfBrowser : AppCompatActivity(), OnPageChangeListener, OnLoadCompleteListener, OnPageErrorListener {

    var uri: Uri? = null
    var pageNumber = 0
    var pdfFileName: String? = null

    companion object {
        private val TAG = PdfBrowser::class.java.simpleName
        private const val REQUEST_CODE = 42
        const val SAMPLE_FILE = "ZKFinger Reader SDK for Android_en.pdf"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf)
    }

    override fun onResume() {
        super.onResume()
        pdfView!!.setBackgroundColor(Color.LTGRAY)
        if (uri != null) {
            displayFromUri(uri)
        } else {
            displayFromAsset(SAMPLE_FILE)
        }
        title = pdfFileName
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options, menu);
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.pickFile -> launchPicker()
        }
        return true
    }


    private fun launchPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        try {
            startActivityForResult(intent, REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.toast_pick_file_error, Toast.LENGTH_SHORT).show()
        }
    }


    private fun displayFromAsset(assetFileName: String) {
        pdfFileName = assetFileName
        pdfView!!.fromAsset(SAMPLE_FILE)
                .defaultPage(pageNumber)
                .onPageChange(this)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .scrollHandle(DefaultScrollHandle(this))
                .spacing(10)
                .onPageError(this)
                .pageFitPolicy(FitPolicy.BOTH)
                .load()
    }

    private fun displayFromUri(uri: Uri?) {
        pdfFileName = getFileName(uri)
        pdfView!!.fromUri(uri)
                .defaultPage(pageNumber)
                .onPageChange(this)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .scrollHandle(DefaultScrollHandle(this))
                .spacing(10)
                .onPageError(this)
                .pageFitPolicy(FitPolicy.BOTH)
                .load()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            uri = data?.data
            displayFromUri(uri)
        }
    }


    override fun onPageChanged(page: Int, pageCount: Int) {
        pageNumber = page
        if (pdfFileName?.length!! >= 35) pdfFileName = pdfFileName?.substring(0, 35) + "..."
        title = String.format("%s %s / %s", pdfFileName, page + 1, pageCount)
    }

    private fun getFileName(uri: Uri?): String? {
        var result: String? = null
        if (uri!!.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor.use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result != null) return result
        result = uri.lastPathSegment
        return result
    }

    override fun loadComplete(nbPages: Int) {
        printBookmarksTree(pdfView!!.tableOfContents, "-")
    }

    private fun printBookmarksTree(tree: List<Bookmark>, sep: String) {
        for (b in tree) {
            Log.e(TAG, String.format("%s %s, p %d", sep, b.title, b.pageIdx))
            if (b.hasChildren()) {
                printBookmarksTree(b.children, "$sep-")
            }
        }
    }


    override fun onPageError(page: Int, t: Throwable) {
        Log.e(TAG, "Cannot load page $page")
    }


}