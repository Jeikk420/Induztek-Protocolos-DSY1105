package cl.induztek.protocolos

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.induztek.protocolos.caseapp.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PdfExportTest {
 @Test fun protocoloLargoProducePdfLegibleYCompartible() {
  val context=ApplicationProvider.getApplicationContext<Context>()
  val data=Demo.data()
  val protocol=data.protocols.first{it.state==States.FINAL}.copy(notes=(1..120).joinToString("\n"){"Observación de prueba número $it: revisar mediciones y mantenimiento del equipo."})
  val file=CasePdf.protocol(context,protocol,data)
  assertTrue(file.length()>1000)
  ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
   PdfRenderer(fd).use { pdf -> assertTrue("El documento debe paginar el texto largo",pdf.pageCount>1);pdf.openPage(0).use { assertEquals(595,it.width);assertEquals(842,it.height) } }
  }
  val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
  context.contentResolver.openInputStream(uri)!!.use { stream -> val header=ByteArray(4);assertEquals(4,stream.read(header));assertEquals("%PDF",String(header)) }
  file.copyTo(File(context.filesDir,"qa-protocolo.pdf"),overwrite=true)
 }
 @Test fun actaCerradaGeneraRespaldoNativo() {
  val context=ApplicationProvider.getApplicationContext<Context>();val data=Demo.data()
  val visit=data.visits.first{it.closedAt!=null && it.actaJson.isNotBlank()}
  val file=CasePdf.acta(context,visit,data)
  ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use { fd -> PdfRenderer(fd).use { assertTrue(it.pageCount>=1) } }
  file.copyTo(File(context.filesDir,"qa-acta.pdf"),overwrite=true)
 }
 @Test fun borradorNoSeExportaComoFormalizado() {
  val context=ApplicationProvider.getApplicationContext<Context>();val data=Demo.data()
  try { CasePdf.protocol(context,data.protocols.first().copy(state=States.DRAFT),data);fail("No debe exportar un borrador como documento final") } catch(_:IllegalArgumentException) { }
 }
 @Test fun visitaAbiertaNoProduceActaFinal() {
  val context=ApplicationProvider.getApplicationContext<Context>();val data=Demo.data()
  try { CasePdf.acta(context,data.visits.first().copy(closedAt=null,receipt=null),data);fail("Primero debe cerrarse la visita") } catch(_:IllegalArgumentException) { }
 }
}
