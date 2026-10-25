@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class,androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package cl.induztek.protocolos.caseapp

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import cl.induztek.protocolos.R
import com.google.gson.Gson
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun fmt(n:Double)=java.math.BigDecimal.valueOf(n).stripTrailingZeros().toPlainString()
fun moment(t:Long)=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Santiago")).format(Instant.ofEpochMilli(t))
private val gson=Gson()
@Composable fun CaseLogo() { Image(painterResource(R.drawable.induztek_company_logo),"Induztek Protocolos",Modifier.fillMaxWidth().height(130.dp),contentScale=ContentScale.Fit) }
@Composable private fun Page(content:@Composable ColumnScope.()->Unit) {
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp),content=content)
}
@Composable private fun CardBlock(title:String,content:@Composable ColumnScope.()->Unit) {
 ElevatedCard(Modifier.fillMaxWidth(),colors=CardDefaults.elevatedCardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) { Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);content() }
 }
}
@Composable private fun Notice(text:String,error:Boolean=false) {
 Surface(color=if(error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth()) {
  Row(Modifier.padding(14.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) { Icon(if(error) Icons.Default.Warning else Icons.Default.Info,null);Text(text,Modifier.weight(1f)) }
 }
}
@Composable private fun ErrorText(error:String?) {
 AnimatedVisibility(visible=error!=null,enter=fadeIn()+expandVertically(),exit=fadeOut()+shrinkVertically()) {
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { Icon(Icons.Default.Warning,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.error);Text(error ?: "",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall) }
 }
}
@Composable private fun Field(label:String,text:String,change:(String)->Unit,error:String?=null,numeric:Boolean=false,lines:Int=1,password:Boolean=false) {
 OutlinedTextField(value=text,onValueChange=change,label={Text(label)},modifier=Modifier.fillMaxWidth(),isError=error!=null,singleLine=lines==1,minLines=lines,
 keyboardOptions=KeyboardOptions(keyboardType=if(password) KeyboardType.Password else if(numeric) KeyboardType.Decimal else KeyboardType.Text),
 visualTransformation=if(password) PasswordVisualTransformation() else VisualTransformation.None,
 leadingIcon={Icon(if(error!=null) Icons.Default.Warning else if(numeric) Icons.Default.Edit else Icons.Default.Edit,null)},
 supportingText={if(error!=null) ErrorText(error)})
}
@Composable private fun Choice(label:String,value:String,options:List<Pair<String,String>>,select:(String)->Unit,error:String?=null) {
 var expanded by remember { mutableStateOf(false) }
 Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
  Text(label,style=MaterialTheme.typography.labelLarge)
  Box { OutlinedButton(onClick={expanded=true},enabled=options.isNotEmpty(),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text(options.find { it.first==value }?.second ?: "Seleccionar",Modifier.weight(1f));Icon(Icons.Default.KeyboardArrowDown,null) }
   DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) { for((id,name) in options) DropdownMenuItem(text={Text(name)},onClick={expanded=false;select(id)}) }
  };ErrorText(error)
 }
}
@Composable private fun ButtonFull(text:String,busy:Boolean=false,click:()->Unit) {
 Button(onClick=click,enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) { Icon(Icons.Default.CheckCircle,null);Spacer(Modifier.width(8.dp));Text(text) }
}
@Composable private fun PdfShareButton(label:String,busy:Boolean,notify:(String)->Unit,create:()->java.io.File) {
 val context=LocalContext.current;val scope=rememberCoroutineScope()
 var preparing by remember { mutableStateOf(false) }
 ButtonFull(if(preparing) "Preparando PDF…" else label,busy || preparing) {
  preparing=true
  scope.launch {
   try { val file=withContext(Dispatchers.IO) { create() };CasePdf.share(context,file) }
   catch(ex:Exception) { if(ex is kotlinx.coroutines.CancellationException) throw ex;notify("No se pudo compartir el PDF: ${ex.message}") }
   finally { preparing=false }
  }
 }
}
@Composable private fun Tag(text:String,warning:Boolean=false) {
 Surface(color=if(warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,shape=MaterialTheme.shapes.small) { Text(text,Modifier.padding(horizontal=10.dp,vertical=6.dp),style=MaterialTheme.typography.labelMedium) }
}
@Composable private fun Link(title:String,detail:String,click:()->Unit) {
 OutlinedCard(onClick=click,modifier=Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) { Text(title,fontWeight=FontWeight.SemiBold);Text(detail,style=MaterialTheme.typography.bodySmall) };Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,null) } }
}
@Composable private fun Empty(text:String) { Notice(text) }
private fun stateLabel(p:Protocol,u:User)=if(u.role==Roles.CLIENT && p.state!=States.FINAL) "Pendiente de formalización" else States.label(p.state)

