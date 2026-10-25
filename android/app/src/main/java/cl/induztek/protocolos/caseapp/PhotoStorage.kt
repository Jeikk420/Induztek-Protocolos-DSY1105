package cl.induztek.protocolos.caseapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import java.io.File

object PhotoStorage {
 fun importImage(context: Context, uri: Uri): String {
  val directory = File(context.filesDir, "photos").apply { mkdirs() }
  val source = File.createTempFile("entrada-", ".tmp", directory)
  var output: File? = null
  var bitmap: Bitmap? = null
  try {
   val input = context.contentResolver.openInputStream(uri)
    ?: throw IllegalArgumentException("No se pudo leer la imagen.")
   input.use { stream ->
    source.outputStream().use { target ->
     val buffer = ByteArray(8192)
     var total = 0L
     var count = stream.read(buffer)
     while (count != -1) {
      total += count
      require(total <= 20L * 1024 * 1024) { "Elige una imagen de hasta 20 MB." }
      target.write(buffer, 0, count)
      count = stream.read(buffer)
     }
    }
   }
   val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
   BitmapFactory.decodeFile(source.path, options)
   require(options.outWidth > 0 && options.outHeight > 0) { "El archivo no es una imagen válida." }
   var sample = 1
   while (options.outWidth / sample > 1600 || options.outHeight / sample > 1600) sample *= 2
   val decoded = BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply { inSampleSize = sample })
    ?: throw IllegalArgumentException("No se pudo abrir la imagen.")
   bitmap = decoded
   val orientation = try {
    ExifInterface(source.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
   } catch (_: Exception) { 1 }
   val matrix = Matrix()
   when (orientation) {
    2 -> matrix.setScale(-1f, 1f)
    3 -> matrix.setRotate(180f)
    4 -> matrix.setScale(1f, -1f)
    5 -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
    6 -> matrix.setRotate(90f)
    7 -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
    8 -> matrix.setRotate(-90f)
   }
   val image = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
   if (image !== decoded) decoded.recycle()
   bitmap = image
   val file = File.createTempFile("galeria-", ".jpg", directory)
   output = file
   file.outputStream().use { require(image.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
   return file.absolutePath
  } catch (error: Exception) {
   output?.delete()
   throw error
  } finally {
   source.delete()
   bitmap?.recycle()
  }
 }
}
