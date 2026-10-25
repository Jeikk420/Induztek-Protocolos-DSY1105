package cl.induztek.protocolos.caseapp

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object CasePdf {
 fun acta(context:Context,v:Visit,d:Data):File {
  require(v.closedAt!=null && v.receipt!=null) { "Primero cierra la visita y registra la recepción." }
  val protocols:List<Protocol> = if(v.actaJson.isNotBlank()) Gson().fromJson(v.actaJson,object:TypeToken<List<Protocol>>() {}.type) else d.protocols.filter { it.visitId==v.id && it.state!=States.VOID }
  val lines=mutableListOf("ACTA DE PRUEBA EN TERRENO","PENDIENTE DE FORMALIZACIÓN","OT: ${v.ot}","Cliente: ${d.clients.find { it.id==v.clientId }?.name ?: v.clientId}","Instalación: ${d.installations.find { it.id==v.installationId }?.name ?: v.installationId}","Fecha: ${v.date} · ${v.kind}")
  if(v.latitude!=null) lines.add("Ubicación: ${v.latitude}, ${v.longitude} · Precisión ${v.accuracy} m")
  for(p in protocols) lines.addAll(protocolLines(p,d,false))
  lines.addAll(listOf("CONSTANCIA DE RECEPCIÓN","Recibió: ${v.receipt.name}","RUT: ${v.receipt.rut} · Cargo: ${v.receipt.cargo}","Fecha y hora de recepción: ${moment(v.receipt.receivedAt)}","Respaldo preliminar. La revisión y aprobación de ingeniería se realiza después."))
  return create(context,"acta-${v.id}",lines)
 }
 fun protocol(context:Context,p:Protocol,d:Data):File {
  require(p.state==States.FINAL) { "Este protocolo todavía está pendiente de formalización." }
  val lines=mutableListOf("PROTOCOLO FORMALIZADO","Cliente: ${d.clients.find { it.id==d.visits.find { v -> v.id==p.visitId }?.clientId }?.name ?: ""}")
  lines.addAll(protocolLines(p,d,true));lines.add("Formalizado por: ${d.users.find { it.id==p.formalizedBy }?.name ?: p.formalizedBy}");lines.add("Fecha de aprobación: ${moment(requireNotNull(p.formalizedAt))}")
  d.protocols.find { it.replacesId==p.id && it.state!=States.VOID }?.let { lines.add("Reemplazado por nº ${it.number}. El original se mantiene en el historial.") }
  return create(context,"protocolo-${p.id}",lines)
 }
 private fun protocolLines(p:Protocol,d:Data,formal:Boolean):List<String> {
  val e=d.equipment.find { it.id==p.equipmentId }
  val lines=mutableListOf("", "PROTOCOLO Nº ${p.number}","Equipo: ${e?.tag ?: p.equipmentId} · ${e?.type ?: ""}","Marca / modelo: ${e?.brand} ${e?.model} · Serie: ${e?.serial}","Instalación: ${d.installations.find { it.id==e?.installationId }?.name ?: ""}","Responsable: ${p.ownerName}","Prueba: ${moment(p.testedAt)}","Temperatura: ${p.temperature} °C · Humedad: ${p.humidity} %","Resultado global: ${p.result}",if(formal) "Estado: Formalizado" else "Estado: Pendiente de formalización")
  if(e!=null) {
   lines.add("Placa: año ${e.year} · Tensión nominal ${e.highVoltage} kV")
   if(e.type=="TRANSFORMADOR") lines.add("Potencia: ${e.power} kVA · Tensión secundaria: ${e.lowVoltage} kV")
   if(e.current.isNotBlank()) lines.add("Corriente nominal: ${e.current} A")
   if(e.contactMax.isNotBlank()) lines.add("Máximo de contactos: ${e.contactMax} µΩ")
  }
  for(t in p.tests) {
   lines.add(Types.testLabel(t.type).uppercase())
   if(t.notPerformed) { lines.add("No realizada. Motivo: ${t.reason}");continue }
   val i=d.instruments.find { it.id==t.instrumentId };lines.add("Instrumento: ${i?.name ?: t.instrumentId} · Serie: ${i?.serial ?: "ver registro"}")
   if(t.afterMaintenance) { lines.add("Mediciones previas al mantenimiento:");for(r in t.previousValues) lines.add("${r.label}: ${r.text} ${r.unit}");lines.add("Mediciones posteriores al mantenimiento:") }
   for(r in t.values) if(r.text.isNotBlank()) lines.add("${r.label}: ${r.text} ${r.unit}")
   if(t.type=="RELE" && !t.relayOperates) lines.add("Relé NO opera.")
   if(e!=null) { lines.addAll(Rules.calculated(t,e));lines.add("Resultado de prueba: ${Rules.testResult(t,e)}") }
   if(t.comment.isNotBlank()) lines.add("Comentario: ${t.comment}")
  }
  if(p.notes.isNotBlank()) lines.add("Observaciones: ${p.notes}")
  if(p.maintenance.isNotBlank()) lines.add("Mantenimiento: ${p.maintenance}")
  if(p.historicalComment.isNotBlank()) lines.add("Comentario de comparación: ${p.historicalComment}")
  if(p.replacesId.isNotBlank()) lines.add("Reemplaza al protocolo: ${d.protocols.find { it.id==p.replacesId }?.number ?: p.replacesId}")
  return lines
 }
 private fun create(context:Context,name:String,lines:List<String>):File {
  val directory=File(context.cacheDir,"protocols").apply { mkdirs() }
  val file=File(directory,"$name.pdf")
  val pdf=PdfDocument();val paint=Paint(Paint.ANTI_ALIAS_FLAG)
  val teal=Color.rgb(0,127,139);val ink=Color.rgb(32,49,58)
  val muted=Color.rgb(91,110,120);val pale=Color.rgb(241,245,247)
  var page:PdfDocument.Page?=null;var y=132f;var count=0;var shaded=false
  val title=lines.firstOrNull() ?: "RESPALDO DE PRUEBAS"
  val preliminary=lines.any { it=="PENDIENTE DE FORMALIZACIÓN" }
  fun text(value:String,x:Float,baseline:Float,size:Float=10f,bold:Boolean=false,color:Int=ink) {
   paint.textSize=size;paint.typeface=if(bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT;paint.color=color
   page!!.canvas.drawText(value,x,baseline,paint)
  }
  fun rect(left:Float,top:Float,right:Float,bottom:Float,color:Int) {
   paint.color=color;page!!.canvas.drawRect(left,top,right,bottom,paint)
  }
  fun wrap(value:String,width:Float,size:Float,bold:Boolean=false):List<String> {
   paint.textSize=size;paint.typeface=if(bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
   val result=mutableListOf<String>()
   for(paragraph in value.replace('\t',' ').split('\n')) {
    var rest=paragraph
    do {
     var length=paint.breakText(rest,true,width,null).coerceAtMost(rest.length)
     if(length==0 && rest.isNotEmpty()) length=1
     if(length<rest.length) { val space=rest.take(length).lastIndexOf(' ');if(space>0) length=space }
     result.add(rest.take(length));rest=rest.drop(length).trimStart()
    } while(rest.isNotEmpty())
   }
   return result
  }
  fun finish() {
   page?.let {
    rect(36f,794f,559f,795f,Color.rgb(210,223,228))
    text("Demostración académica · Datos ficticios",36f,809f,8f,color=muted)
    text("Página $count",509f,809f,8f,color=muted)
    text(if(preliminary) "Acta preliminar: no sustituye la aprobación de ingeniería." else "Protocolo formalizado · Conservar como respaldo de la visita.",36f,822f,8f,color=muted)
    pdf.finishPage(it)
   }
  }
  fun next() {
   if(page!=null) finish()
   count++;page=pdf.startPage(PdfDocument.PageInfo.Builder(595,842,count).create())
   rect(0f,0f,595f,7f,teal)
   text("INDUZTEK",36f,42f,23f,true,teal)
   text("PROTOCOLOS / PRUEBAS EN TERRENO",36f,58f,8f,true,muted)
   text("RESPALDO TÉCNICO",447f,40f,9f,true,muted)
   text(if(count==1) "REGISTRO DE VISITA" else "CONTINUACIÓN",447f,54f,8f,color=muted)
   text(title,36f,84f,15f,true)
   rect(36f,96f,559f,119f,if(preliminary) Color.rgb(255,245,219) else Color.rgb(224,242,238))
   text(if(preliminary) "PENDIENTE DE FORMALIZACIÓN" else "FORMALIZADO POR INGENIERÍA",46f,111f,9f,true,if(preliminary) Color.rgb(133,91,20) else Color.rgb(24,107,85))
   y=132f
  }
  fun room(height:Float) { if(y+height>779f) next() }
  fun section(value:String,keep:Float=0f) {
   val rows=wrap(value,499f,10f,true);val height=rows.size*13f+12f
   room(height+keep);y+=5f
   rect(36f,y,559f,y+height-5f,pale);rect(36f,y,39f,y+height-5f,teal)
   rows.forEachIndexed { i,row -> text(row,47f,y+14f+i*13f,10f,true,teal) }
   y+=height+3f;shaded=false
  }
  fun row(label:String,value:String) {
   val left=wrap(label,179f,9f,true);val right=wrap(value,316f,9.5f)
   val total=maxOf(left.size,right.size)
   val fullHeight=total*12f+6f
   if(fullHeight<=600f) room(fullHeight)
   
   var offset=0
   while(offset<total) {
    room(20f)
    val capacity=((779f-y-6f)/12f).toInt().coerceAtLeast(1)
    val countRows=minOf(total-offset,capacity);val height=countRows*12f+6f
    if(shaded) rect(36f,y,559f,y+height,pale)
    for(i in 0 until countRows) {
     left.getOrNull(offset+i)?.let { text(it,44f,y+12f+i*12f,9f,true,muted) }
     right.getOrNull(offset+i)?.let { text(it,235f,y+12f+i*12f,9.5f) }
    }
    y+=height;offset+=countRows;shaded=!shaded
    if(offset<total) { next();text("$label (continuación)",36f,y+10f,9f,true,muted);y+=18f }
   }
  }
  fun paragraph(value:String) {
   for(line in wrap(value,507f,10f)) { room(16f);text(line,44f,y+12f);y+=15f }
   y+=4f
  }
  try {
   next()
   for(line in lines.drop(1)) {
    if(line.isBlank() || line=="PENDIENTE DE FORMALIZACIÓN") continue
    when {
     line.startsWith("Resultado global:") -> {
      val result=line.substringAfter(':').trim();room(37f)
      val rejected=result.contains("RECHAZ",true)
      val approved=result=="APROBADO"
      val background=if(rejected) Color.rgb(253,233,234) else if(approved) Color.rgb(224,242,238) else Color.rgb(255,245,219)
      val foreground=if(rejected) Color.rgb(166,42,50) else if(approved) Color.rgb(24,107,85) else Color.rgb(133,91,20)
      rect(36f,y+3f,559f,y+33f,background)
      text("RESULTADO GLOBAL",46f,y+22f,9f,true,foreground);text(result,235f,y+22f,12f,true,foreground);y+=38f
     }
     line.startsWith("Observaciones:") || line.startsWith("Mantenimiento:") || line.startsWith("Comentario:") || line.startsWith("Comentario de comparación:") -> {
      paragraph(line)
     }
     line=="CONSTANCIA DE RECEPCIÓN" -> section(line,102f)
     line==line.uppercase() && !line.contains(':') && !line.contains('=') -> section(line,42f)
     line.contains(':') -> row(line.substringBefore(':'),line.substringAfter(':').trim())
     else -> paragraph(line)
    }
   }
   finish();file.outputStream().use { pdf.writeTo(it) }
  } finally { pdf.close() }
  return file
 }
 fun share(context:Context,file:File) {
  val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
  val intent=Intent(Intent.ACTION_SEND).apply { type="application/pdf";putExtra(Intent.EXTRA_STREAM,uri);putExtra(Intent.EXTRA_SUBJECT,"Respaldo Induztek");clipData=ClipData.newRawUri("Respaldo PDF",uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
  context.startActivity(Intent.createChooser(intent,"Entregar respaldo al cliente"))
 }
}