@Composable fun CaseApp(vm:CaseViewModel) {
 val all by vm.data.collectAsStateWithLifecycle();val user by vm.user.collectAsStateWithLifecycle();val busy by vm.busy.collectAsStateWithLifecycle();val ready by vm.ready.collectAsStateWithLifecycle()
 val message by vm.message.collectAsStateWithLifecycle();val pending by vm.pending.collectAsStateWithLifecycle();val errors by vm.errors.collectAsStateWithLifecycle()
 val snackbar=remember { SnackbarHostState() }
 LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it);vm.clearMessage() } }
 val u=user
 if(!ready) { Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator() };return }
 if(u==null) { Scaffold(snackbarHost={SnackbarHost(snackbar)}) { pad -> Box(Modifier.padding(pad)) { LoginScreen(all,errors,vm) } };return }
 val d=Rules.visible(u,all);val nav=rememberNavController();val entry by nav.currentBackStackEntryAsState();val route=entry?.destination?.route ?: "home"
 val roots=setOf("home","visits","equipment","more")
 LaunchedEffect(route) { vm.clearErrors() }
 val title=when(route) { "home" -> "Inicio";"protocols" -> "Protocolos";"visit/new" -> "Nueva visita";"visits" -> if(u.role==Roles.CLIENT) "Mis actas" else "Visitas";"equipment" -> "Equipos";"more" -> "Mi perfil";"pending" -> "Por formalizar";"clients" -> "Clientes";"installations" -> "Instalaciones";"instruments" -> "Instrumentos";"accounts" -> "Usuarios";"protocol/{id}" -> "Protocolo";"edit/{id}" -> "Registrar pruebas";"visit/{id}" -> "Detalle de visita";else -> "Nuevo registro" }
 Scaffold(topBar={
  TopAppBar(colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background),title={
   if(route=="home") Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
    Image(painterResource(R.mipmap.ic_launcher_induztek),null,Modifier.size(39.dp),contentScale=ContentScale.Fit)
    Column { Text("INDUZTEK",fontSize=17.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary);Text("Protocolos",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
   } else Column { Text(title,fontWeight=FontWeight.Bold);Text("${u.name.substringBefore(' ')} · ${Roles.label(u.role)}",style=MaterialTheme.typography.labelSmall) }
  },navigationIcon={if(route !in roots) IconButton(onClick={nav.popBackStack()}) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Volver") }},actions={
   if(route=="home") IconButton(onClick={nav.navigate("more") { popUpTo(nav.graph.startDestinationId) { saveState=true };launchSingleTop=true;restoreState=true }},modifier=Modifier.semantics { contentDescription="Mi perfil" }) {
    Surface(shape=CircleShape,color=MaterialTheme.colorScheme.secondaryContainer) { Box(Modifier.size(38.dp),contentAlignment=Alignment.Center) { Text(u.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U",fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSecondaryContainer) } }
   } else IconButton(onClick=vm::logout) { Icon(Icons.AutoMirrored.Filled.ExitToApp,"Cerrar sesión") }
  })
 },snackbarHost={SnackbarHost(snackbar)},bottomBar={if(route in roots) NavigationBar(containerColor=MaterialTheme.colorScheme.surface,tonalElevation=0.dp) {
  val tabs=listOf(Triple("home","Inicio",Icons.Default.Home),Triple("equipment","Equipos",Icons.Default.Build),Triple("visits",if(u.role==Roles.CLIENT) "Actas" else "Visitas",Icons.AutoMirrored.Filled.List),Triple("more","Perfil",Icons.Default.Person))
  for((target,label,icon) in tabs) NavigationBarItem(selected=route==target,onClick={nav.navigate(target) { popUpTo(nav.graph.startDestinationId) { saveState=true };launchSingleTop=true;restoreState=true }},icon={Icon(icon,label)},label={Text(label)},colors=NavigationBarItemDefaults.colors(indicatorColor=MaterialTheme.colorScheme.primaryContainer,selectedIconColor=MaterialTheme.colorScheme.primary,selectedTextColor=MaterialTheme.colorScheme.primary))
 }}) { padding ->
  Column(Modifier.fillMaxSize().padding(padding),horizontalAlignment=Alignment.CenterHorizontally) {
   AnimatedVisibility(busy) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
   Box(Modifier.widthIn(max=900.dp).weight(1f)) {
    NavHost(nav,startDestination="home",enterTransition={fadeIn(tween(220))},exitTransition={fadeOut(tween(160))}) {
     composable("home") { HomeScreen(d,u,pending,busy,nav) }
     composable("visits") { VisitsScreen(d,u,nav) }
     composable("equipment") { EquipmentScreen(d,u,nav) }
     composable("more") { MoreScreen(d,u,pending,vm,nav,busy,errors) }
     composable("protocols") { ProtocolListScreen(d,u,nav) }
     composable("pending") { ProtocolListScreen(d,u,nav,true) }
     composable("clients") { Page { if(Rules.canCreate(u)) ButtonFull("Registrar cliente") { nav.navigate("client/new") };for(c in d.clients) CardBlock(c.name) { Text("RUT: ${c.rut}");Text("${c.contact} · ${c.email}");Text(c.phone) } } }
     composable("installations") { Page { if(Rules.canCreate(u)) ButtonFull("Registrar instalación") { nav.navigate("installation/new") };for(i in d.installations) CardBlock(i.name) { Text(d.clients.find { it.id==i.clientId }?.name ?: "");Text("${i.location} · ${i.comuna}");Text(i.access) } } }
     composable("instruments") { InstrumentsScreen(d,u,nav) }
     composable("accounts") { AccountsScreen(d,u,nav) }
     composable("client/new") { ClientForm(d,vm,busy) { nav.popBackStack() } }
     composable("installation/new") { InstallationForm(d,vm,busy) { nav.popBackStack() } }
     composable("equipment/new") { EquipmentForm(d,vm,busy) { nav.popBackStack() } }
     composable("instrument/{id}") { b -> InstrumentForm(d,b.arguments?.getString("id") ?: "new",vm,busy) { nav.popBackStack() } }
     composable("account/{id}") { b -> AccountForm(d,b.arguments?.getString("id") ?: "new",vm,busy) { nav.popBackStack() } }
     composable("visit/new") { VisitForm(d,u,vm,busy) { id -> nav.popBackStack();nav.navigate("visit/$id") } }
     composable("visit/{id}") { b -> VisitDetail(d,u,b.arguments?.getString("id") ?: "",vm,nav,busy,errors) }
     composable("equipment/{id}") { b -> EquipmentDetail(d,u,b.arguments?.getString("id") ?: "",nav) }
     composable("edit/{id}") { b -> ProtocolEditor(d,u,b.arguments?.getString("id") ?: "",vm,busy) { nav.popBackStack() } }
     composable("protocol/{id}") { b -> ProtocolDetail(d,u,b.arguments?.getString("id") ?: "",vm,nav,busy) }
    }
   }
  }
 }
}
@Composable private fun LoginScreen(d:Data,errors:Map<String,String>,vm:CaseViewModel) {
 var email by rememberSaveable { mutableStateOf("") };var password by rememberSaveable { mutableStateOf("") };var selected by rememberSaveable { mutableStateOf("") }
 Page {
  CaseLogo();Text("Pruebas en terreno, respaldo al instante",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
  CardBlock("Ingresar") { Field("Correo",email,{email=it},errors["email"]);Field("Contraseña",password,{password=it},errors["password"],password=true);ButtonFull("Entrar") { vm.login(email,password) } }
  CardBlock("Usuarios de demostración") {
   Text("Selecciona un usuario ficticio para probar sus permisos.")
   Choice("Perfil y usuario",selected,d.users.map { it.id to "${Roles.label(it.role)} · ${it.name}" },{selected=it;email=d.users.first { u -> u.id==it }.email;password=Demo.PASSWORD;vm.clearErrors()})
   Text("Contraseña de demostración: ${Demo.PASSWORD}",style=MaterialTheme.typography.bodySmall)
  }
 }
}
@Composable private fun HomeScreen(d:Data,u:User,pending:List<Pending>,busy:Boolean,nav:NavHostController) {
 TechnicalDashboard(d,u,pending,busy) { target ->
  nav.navigate(target) {
   if(target in listOf("home","equipment","visits","more")) { popUpTo(nav.graph.startDestinationId) { saveState=true };restoreState=true }
   launchSingleTop=true
  }
 }
}
@Composable private fun VisitsScreen(d:Data,u:User,nav:NavHostController) {
 var query by rememberSaveable { mutableStateOf("") }
 Page {
  if(u.role in listOf(Roles.TECH,Roles.ENGINEER)) ButtonFull("Iniciar visita") { nav.navigate("visit/new") }
  Field("Buscar OT, cliente o instalación",query,{query=it})
  val filtered=d.visits.filter { v -> "${v.ot} ${d.clients.find { it.id==v.clientId }?.name} ${d.installations.find { it.id==v.installationId }?.name}".contains(query,true) }.sortedByDescending { it.date }
  if(filtered.isEmpty()) Empty("No hay visitas para esta búsqueda.")
  for(v in filtered) Link("${v.ot} · ${d.installations.find { it.id==v.installationId }?.name}","${v.date} · ${if(v.closedAt==null) "Abierta" else "Cerrada · Acta disponible"}") { nav.navigate("visit/${v.id}") }
 }
}
@Composable private fun EquipmentScreen(d:Data,u:User,nav:NavHostController) {
 var query by rememberSaveable { mutableStateOf("") };var classification by rememberSaveable { mutableStateOf("Todos") }
 Page {
  if(Rules.canCreate(u)) ButtonFull("Registrar equipo") { nav.navigate("equipment/new") }
  Field("Buscar TAG, serie o instalación",query,{query=it})
  Choice("Clasificación",classification,listOf("Todos","Primario","Secundario").map { it to it },{classification=it})
  val filtered=d.equipment.filter { e -> (classification=="Todos" || e.classification==classification) && "${e.tag} ${e.serial} ${d.installations.find { it.id==e.installationId }?.name}".contains(query,true) }
  if(filtered.isEmpty()) Empty("No hay equipos. Prueba otra búsqueda o registra uno nuevo.")
  for(e in filtered) Link("${e.tag} · ${Types.label(e.type)}","${d.installations.find { it.id==e.installationId }?.name} · ${e.classification}") { nav.navigate("equipment/${e.id}") }
 }
}
@Composable private fun EquipmentDetail(d:Data,u:User,id:String,nav:NavHostController) {
 val e=d.equipment.find { it.id==id } ?: return
 Page {
  CardBlock(e.tag) { Text("${Types.label(e.type)} · ${e.classification}");Text("${e.brand} ${e.model} · Serie ${e.serial}");Text("Instalación: ${d.installations.find { it.id==e.installationId }?.name}");Text("Fabricación: ${e.year} · Tensión: ${e.highVoltage} kV");if(e.type=="TRANSFORMADOR") Text("Potencia: ${e.power} kVA · Tensiones: ${e.highVoltage}/${e.lowVoltage} kV");if(e.current.isNotBlank()) Text("Corriente: ${e.current} A");if(e.contactMax.isNotBlank()) Text("Máximo de contactos: ${e.contactMax} µΩ");if(e.notes.isNotBlank()) Text(e.notes);if(e.photoPath.isNotBlank()) PhotoPreview(e.photoPath) }
  if(u.role in listOf(Roles.TECH,Roles.ENGINEER)) Notice("Para registrar una prueba, inicia o abre una visita de esta instalación.")
  Text("Historial, del más reciente al más antiguo",fontWeight=FontWeight.Bold)
  val history=d.protocols.filter { it.equipmentId==id }.sortedByDescending { it.testedAt }
  if(history.isEmpty()) Empty("Este equipo aún no tiene protocolos.")
  for(p in history) ProtocolLink(p,u,d,nav)
 }
}
@Composable private fun ProtocolLink(p:Protocol,u:User,d:Data,nav:NavHostController) {
 val replacement=d.protocols.find { it.replacesId==p.id && it.state!=States.VOID }
 Link("${d.equipment.find { it.id==p.equipmentId }?.tag} · ${p.number}","${moment(p.testedAt)} · ${stateLabel(p,u)} · ${p.result.lowercase().replaceFirstChar { it.uppercase() }}"+(replacement?.let { "\nReemplazado por nº ${it.number}" } ?: "")) { nav.navigate("protocol/${p.id}") }
}
@Composable private fun ClientForm(d:Data,vm:CaseViewModel,busy:Boolean,done:()->Unit) {
 var json by rememberSaveable { mutableStateOf(gson.toJson(Client())) };val v=remember(json) { gson.fromJson(json,Client::class.java) };fun change(n:Client) { json=gson.toJson(n) }
 var attempted by rememberSaveable { mutableStateOf(false) };val e=if(attempted) Rules.client(v,d) else emptyMap()
 Page { CardBlock("Cliente") {
  Field("Razón social",v.name,{change(v.copy(name=it))},e["name"]);Field("RUT",v.rut,{change(v.copy(rut=it))},e["rut"])
  Field("Giro",v.giro,{change(v.copy(giro=it))},e["giro"]);Field("Contacto principal",v.contact,{change(v.copy(contact=it))},e["contact"])
  Field("Teléfono",v.phone,{change(v.copy(phone=it))},e["phone"]);Field("Correo",v.email,{change(v.copy(email=it))},e["email"])
 };ButtonFull("Guardar cliente",busy) { attempted=true;vm.client(v,done) } }
}
@Composable private fun InstallationForm(d:Data,vm:CaseViewModel,busy:Boolean,done:()->Unit) {
 var json by rememberSaveable { mutableStateOf(gson.toJson(Installation())) };val v=remember(json) { gson.fromJson(json,Installation::class.java) };fun change(n:Installation) { json=gson.toJson(n) }
 var attempted by rememberSaveable { mutableStateOf(false) };val e=if(attempted) Rules.installation(v,d) else emptyMap()
 Page { CardBlock("Instalación del cliente") {
  Choice("Cliente",v.clientId,d.clients.map { it.id to it.name },{change(v.copy(clientId=it))},e["clientId"])
  Field("Nombre de instalación",v.name,{change(v.copy(name=it))},e["name"]);Field("Ubicación o dirección",v.location,{change(v.copy(location=it))},e["location"])
  Field("Comuna",v.comuna,{change(v.copy(comuna=it))},e["comuna"]);Field("Región",v.region,{change(v.copy(region=it))},e["region"])
  Field("Observaciones de acceso (opcional)",v.access,{change(v.copy(access=it))},e["access"],lines=2)
 };ButtonFull("Guardar instalación",busy) { attempted=true;vm.installation(v,done) } }
}
@Composable private fun EquipmentForm(d:Data,vm:CaseViewModel,busy:Boolean,done:()->Unit) {
 var loadingPhoto by remember { mutableStateOf(false) }
 var json by rememberSaveable { mutableStateOf(gson.toJson(Equipment())) };val v=remember(json) { gson.fromJson(json,Equipment::class.java) };fun change(n:Equipment) { json=gson.toJson(n) }
 var attempted by rememberSaveable { mutableStateOf(false) };val e=if(attempted) Rules.equipment(v,d) else emptyMap()
 Page {
  CardBlock("Identificación") {
   Choice("Instalación",v.installationId,d.installations.map { it.id to "${it.name} · ${d.clients.find { c -> c.id==it.clientId }?.name}" },{change(v.copy(installationId=it))},e["installationId"])
   Field("TAG del equipo",v.tag,{change(v.copy(tag=it))},e["tag"])
   Choice("Tipo de equipo",v.type,Types.all.map { it to Types.label(it) },{change(v.copy(type=it,classification=Types.classification(it)))},e["type"])
   Tag("Clasificación: ${v.classification}")
   Field("Marca",v.brand,{change(v.copy(brand=it))},e["brand"]);Field("Modelo",v.model,{change(v.copy(model=it))},e["model"])
   Field("Número de serie",v.serial,{change(v.copy(serial=it))},e["serial"]);Field("Año de fabricación",v.year,{change(v.copy(year=it))},e["year"],numeric=true)
  }
  CardBlock("Datos de placa") {
   Field("Tensión nominal / primaria (kV)",v.highVoltage,{change(v.copy(highVoltage=it))},e["highVoltage"],numeric=true)
   if(v.type=="TRANSFORMADOR") { Field("Tensión secundaria (kV)",v.lowVoltage,{change(v.copy(lowVoltage=it))},e["lowVoltage"],numeric=true);Field("Potencia (kVA)",v.power,{change(v.copy(power=it))},e["power"],numeric=true) }
   if(v.type in listOf("CELDA","INTERRUPTOR","RELE")) Field("Corriente nominal / ajuste (A)",v.current,{change(v.copy(current=it))},e["current"],numeric=true)
   if(v.type in listOf("CELDA","INTERRUPTOR")) Field("Resistencia máxima de contactos (µΩ)",v.contactMax,{change(v.copy(contactMax=it))},e["contactMax"],numeric=true)
  }
  Field("Observaciones del equipo (opcional)",v.notes,{change(v.copy(notes=it))},e["notes"],lines=2)
  PhotoField(v.photoPath,{change(v.copy(photoPath=it))},vm::notify,{loadingPhoto=it})
  ButtonFull("Guardar equipo",busy || loadingPhoto) { attempted=true;vm.equipment(v,done) }
 }
}
@Composable private fun InstrumentsScreen(d:Data,u:User,nav:NavHostController) {
 Page {
  if(u.role==Roles.CHIEF) ButtonFull("Registrar instrumento") { nav.navigate("instrument/new") }
  for(i in d.instruments) {
   val expires=Rules.iso(i.expires);val days=expires?.let { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(),it) }
   CardBlock(i.name) {
    Text("${i.brand} ${i.model} · Serie ${i.serial}");Text("${Types.testLabel(i.type)}\nCalibrado: ${i.calibrated}\nVence: ${i.expires}")
    if(days!=null && days<0) Tag("Calibración vencida",true) else if(days!=null && days<=30) Tag("Vence en $days días",true) else Tag("Calibración vigente")
    if(u.role==Roles.CHIEF) TextButton(onClick={nav.navigate("instrument/${i.id}")}) { Text("Actualizar calibración") }
   }
  }
 }
}
@Composable private fun InstrumentForm(d:Data,id:String,vm:CaseViewModel,busy:Boolean,done:()->Unit) {
 var json by rememberSaveable(id) { mutableStateOf(gson.toJson(d.instruments.find { it.id==id } ?: Instrument())) };val v=remember(json) { gson.fromJson(json,Instrument::class.java) };fun change(n:Instrument) { json=gson.toJson(n) }
 var attempted by rememberSaveable { mutableStateOf(false) };val e=if(attempted) Rules.instrument(v,d) else emptyMap()
 Page { CardBlock("Instrumento y calibración") {
  Field("Nombre del instrumento",v.name,{change(v.copy(name=it))},e["name"])
  Choice("Prueba para la que se utiliza",v.type,listOf("AISLACION","TTR","DEVANADOS","CONTACTOS","RELE").map { it to Types.testLabel(it) },{change(v.copy(type=it))},e["type"])
  Field("Marca",v.brand,{change(v.copy(brand=it))},e["brand"]);Field("Modelo",v.model,{change(v.copy(model=it))},e["model"]);Field("Número de serie",v.serial,{change(v.copy(serial=it))},e["serial"])
  Field("Fecha de calibración (AAAA-MM-DD)",v.calibrated,{change(v.copy(calibrated=it))},e["calibrated"]);Field("Fecha de vencimiento (AAAA-MM-DD)",v.expires,{change(v.copy(expires=it))},e["expires"])
 };ButtonFull("Guardar instrumento",busy) { attempted=true;vm.instrument(v,done) } }
}
@Composable private fun AccountsScreen(d:Data,u:User,nav:NavHostController) {
 if(u.role!=Roles.CHIEF) return
 Page { ButtonFull("Otorgar cuenta") { nav.navigate("account/new") };for(a in d.users) Link(a.name,"${Roles.label(a.role)} · ${if(a.active) "Activa" else "Desactivada"}") { nav.navigate("account/${a.id}") } }
}
@Composable private fun AccountForm(d:Data,id:String,vm:CaseViewModel,busy:Boolean,done:()->Unit) {
 var json by rememberSaveable(id) { mutableStateOf(gson.toJson(d.users.find { it.id==id } ?: User(role=Roles.CLIENT))) };val v=remember(json) { gson.fromJson(json,User::class.java) };fun change(n:User) { json=gson.toJson(n) }
 var attempted by rememberSaveable { mutableStateOf(false) };val e=if(attempted) Rules.user(v,d) else emptyMap()
 Page { CardBlock("Cuenta de usuario") {
  Field("Nombre completo",v.name,{change(v.copy(name=it))},e["name"]);Field("RUT",v.rut,{change(v.copy(rut=it))},e["rut"]);Field("Correo",v.email,{change(v.copy(email=it))},e["email"])
  Choice("Perfil",v.role,listOf(Roles.TECH,Roles.ENGINEER,Roles.CHIEF,Roles.CLIENT).map { it to Roles.label(it) },{change(v.copy(role=it,clientId=if(it==Roles.CLIENT) v.clientId else ""))},e["role"])
  if(v.role==Roles.CLIENT) { Choice("Empresa cliente",v.clientId,d.clients.map { it.id to it.name },{change(v.copy(clientId=it))},e["clientId"]);Field("Cargo",v.cargo,{change(v.copy(cargo=it))},e["cargo"]) }
  Row(verticalAlignment=Alignment.CenterVertically) { Switch(v.active,{change(v.copy(active=it))});Spacer(Modifier.width(8.dp));Text("Cuenta activa") }
  Text("La cuenta utiliza la contraseña ficticia de demostración.",style=MaterialTheme.typography.bodySmall)
 };ButtonFull("Guardar cuenta",busy) { attempted=true;vm.account(v,done) } }
}
@Composable private fun VisitForm(d:Data,u:User,vm:CaseViewModel,busy:Boolean,done:(String)->Unit) {
 var json by rememberSaveable { mutableStateOf(gson.toJson(Visit(date=LocalDate.now(ZoneId.of("America/Santiago")).toString(),technicianIds=listOf(u.id)))) };val v=remember(json) { gson.fromJson(json,Visit::class.java) };fun change(n:Visit) { json=gson.toJson(n) }
 var attempted by rememberSaveable { mutableStateOf(false) };val e=if(attempted) Rules.visit(v,d) else emptyMap()
 Page { CardBlock("Datos de la visita") {
  Choice("Cliente",v.clientId,d.clients.map { it.id to it.name },{change(v.copy(clientId=it,installationId=""))},e["clientId"])
  Choice("Instalación",v.installationId,d.installations.filter { it.clientId==v.clientId }.map { it.id to it.name },{change(v.copy(installationId=it))},e["installationId"])
  Field("Número de orden de trabajo",v.ot,{change(v.copy(ot=it))},e["ot"]);Field("Fecha (AAAA-MM-DD)",v.date,{change(v.copy(date=it))},e["date"])
  Choice("Tipo de visita",v.kind,listOf("Solo pruebas","Pruebas y mantenimiento").map { it to it },{change(v.copy(kind=it))},e["kind"])
  Choice("Ingeniero a cargo",v.engineerId,d.users.filter { it.active && it.role==Roles.ENGINEER }.map { it.id to it.name },{change(v.copy(engineerId=it))},e["engineerId"])
  Text("Participantes de terreno",fontWeight=FontWeight.SemiBold)
  for(a in d.users.filter { it.active && it.role in listOf(Roles.TECH,Roles.ENGINEER) }) Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()) {
   Checkbox(a.id in v.technicianIds,{checked -> change(v.copy(technicianIds=if(checked) (v.technicianIds+a.id).distinct() else v.technicianIds-a.id))},enabled=a.id!=u.id);Text(a.name)
  };ErrorText(e["technicianIds"])
 }
 LocationField(v.latitude,v.longitude,v.accuracy,{lat,lon,accuracy,at -> val latest=gson.fromJson(json,Visit::class.java);change(latest.copy(latitude=lat,longitude=lon,accuracy=accuracy,locatedAt=at))},vm::notify)
 ButtonFull("Abrir visita",busy) { attempted=true;vm.visit(v,done) } }
}
@Composable private fun MoreScreen(d:Data,u:User,pending:List<Pending>,vm:CaseViewModel,nav:NavHostController,busy:Boolean,errors:Map<String,String>) {
 var a by rememberSaveable { mutableStateOf(vm.repo.equipmentUrl) };var b by rememberSaveable { mutableStateOf(vm.repo.protocolUrl) }
 Page {
  CardBlock("Mi cuenta") { Text(u.name);Text("${Roles.label(u.role)} · RUT ${u.rut}");Text(u.email) }
  Link("Protocolos","Buscar y filtrar el historial") { nav.navigate("protocols") }
  Link("Clientes e instalaciones","Consultar datos de identificación") { nav.navigate("clients") };Link("Instalaciones","Direcciones y condiciones de acceso") { nav.navigate("installations") }
  if(u.role!=Roles.CLIENT) Link("Instrumentos","Calibración y próximos vencimientos") { nav.navigate("instruments") }
  if(u.role==Roles.CHIEF) Link("Usuarios","Otorgar cuentas y activar o desactivar personal") { nav.navigate("accounts") }
  CardBlock("Conexión") {
   Field("Servidor de equipos",a,{a=it},errors["url1"]);Field("Servidor de protocolos",b,{b=it},errors["url2"])
   ButtonFull("Guardar conexión",busy) { vm.connection(a,b) };ButtonFull("Sincronizar ahora",busy,vm::sync)
   if(u.role!=Roles.CLIENT) { Text("${pending.size} cambios pendientes.");pending.firstOrNull { it.error.isNotBlank() }?.let { Notice(it.error,true) } }
  }
  OutlinedButton(onClick=vm::logout,modifier=Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
 }
}
@Composable private fun ProtocolListScreen(d:Data,u:User,nav:NavHostController,onlyPending:Boolean=false) {
 var q by rememberSaveable { mutableStateOf("") };var state by rememberSaveable(onlyPending) { mutableStateOf(if(onlyPending) "PENDIENTES" else "TODOS") };var client by rememberSaveable { mutableStateOf("") };var tech by rememberSaveable { mutableStateOf("") }
 Page {
  Field("Buscar número, TAG o técnico",q,{q=it})
  Choice("Estado",state,listOf("TODOS" to "Todos","PENDIENTES" to "Pendientes de formalización")+States.all.filter { u.role!=Roles.CLIENT || it in listOf(States.REGISTERED,States.FINAL) }.map { it to if(u.role==Roles.CLIENT && it==States.REGISTERED) "Pendiente de formalización" else States.label(it) },{state=it})
  Choice("Cliente",client,listOf("" to "Todos")+d.clients.map { it.id to it.name },{client=it})
  if(u.role!=Roles.CLIENT) Choice("Técnico",tech,listOf("" to "Todos")+d.users.filter { it.role!=Roles.CLIENT }.map { it.id to it.name },{tech=it})
  val filtered=d.protocols.filter { p -> val visit=d.visits.find { it.id==p.visitId };(state=="TODOS" || (state=="PENDIENTES" && p.state in listOf(States.REGISTERED,States.OBSERVED)) || p.state==state || (u.role==Roles.CLIENT && state==States.REGISTERED && p.state==States.OBSERVED)) && (client.isBlank() || d.installations.find { it.id==d.equipment.find { eq -> eq.id==p.equipmentId }?.installationId }?.clientId==client) && (tech.isBlank() || p.ownerId==tech) && "${p.number} ${p.ownerName} ${d.equipment.find { it.id==p.equipmentId }?.tag}".contains(q,true) }.sortedByDescending { it.testedAt }
  if(filtered.isEmpty()) Empty("No hay protocolos para estos filtros.")
  for(p in filtered) ProtocolLink(p,u,d,nav)
 }
}
@Composable private fun VisitDetail(d:Data,u:User,id:String,vm:CaseViewModel,nav:NavHostController,busy:Boolean,errors:Map<String,String>) {
 val v=d.visits.find { it.id==id } ?: return;val ps=d.protocols.filter { it.visitId==id };val context=LocalContext.current
 var equipmentId by rememberSaveable(id) { mutableStateOf("") };var replaces by rememberSaveable(id) { mutableStateOf("") }
 var receiptName by rememberSaveable(id) { mutableStateOf("") };var receiptRut by rememberSaveable(id) { mutableStateOf("") };var receiptCargo by rememberSaveable(id) { mutableStateOf("") };var confirm by remember { mutableStateOf(false) }
 val canWork=u.role in listOf(Roles.TECH,Roles.ENGINEER) && u.id in v.technicianIds
 Page {
  CardBlock(v.ot) {
   Text(d.clients.find { it.id==v.clientId }?.name ?: "");Text(d.installations.find { it.id==v.installationId }?.name ?: "");Text("${v.date} · ${v.kind}")
   if(u.role!=Roles.CLIENT) Text("Ingeniero a cargo: ${d.users.find { it.id==v.engineerId }?.name ?: v.engineerId}")
   Tag(if(v.closedAt==null) "Visita abierta" else "Visita cerrada")
   if(v.latitude!=null) Text("Ubicación: ${fmt(v.latitude)}, ${fmt(v.longitude ?: 0.0)} · Precisión ${v.accuracy?.toInt()} m")
   Text("${ps.size} equipos · ${ps.count { it.result=="OBSERVADO" }} observados · ${ps.count { it.result=="RECHAZADO" }} rechazados")
  }
  if(v.closedAt==null && (canWork || u.role==Roles.CHIEF)) CardBlock("Agregar equipo a la visita") {
   val options=d.equipment.filter { it.installationId==v.installationId && it.id !in ps.map { p -> p.equipmentId } }
   Choice("Equipo",equipmentId,options.map { it.id to "${it.tag} · ${Types.label(it.type)}" },{equipmentId=it;replaces=""})
   val previous=d.protocols.filter { it.equipmentId==equipmentId && it.state==States.FINAL && d.protocols.none { n -> n.replacesId==it.id && n.state!=States.VOID } }
   Choice("Reemplaza un protocolo anterior (opcional)",replaces,listOf("" to "No reemplaza")+previous.map { it.id to it.number },{replaces=it})
   if(options.isEmpty()) Text("Todos los equipos de esta instalación ya tienen protocolo en la visita.")
   else ButtonFull("Iniciar protocolo",busy) { if(equipmentId.isBlank()) vm.notify("Selecciona un equipo.") else vm.start(id,equipmentId,replaces) { nav.navigate("edit/$it") } }
  }
  for(p in ps) ProtocolLink(p,u,d,nav)
  if(v.closedAt==null && canWork) CardBlock("Entregar acta y cerrar visita") {
   Text("Todos los protocolos deben estar registrados en terreno o anulados. El acta es preliminar, pendiente de formalización.")
   Field("Nombre de quien recibe",receiptName,{receiptName=it},errors["receiptName"]);Field("RUT de quien recibe",receiptRut,{receiptRut=it},errors["receiptRut"]);Field("Cargo",receiptCargo,{receiptCargo=it},errors["receiptCargo"])
   ButtonFull("Confirmar recepción y cerrar visita",busy) { confirm=true }
  }
  if(v.closedAt!=null) {
   CardBlock("Constancia de recepción") { Text("${v.receipt?.name}\nRUT: ${v.receipt?.rut}\nCargo: ${v.receipt?.cargo}");Text("Recibido: ${moment(v.receipt?.receivedAt ?: v.closedAt)}") }
   PdfShareButton("Compartir acta de terreno",busy,vm::notify) { CasePdf.acta(context,v,d) }
   Notice("El acta dice «Pendiente de formalización». La aprobación de ingeniería ocurre después.")
  }
 }
 if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Confirmar recepción")},text={Text("Confirma que los datos corresponden a quien recibe el respaldo. Después del cierre podrás compartir el acta con el cliente.")},confirmButton={TextButton(onClick={confirm=false;vm.close(id,Receipt(receiptName,receiptRut,receiptCargo))}) { Text("Cerrar visita") }},dismissButton={TextButton(onClick={confirm=false}) { Text("Revisar") }})
}
@Composable private fun ProtocolEditor(d:Data,u:User,id:String,vm:CaseViewModel,busy:Boolean,done:()->Unit) {
 val draft by vm.draft.collectAsStateWithLifecycle();var attempted by rememberSaveable(id) { mutableStateOf(false) }
 LaunchedEffect(id) { vm.openDraft(id) }
 DisposableEffect(id) { onDispose { vm.flushDraft(id) } }
 val p=draft?.takeIf { it.id==id } ?: return;val equip=d.equipment.find { it.id==p.equipmentId } ?: return;val visit=d.visits.find { it.id==p.visitId }
 if(!Rules.canEdit(u,p)) { Page { Empty("Este protocolo está bloqueado para edición.") };return }
 val errors=if(attempted) Rules.protocol(p,d,true) else emptyMap()
 fun updateTest(index:Int,t:Test) { vm.update(p.copy(tests=p.tests.mapIndexed { i,old -> if(i==index) t else old })) }
 Page {
  Tag("${equip.tag} · ${States.label(p.state)}");Text(p.number,style=MaterialTheme.typography.bodySmall)
  Text("El borrador se guarda automáticamente al dejar de escribir. Las unidades y las pruebas dependen del equipo.")
  CardBlock("Condiciones de la prueba") {
   Field("Temperatura ambiente (°C)",p.temperature,{vm.update(p.copy(temperature=it))},errors["temperature"])
   Field("Humedad relativa (%)",p.humidity,{vm.update(p.copy(humidity=it))},errors["humidity"],numeric=true)
   Text("Técnico responsable: ${p.ownerName}\nFecha y hora: ${moment(p.testedAt)}",style=MaterialTheme.typography.bodySmall)
  }
  ErrorText(errors["tests"])
  for((index,t) in p.tests.withIndex()) CardBlock(Types.testLabel(t.type)) {
   val prefix="${t.type}."
   Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(t.notPerformed,{updateTest(index,t.copy(notPerformed=it))});Text("No realizada") }
   AnimatedVisibility(t.notPerformed) { Field("Motivo obligatorio",t.reason,{updateTest(index,t.copy(reason=it))},errors[prefix+"reason"],lines=2) }
   AnimatedVisibility(!t.notPerformed) {
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
     Choice("Instrumento utilizado",t.instrumentId,d.instruments.filter { it.type==t.type }.map { it.id to "${it.name} · vence ${it.expires}" },{updateTest(index,t.copy(instrumentId=it))},errors[prefix+"instrumentId"])
     ErrorText(errors[prefix+"values"])
     if(t.type=="RELE") Row(verticalAlignment=Alignment.CenterVertically) { Switch(t.relayOperates,{updateTest(index,t.copy(relayOperates=it))});Spacer(Modifier.width(8.dp));Text(if(t.relayOperates) "El relé opera" else "El relé NO opera") }
     if(visit?.kind=="Pruebas y mantenimiento") {
      if(!t.afterMaintenance) OutlinedButton(onClick={updateTest(index,t.copy(previousValues=t.values,values=t.values.map { it.copy(text="") },afterMaintenance=true))}) { Text("Registrar repetición posterior al mantenimiento") }
      else {
       Notice("Estas son las lecturas posteriores. Las previas se conservan y el resultado se calcula con las posteriores.")
       for((pi,old) in t.previousValues.withIndex()) Field("Previa: ${old.label} (${old.unit})",old.text,{ text -> updateTest(index,t.copy(previousValues=t.previousValues.mapIndexed { i,r -> if(i==pi) r.copy(text=text) else r })) },numeric=true)
       ErrorText(errors[prefix+"previousValues"])
      }
     }
     if(t.type=="AISLACION") {
      val unit=t.values.firstOrNull { it.key.endsWith("_1") }?.unit ?: "MΩ"
      Choice("Unidad de las lecturas",unit,listOf("MΩ" to "MΩ","GΩ" to "GΩ"),{next -> updateTest(index,t.copy(values=t.values.map { r -> if(r.key.endsWith("_1") || r.key.endsWith("_10")) { val n=Rules.number(r.text);r.copy(unit=next,text=if(n==null) r.text else fmt(if(r.unit==next) n else if(next=="GΩ") n/1000 else n*1000)) } else r })) })
     }
     for((ri,r) in t.values.withIndex()) {
      if(t.type=="RELE" && !t.relayOperates && r.key in listOf("measured","time")) continue
      Field("${r.label} (${r.unit})",r.text,{text -> updateTest(index,t.copy(values=t.values.mapIndexed { i,old -> if(i==ri) old.copy(text=text) else old }))},errors[prefix+r.key],numeric=true)
     }
     if(t.type=="DEVANADOS") {
      if(Rules.canReview(u)) Choice("Resultado definido por ingeniería",t.manualResult,listOf("" to "Pendiente de revisión")+Rules.results.map { it to it.lowercase().replaceFirstChar { c -> c.uppercase() } },{updateTest(index,t.copy(manualResult=it))})
      else Text("El ingeniero determinará el resultado de devanados durante su revisión.")
     }
     val valid=Rules.test(t,equip,d.instruments,p.testedAt).isEmpty()
     Tag(if(valid) "Resultado: ${Rules.testResult(t,equip)}" else "Completa los datos para calcular el resultado",valid && Rules.testResult(t,equip)=="RECHAZADO")
     if(valid) for(line in Rules.calculated(t,equip)) Text(line,style=MaterialTheme.typography.bodySmall)
     Field("Comentario de la prueba (opcional)",t.comment,{updateTest(index,t.copy(comment=it))},errors[prefix+"comment"],lines=2)
    }
   }
  }
  if(visit?.kind=="Pruebas y mantenimiento") Field("Mantenimiento realizado en este equipo",p.maintenance,{vm.update(p.copy(maintenance=it))},errors["maintenance"],lines=2)
  Field("Observaciones (opcional)",p.notes,{vm.update(p.copy(notes=it))},errors["notes"],lines=2)
  val variations=Rules.variations(p,d);if(variations.any { it.significant }) Notice("Existe una variación mayor al 25 % respecto de la prueba anterior. Ingeniería debe comentarla antes de formalizar.",true)
  ButtonFull("Registrar en terreno",busy) { attempted=true;vm.register(done) }
  OutlinedButton(onClick={vm.saveDraft(done)},enabled=!busy,modifier=Modifier.fillMaxWidth()) { Text("Guardar borrador y salir") }
 }
}
@Composable private fun ProtocolDetail(d:Data,u:User,id:String,vm:CaseViewModel,nav:NavHostController,busy:Boolean) {
 val p=d.protocols.find { it.id==id } ?: return;val equip=d.equipment.find { it.id==p.equipmentId } ?: return;val context=LocalContext.current
 var reasonAttempted by remember { mutableStateOf(false) }
 var target by remember { mutableStateOf("") };var reason by rememberSaveable(id) { mutableStateOf("") };var comment by rememberSaveable(id,p.historicalComment) { mutableStateOf(p.historicalComment) }
 var manual by rememberSaveable(id) { mutableStateOf(p.tests.find { it.type=="DEVANADOS" }?.manualResult ?: "") }
 Page {
  CardBlock("${equip.tag} · Protocolo") {
   Text(p.number);FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) { Tag(stateLabel(p,u));Tag(p.result,p.result=="RECHAZADO") }
   Text("Responsable: ${p.ownerName}\nPrueba: ${moment(p.testedAt)}")
   if(p.formalizedAt!=null) Text("Formalizado por ${p.formalizedBy}\n${moment(p.formalizedAt)}")
   if(p.reason.isNotBlank()) Notice(p.reason,p.state==States.OBSERVED)
   d.protocols.find { it.replacesId==p.id && it.state!=States.VOID }?.let { Notice("Reemplazado por nº ${it.number}. Este original se conserva.") }
   if(p.replacesId.isNotBlank()) Text("Reemplaza a ${d.protocols.find { it.id==p.replacesId }?.number ?: p.replacesId}")
   Text("Temperatura: ${p.temperature} °C · Humedad: ${p.humidity} %")
  }
  for(t in p.tests) CardBlock(Types.testLabel(t.type)) {
   if(t.notPerformed) Text("No realizada: ${t.reason}") else {
    val instrument=d.instruments.find { it.id==t.instrumentId }
    if(u.role!=Roles.CLIENT) Text("Instrumento: ${instrument?.name ?: t.instrumentId} · Serie ${instrument?.serial ?: ""}")
    if(t.afterMaintenance) { Text("Mediciones previas",fontWeight=FontWeight.Bold);for(v in t.previousValues) Text("${v.label}: ${v.text} ${v.unit}");Text("Mediciones posteriores",fontWeight=FontWeight.Bold) }
    for(v in t.values) if(v.text.isNotBlank()) Text("${v.label}: ${v.text} ${v.unit}")
    if(t.type=="RELE" && !t.relayOperates) Notice("El relé no opera.",true)
    Tag("Resultado: ${Rules.testResult(t,equip)}",Rules.testResult(t,equip)=="RECHAZADO")
    for(line in Rules.calculated(t,equip)) Text(line,style=MaterialTheme.typography.bodySmall)
    if(t.comment.isNotBlank()) Text(t.comment)
   }
  }
  CardBlock("Comparación histórica") {
   val comparison=Rules.variations(p,d)
   if(comparison.isEmpty()) Text("No hay mediciones anteriores compatibles.")
   for(c in comparison) {
    Text(c.label,fontWeight=FontWeight.SemiBold);Text("Anterior: ${fmt(c.old)} · Actual: ${fmt(c.current)}")
    Text("Variación: ${c.percent?.let { fmt(it)+" %" } ?: "no calculable: valor anterior cero"}")
    if(c.significant) Tag("Variación significativa > 25 %",true)
   }
   if(p.historicalComment.isNotBlank()) Text("Comentario de ingeniería: ${p.historicalComment}")
  }
  if(p.notes.isNotBlank()) CardBlock("Observaciones") { Text(p.notes) }
  if(p.maintenance.isNotBlank()) CardBlock("Mantenimiento realizado") { Text(p.maintenance) }
  if(Rules.canEdit(u,p)) ButtonFull("Continuar o corregir registro",busy) { vm.openDraft(id);nav.navigate("edit/$id") }
  if(Rules.canReview(u) && p.state==States.REGISTERED) CardBlock("Revisión de ingeniería") {
   if(p.tests.any { !it.notPerformed && it.type=="DEVANADOS" }) Choice("Resultado de devanados",manual,listOf("" to "Seleccionar")+Rules.results.map { it to it },{manual=it})
   Field("Comentario sobre variación histórica",comment,{comment=it},error=if(comment.length>1000) "Máximo 1000 caracteres." else null,lines=2)
   ButtonFull("Guardar revisión",busy) { vm.review(id,comment,manual) }
   ButtonFull("Formalizar protocolo",busy) { target=States.FINAL }
   OutlinedButton(onClick={target=States.OBSERVED},enabled=!busy,modifier=Modifier.fillMaxWidth()) { Text("Devolver con observaciones") }
  }
  if(Rules.canReview(u) && p.state !in listOf(States.FINAL,States.VOID)) TextButton(onClick={target=States.VOID},enabled=!busy) { Text("Anular protocolo") }
  if(p.state==States.FINAL) PdfShareButton("Compartir protocolo formalizado",busy,vm::notify) { CasePdf.protocol(context,p,d) }
  if(u.role!=Roles.CLIENT) CardBlock("Historial de cambios") { for(c in p.changes.asReversed().take(30)) Text("${moment(c.at)} · ${c.userName}\n${c.from} → ${c.to}${if(c.reason.isNotBlank()) " · ${c.reason}" else ""}",style=MaterialTheme.typography.bodySmall) }
 }
 if(target.isNotBlank()) AlertDialog(onDismissRequest={target=""},title={Text(States.label(target))},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
  if(target==States.FINAL) Text("Confirma la revisión. El protocolo quedará bloqueado y se conservará el nombre de quien lo aprobó.") else Field("Motivo obligatorio",reason,{reason=it},error=if(reasonAttempted && (reason.isBlank()||reason.length>500)) "Indica un motivo de hasta 500 caracteres." else null,lines=2)
 }},confirmButton={TextButton(onClick={reasonAttempted=true;if(target==States.FINAL || (reason.isNotBlank() && reason.length<=500)) { vm.transition(id,target,reason);target="";reason="";reasonAttempted=false }}) { Text("Confirmar") }},dismissButton={TextButton(onClick={target=""}) { Text("Cancelar") }})
}
