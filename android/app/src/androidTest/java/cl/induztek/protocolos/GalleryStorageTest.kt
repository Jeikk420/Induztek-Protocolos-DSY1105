package cl.induztek.protocolos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.induztek.protocolos.caseapp.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile

@RunWith(AndroidJUnit4::class)
class GalleryStorageTest {
 private val context = ApplicationProvider.getApplicationContext<Context>()
 private fun image(width: Int = 80, height: Int = 40): File {
  val file = File.createTempFile("test-", ".jpg", context.cacheDir)
  val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
  bitmap.recycle()
  return file
 }
 @Test fun fotoPermaneceAlBorrarOrigenYReabrirBase() = runTest {
  val source = image()
  val name = "gallery-${System.nanoTime()}.db"
  val photo = File(PhotoStorage.importImage(context, Uri.fromFile(source)))
  fun open() = Room.databaseBuilder(context, CaseDatabase::class.java, name).build()
  try {
   val first = open()
   val equipment = Demo.data().equipment.first().copy(photoPath = photo.path)
   try { first.dao().equipment(equipment) } finally { first.close() }
   source.delete()
   val second = open()
   try {
    val restored = CaseRepository(second, context).data().equipment.single()
    assertEquals(photo.path, restored.photoPath)
    val bitmap = BitmapFactory.decodeFile(restored.photoPath)
    assertNotNull(bitmap)
    bitmap?.recycle()
   } finally { second.close() }
  } finally { source.delete(); photo.delete(); context.deleteDatabase(name) }
 }
 @Test fun orientacionExifSeRespeta() {
  val source = image()
  InstrumentationRegistry.getInstrumentation().context.assets.open("orientation90.jpg").use { input ->
   source.outputStream().use { input.copyTo(it) }
  }
  var output: File? = null
  try {
   val photo = File(PhotoStorage.importImage(context, Uri.fromFile(source)))
   output = photo
   val bitmap = BitmapFactory.decodeFile(photo.path)
   assertEquals(40, bitmap.width)
   assertEquals(80, bitmap.height)
   bitmap.recycle()
  } finally { source.delete(); output?.delete() }
 }
 @Test fun imagenGrandeSeReduce() {
  val source = image(3200, 1800)
  var output: File? = null
  try {
   val photo = File(PhotoStorage.importImage(context, Uri.fromFile(source)))
   output = photo
   val bitmap = BitmapFactory.decodeFile(photo.path)
   assertTrue(bitmap.width <= 1600 && bitmap.height <= 1600)
   bitmap.recycle()
  } finally { source.delete(); output?.delete() }
 }
 @Test fun archivoDanadoSeRechazaSinRestos() {
  val source = File.createTempFile("bad-", ".jpg", context.cacheDir).apply { writeText("no es una imagen") }
  val before = File(context.filesDir, "photos").list()?.toSet() ?: emptySet()
  try {
   try { PhotoStorage.importImage(context, Uri.fromFile(source)); fail("Debió rechazar el archivo") }
   catch (_: IllegalArgumentException) { }
   assertEquals(before, File(context.filesDir, "photos").list()?.toSet() ?: emptySet<String>())
  } finally { source.delete() }
 }
 @Test fun archivoMayorA20MbSeRechaza() {
  val source = File.createTempFile("large-", ".jpg", context.cacheDir)
  RandomAccessFile(source, "rw").use { it.setLength(21L * 1024 * 1024) }
  try {
   try { PhotoStorage.importImage(context, Uri.fromFile(source)); fail("Debió rechazar el tamaño") }
   catch (error: IllegalArgumentException) { assertTrue(error.message!!.contains("20 MB")) }
  } finally { source.delete() }
 }
}
