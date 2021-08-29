package net.amond.mediastore

import android.content.ContentValues
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MediaStorePlugin(private val registrar: Registrar): MethodCallHandler {

  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "net.amond/media_store")
      channel.setMethodCallHandler(
          MediaStorePlugin(registrar))
    }
  }

  override fun onMethodCall(call: MethodCall, result: Result): Unit {
    when (call.method) {
      "saveImageToGallery" -> {
        val image = call.arguments as ByteArray
        result.success(saveImageToGallery(BitmapFactory.decodeByteArray(image,0,image.size)))
      }
      "saveFileToGallery" -> {
//        val path = call.arguments as String

        val path  = call.argument<String>("path")!!
        val name =  call.argument<String>("name")!!
        result.success(saveFileToGallery(path,name))
      }
      else -> result.notImplemented()
    }

  }

  private fun generateFile(extension: String = ""): File {
    val storePath =  Environment.getExternalStorageDirectory().absolutePath + File.separator + getApplicationName()
    val appDir = File(storePath)
    if (!appDir.exists()) {
      appDir.mkdir()
    }
    var fileName = System.currentTimeMillis().toString()
    if (extension.isNotEmpty()) {
      fileName += ("." + extension)
    }
    return File(appDir, fileName)
  }

  private fun saveImageToGallery(bmp: Bitmap): String {
    val context = registrar.activeContext().applicationContext
    val file = generateFile("png")
    try {
      val fos = FileOutputStream(file)
      bmp.compress(Bitmap.CompressFormat.PNG, 60, fos)
      fos.flush()
      fos.close()
      val uri = Uri.fromFile(file)
      context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
      return uri.toString()
    } catch (e: IOException) {
      e.printStackTrace()
    }
    return ""
  }


  private fun saveFileToGallery(path: String, name: String): String {

    val extension = MimeTypeMap.getFileExtensionFromUrl(path)
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)!!

    val collection = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
      MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    val values = ContentValues().apply {
      put(MediaStore.Images.Media.DISPLAY_NAME, name)
      put(MediaStore.Images.Media.MIME_TYPE, mimeType)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + getApplicationName())
        put(MediaStore.Images.Media.IS_PENDING, 1)
      }
    }

    val resolver = registrar.activeContext().applicationContext.contentResolver
    val uri = resolver.insert(collection, values)!!

    return try {
      resolver.openOutputStream(uri).use { os ->
        File(path).inputStream().use { it.copyTo(os!!) }
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
      }
      return uri.toString()
    } catch (ex: IOException) {
      Log.e("MediaStore", ex.message, ex)
      ""
    }
  }

//  private fun saveFileToGallery(filePath: String): String {
//    val context = registrar.activeContext().applicationContext
//    return try {
//      val originalFile = File(filePath)
//      val file = generateFile(originalFile.extension)
//      originalFile.copyTo(file)
//
//      val uri = Uri.fromFile(file)
//      context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
//      return uri.toString()
//    } catch (e: IOException) {
//      e.printStackTrace()
//      ""
//    }
//  }

  private fun getApplicationName(): String {
    val context = registrar.activeContext().applicationContext
    var ai: ApplicationInfo? = null
    try {
        ai = context.packageManager.getApplicationInfo(context.packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
    }
    var appName: String
    appName = if (ai != null) {
      val charSequence = context.packageManager.getApplicationLabel(ai)
      StringBuilder(charSequence.length).append(charSequence).toString()
    } else {
      "image_gallery_saver"
    }
    return  appName
  }


}
