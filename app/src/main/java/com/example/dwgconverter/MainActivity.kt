package com.example.dwgconverter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import java.io.File

class MainActivity : Activity() {

    private lateinit var btnPick: Button
    private lateinit var btnConvert: Button
    private lateinit var txtFile: TextView
    private lateinit var txtVersion: TextView
    private lateinit var txtStatus: TextView
    private lateinit var spinner: Spinner
    private lateinit var progress: ProgressBar

    private var inputFile: File? = null
    private var baseName = "drawing"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnPick = findViewById(R.id.btnPick)
        btnConvert = findViewById(R.id.btnConvert)
        txtFile = findViewById(R.id.txtFile)
        txtVersion = findViewById(R.id.txtVersion)
        txtStatus = findViewById(R.id.txtStatus)
        spinner = findViewById(R.id.spinnerTarget)
        progress = findViewById(R.id.progress)

        spinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, DwgVersions.outputs
        )

        // About: app version + LibreDWG version
        val appVersion = packageManager.getPackageInfo(packageName, 0).versionName
        findViewById<TextView>(R.id.txtAppInfo).text =
            "Version $appVersion · Uses ${DwgNative.libVersion()} (GPLv3)"

        btnPick.setOnClickListener { pickFile() }
        btnConvert.setOnClickListener { chooseSaveLocation() }

        // Also accept a DWG shared/opened from another app
        intent?.data?.let { loadInput(it) }
    }

    // ---- Step 1: pick a DWG -------------------------------------------------

    private fun pickFile() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"   // DWG has no reliable MIME type on Android
        }
        startActivityForResult(i, REQ_OPEN)
    }

    private fun loadInput(uri: Uri) {
        try {
            val name = displayName(uri) ?: "drawing.dwg"
            baseName = name.substringBeforeLast('.')
            val dest = File(cacheDir, "input.dwg")
            contentResolver.openInputStream(uri)!!.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
            val version = DwgVersions.detect(dest)
            txtFile.text = name
            if (version == null) {
                txtVersion.text = "This doesn't look like a DWG file."
                btnConvert.isEnabled = false
                inputFile = null
            } else {
                txtVersion.text = "Current version: $version"
                btnConvert.isEnabled = true
                inputFile = dest
            }
            txtStatus.text = ""
        } catch (e: Exception) {
            txtStatus.text = "Could not open file: ${e.message}"
        }
    }

    // ---- Step 2: choose where to save, then convert -------------------------

    private fun chooseSaveLocation() {
        val fmt = spinner.selectedItem as OutputFormat
        val i = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, "$baseName${fmt.suffix}.${fmt.extension}")
        }
        startActivityForResult(i, REQ_SAVE)
    }

    private fun convertTo(outUri: Uri) {
        val input = inputFile ?: return
        val fmt = spinner.selectedItem as OutputFormat
        val tmpOut = File(cacheDir, "output.${fmt.extension}").apply { delete() }

        setBusy(true)
        txtStatus.text = "Converting…"

        // LibreDWG recurses deeply on big drawings, so give it a large stack.
        Thread(null, {
            val error = try {
                DwgNative.convert(input.absolutePath, tmpOut.absolutePath, fmt.target, fmt.asDxf)
            } catch (t: Throwable) {
                t.message ?: t.toString()
            }

            val result = if (error == null && tmpOut.length() > 0) {
                try {
                    contentResolver.openOutputStream(outUri, "wt")!!.use { out ->
                        tmpOut.inputStream().use { it.copyTo(out) }
                    }
                    "✓ Saved ${fmt.extension.uppercase()} (${tmpOut.length() / 1024} KB)"
                } catch (e: Exception) {
                    "Converted, but saving failed: ${e.message}"
                }
            } else {
                "✗ Conversion failed: ${error ?: "empty output"}"
            }
            tmpOut.delete()

            runOnUiThread {
                setBusy(false)
                txtStatus.text = result
            }
        }, "dwg-convert", 64L * 1024 * 1024).start()
    }

    // ---- helpers ------------------------------------------------------------

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (resultCode != RESULT_OK || uri == null) return
        when (requestCode) {
            REQ_OPEN -> loadInput(uri)
            REQ_SAVE -> convertTo(uri)
        }
    }

    private fun setBusy(busy: Boolean) {
        progress.visibility = if (busy) View.VISIBLE else View.GONE
        btnPick.isEnabled = !busy
        btnConvert.isEnabled = !busy && inputFile != null
        spinner.isEnabled = !busy
    }

    private fun displayName(uri: Uri): String? =
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }

    companion object {
        private const val REQ_OPEN = 1
        private const val REQ_SAVE = 2
    }
}
