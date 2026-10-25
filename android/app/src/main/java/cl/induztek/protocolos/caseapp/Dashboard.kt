@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package cl.induztek.protocolos.caseapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable internal fun TechnicalDashboard(d:Data,u:User,pending:List<Pending>,busy:Boolean,open:(String)->Unit) {
 val client=u.role==Roles.CLIENT
 val canVisit=u.role in listOf(Roles.TECH,Roles.ENGINEER)
 val waiting=d.protocols.count { it.state in listOf(States.REGISTERED,States.OBSERVED) }
 val recent=d.visits.sortedByDescending { it.date }.take(2)
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=18.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Text(if(client) "Resumen de tus equipos" else "Resumen de trabajo",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
  Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary,Color(0xFF006A75))),RoundedCornerShape(22.dp))) {
   Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
     Surface(color=Color.White.copy(alpha=0.14f),shape=RoundedCornerShape(14.dp)) { Icon(Icons.AutoMirrored.Filled.List,null,Modifier.padding(12.dp).size(25.dp),tint=Color.White) }
     Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
      Text(if(client) "Tus visitas y actas" else "Tus visitas",color=Color.White,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
      Text(if(canVisit) "Todo listo para registrar" else if(client) "Consulta el respaldo de tu empresa" else "Supervisa el trabajo en terreno",color=Color.White.copy(alpha=0.9f),style=MaterialTheme.typography.bodyMedium)
     }
    }
    Button(onClick={open(if(canVisit) "visit/new" else "visits")},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=49.dp),shape=RoundedCornerShape(13.dp),colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=MaterialTheme.colorScheme.primary,disabledContainerColor=Color.White.copy(alpha=0.7f),disabledContentColor=MaterialTheme.colorScheme.primary)) {
     Icon(if(canVisit) Icons.Default.Add else Icons.AutoMirrored.Filled.List,null,Modifier.size(21.dp));Spacer(Modifier.width(8.dp));Text(if(canVisit) "Nueva visita" else if(client) "Ver mis actas" else "Ver visitas",fontWeight=FontWeight.SemiBold)
    }
   }
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   DashboardStat("Equipos",d.equipment.size,Icons.Default.Build,Modifier.weight(1f)) { open("equipment") }
   DashboardStat(if(client) "Actas" else "Visitas",d.visits.size,Icons.Default.DateRange,Modifier.weight(1f)) { open("visits") }
   DashboardStat("Pendientes",waiting,Icons.Default.Info,Modifier.weight(1f)) { open("pending") }
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
   DashboardShortcut("Equipos",Icons.Default.Build,Modifier.weight(1f)) { open("equipment") }
   DashboardShortcut("Protocolos",Icons.AutoMirrored.Filled.List,Modifier.weight(1f)) { open("protocols") }
  }
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
   Text("Actividad reciente",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   TextButton(onClick={open("visits")},contentPadding=PaddingValues(horizontal=4.dp,vertical=8.dp)) { Text("Ver todas") }
  }
  Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
   if(recent.isEmpty()) {
    Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(17.dp)) {
     Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
      Text(if(client) "Aún no hay actas disponibles" else "Aún no hay visitas",fontWeight=FontWeight.SemiBold)
      Text(if(canVisit) "Pulsa Nueva visita para comenzar." else "Los registros aparecerán aquí cuando estén disponibles.",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
     }
    }
   }
   for(v in recent) DashboardVisitRow(v,d) { open("visit/${v.id}") }
  }
  if(!client) {
   Row(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically) {
    Icon(Icons.Default.CheckCircle,null,Modifier.size(17.dp),tint=MaterialTheme.colorScheme.primary)
    Text(if(pending.isEmpty()) "Tus registros se guardan en el dispositivo" else "${pending.size} cambios guardados, pendientes de envío",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
   val late=d.protocols.count { Rules.late(it) }
   if(late>0) Surface(color=Color(0xFFFFF4D8),shape=RoundedCornerShape(14.dp)) {
    Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.spacedBy(9.dp)) { Icon(Icons.Default.Info,null,Modifier.size(20.dp),tint=Color(0xFF805A0A));Text(if(late==1) "1 protocolo supera el plazo de formalización. Revísalo en Protocolos." else "$late protocolos superan el plazo de formalización. Revísalos en Protocolos.",style=MaterialTheme.typography.bodySmall,color=Color(0xFF6A4B10)) }
   }
   val finalized=d.protocols.filter { it.formalizedAt!=null }
   if(finalized.isNotEmpty()) Text("Tiempo medio hasta formalizar: "+String.format(Locale.getDefault(),"%.1f",finalized.map { (it.formalizedAt!!-it.testedAt)/3600000.0 }.average())+" horas",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
  Text("Demostración académica · Datos ficticios",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}

@Composable private fun DashboardStat(label:String,count:Int,icon:ImageVector,modifier:Modifier,click:()->Unit) {
 Card(onClick=click,modifier=modifier.heightIn(min=108.dp),shape=RoundedCornerShape(17.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(defaultElevation=0.dp)) {
  Column(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=13.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
   Icon(icon,null,Modifier.size(21.dp),tint=MaterialTheme.colorScheme.primary)
   Text(count.toString(),fontSize=27.sp,lineHeight=31.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
   Text(label,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
  }
 }
}
@Composable private fun DashboardShortcut(label:String,icon:ImageVector,modifier:Modifier,click:()->Unit) {
 Card(onClick=click,modifier=modifier.heightIn(min=66.dp),shape=RoundedCornerShape(17.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
  Row(Modifier.fillMaxWidth().padding(horizontal=13.dp,vertical=18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   Icon(icon,null,Modifier.size(23.dp),tint=MaterialTheme.colorScheme.primary);Text(label,Modifier.weight(1f),style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.SemiBold);Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}
@Composable private fun DashboardVisitRow(v:Visit,d:Data,click:()->Unit) {
 val protocols=d.protocols.filter { it.visitId==v.id && it.state!=States.VOID }
 val formalized=v.closedAt!=null && protocols.isNotEmpty() && protocols.all { it.state==States.FINAL }
 val label=if(v.closedAt==null) "Abierta" else if(formalized) "Formalizados" else if(protocols.isEmpty()) "Acta disponible" else "Por revisar"
 val tone=if(formalized) Color(0xFF206B46) else if(v.closedAt==null) MaterialTheme.colorScheme.primary else Color(0xFF805A0A)
 val tint=if(formalized) Color(0xFFDDF4E7) else if(v.closedAt==null) Color(0xFFDCF0F3) else Color(0xFFFFEAB0)
 val installation=d.installations.find { it.id==v.installationId }?.name ?: "Instalación"
 val date=Rules.iso(v.date)?.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: v.date
 Card(onClick=click,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(17.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
  Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(11.dp)) {
   Icon(Icons.AutoMirrored.Filled.List,null,Modifier.size(25.dp),tint=MaterialTheme.colorScheme.primary)
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
    Text("${v.ot} · $installation",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
    Text(date,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
   Surface(color=tint,shape=RoundedCornerShape(20.dp)) { Text(label,Modifier.padding(horizontal=10.dp,vertical=6.dp),color=tone,style=MaterialTheme.typography.labelSmall,maxLines=2) }
   Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,null,Modifier.size(17.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}
