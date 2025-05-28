package com.example.barcodescannerbycamera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.googlecode.tesseract.android.TessBaseAPI
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.PermissionListener
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var capturedImageView: ImageView
    private lateinit var imeiTextView: TextView
    private lateinit var imageFilePath: String
    private var locationPermissionReqCode = 1000
//
//    companion object {
//        const val REQUEST_IMAGE_CAPTURE = 1
//    }
companion object {
    const val LOCATION_REQUEST_CODE = 1
    private const val GALLERY_REQUEST_CODE = 2
    const val REQUEST_IMAGE_CAPTURE = 1
    const val TAG = "CameraXApp"
    const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    val REQUIRED_PERMISSIONS =
        mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
}

    lateinit var extractButton :Button
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        capturedImageView = findViewById(R.id.capturedImageView)
        extractButton = findViewById(R.id.extractButton)
        imeiTextView = findViewById(R.id.imeiTextView)
     //   galleryCheckPermission()
        extractButton.setOnClickListener {view->
            extractIMEIFromImage(view)
        }

//        cameraPreview = findViewById(R.id.cameraPreview)
//        barcodeResult = findViewById(R.id.barcodeResult)
//        barcodeButton = findViewById(R.id.button)
//
//
//        detector = BarcodeDetector.Builder(this)
//            .setBarcodeFormats(Barcode.ALL_FORMATS)
//            .build()
//
//        if (!detector.isOperational) {
//            // Handle error
//            showBarcodeScannerErrorDialog()
//        }
//
//        cameraSource = CameraSource.Builder(this, detector)
//            .setAutoFocusEnabled(true)
//            .build()
//
//        cameraPreview.holder.addCallback(object : SurfaceHolder.Callback {
//            override fun surfaceCreated(holder: SurfaceHolder) {
//                try {
//                    if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
//                        cameraSource.start(holder)
//                    } else {
//                        // Request camera permission
//                        requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 123)
//                    }
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }
//            }
//
//            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
//
//            override fun surfaceDestroyed(holder: SurfaceHolder) {
//                cameraSource.stop()
//            }
//        })
//
//    barcodeButton.setOnClickListener {
//        detector.setProcessor(object : com.google.android.gms.vision.Detector.Processor<Barcode> {
//            override fun release() {}
//
//            override fun receiveDetections(detections: com.google.android.gms.vision.Detector.Detections<Barcode>) {
//                val barcodes = detections.detectedItems
//                if (barcodes.size() != 0) {
//                    val value = barcodes.valueAt(0).displayValue
//                    runOnUiThread {
//                        // Update UI with the barcode value
//                        barcodeResult.text = "Barcode: $value"
//                    }
//                }
//            }
//        })
//    }
        galleryCheckPermission()
    }
    private fun getStoragePermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    private fun galleryCheckPermission() {

        Dexter.withContext(this).withPermission(
            getStoragePermission()
        ).withListener(object : PermissionListener {
            override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                pickFromGallery()
            }

            override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                Toast.makeText(
                  this@MainActivity,
                    "You have denied the storage permission to select image",
                    Toast.LENGTH_SHORT
                ).show()
               showRotationalDialogForPermission()
            }

            override fun onPermissionRationaleShouldBeShown(
                p0: com.karumi.dexter.listener.PermissionRequest?, p1: PermissionToken?,
            ) {
                // showRotationalDialogForPermission()
            }
        }).onSameThread().check()
    }
    private fun showRotationalDialogForPermission() {
        AlertDialog.Builder(this)
            .setMessage(
                "It looks like you have turned off permissions"
                        + "required for this feature. It can be enable under App settings!!!"
            )

            .setPositiveButton("Go TO SETTINGS") { _, _ ->

                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package",this.packageName, null)
                    intent.data = uri
                    startActivity(intent)

                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }

            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            locationPermissionReqCode -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    // permission granted
                } else {
                    // permission denied
                    Toast.makeText(
                        this, "You need to grant permission to access location",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    fun extractIMEIFromImage(view: android.view.View) {
       // dispatchTakePictureIntent()
    }
//    private fun dispatchTakePictureIntent() {
//        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        if (takePictureIntent.resolveActivity(packageManager) != null) {
//            val photoFile: File? = createImageFile()
//            if (photoFile != null) {
//                val photoURI = FileProvider.getUriForFile(
//                    this,
//                    "com.example.android.fileprovider",
//                    photoFile
//                )
//                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
//                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
//            }
//        }
//    }
    private fun createImageFile(): File? {
        // Create an image file name
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val image = File.createTempFile(
            "JPEG_${System.currentTimeMillis()}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
        imageFilePath = image.absolutePath
        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {

            val selectedImageUri = data?.data

            val bitmap = BitmapFactory.decodeFile(imageFilePath)
            capturedImageView.setImageBitmap(bitmap)
            processImage()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
    private fun processImage() {
        val bitmap = BitmapFactory.decodeFile(imageFilePath)
        capturedImageView.setImageBitmap(bitmap)

        // Perform OCR on the image
        val tessBaseApi = TessBaseAPI()
        tessBaseApi.init(externalCacheDir?.absolutePath, "eng")
        tessBaseApi.setImage(bitmap)

        // Extract text from the image
        val extractedText = tessBaseApi.utF8Text
        tessBaseApi.end()

        // Extract IMEI from the recognized text
        val imeiPattern = Regex("""\d{15}""")
        val matchResult = imeiPattern.find(extractedText)

        if (matchResult != null) {
            val imei = matchResult.value
            imeiTextView.text = "IMEI: $imei"
        } else {
            imeiTextView.text = "IMEI not found."
        }
    }



}