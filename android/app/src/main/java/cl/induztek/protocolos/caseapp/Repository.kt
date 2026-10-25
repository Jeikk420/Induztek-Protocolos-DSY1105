package cl.induztek.protocolos.caseapp

import android.content.Context
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Credentials
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface CaseApi {
 @GET("api/v2/records") suspend fun records(@Header("Authorization") auth: String): List<RemoteRecord>
 @PUT("api/v2/records/{kind}/{id}") suspend fun save(@Path("kind") kind: String,@Path("id") id: String,@Header("Authorization") auth: String,@Body body: JsonObject): RemoteRecord
}
data class RemoteRecord(val kind: String,val id: String,val revision: Int,val payload: JsonObject)

class CaseRepository(val db: CaseDatabase,context: Context) {
 private val dao=db.dao(); private val gson=Gson(); private val lock=Mutex(); private val syncLock=Mutex()
 val prefs=context.getSharedPreferences("case_settings",Context.MODE_PRIVATE)
 var equipmentUrl: String
  get()=prefs.getString("equipment_url","http://10.0.2.2:8081/")!!
  set(value) { prefs.edit().putString("equipment_url",value).apply() }
 var protocolUrl: String
  get()=prefs.getString("protocol_url","http://10.0.2.2:8082/")!!
  set(value) { prefs.edit().putString("protocol_url",value).apply() }
 suspend fun data()=Data(dao.clients(),dao.installations(),dao.equipment(),dao.users(),dao.instruments(),dao.visits(),dao.protocols())
 suspend fun pending()=dao.pending()
 suspend fun initialize()=lock.withLock { db.withTransaction {
  if(dao.users().isNotEmpty()) return@withTransaction
  val d=Demo.data()
  for(v in d.clients) dao.client(v); for(v in d.installations) dao.installation(v); for(v in d.equipment) dao.equipment(v)
  for(v in d.users) dao.user(v); for(v in d.instruments) dao.instrument(v); for(v in d.visits) dao.visit(v); for(v in d.protocols) dao.protocol(v)
 } }
 private fun valid(errors: Map<String,String>) { require(errors.isEmpty()) { errors.values.first() } }
 private fun staff(user: User) { require(Rules.canCreate(user)) { "El usuario cliente tiene acceso solo de consulta." } }
 private suspend fun actor(user: User): User {
  val actual=dao.users().find { it.id==user.id };require(actual!=null && actual.active && actual.role==user.role && actual.clientId==user.clientId) { "La cuenta no está activa o cambió de permisos. Vuelve a ingresar." };return actual
 }
 private suspend fun queue(kind:String,id:String,revision:Int,v:Any,user:User) {
  dao.pending(Pending("$kind:$id:$revision",kind,id,revision,gson.toJson(v),user.id))
 }
 suspend fun saveClient(v: Client,u: User)=lock.withLock { db.withTransaction {
  staff(actor(u));val d=data();valid(Rules.client(v,d));val next=v.copy(name=v.name.trim(),rut=Rules.cleanRut(v.rut),email=v.email.trim(),revision=(d.clients.find { it.id==v.id }?.revision ?: 0)+1)
  dao.client(next);queue("clients",next.id,next.revision,next,u)
 } }
 suspend fun saveInstallation(v: Installation,u: User)=lock.withLock { db.withTransaction {
  staff(actor(u));val d=data();valid(Rules.installation(v,d));val next=v.copy(name=v.name.trim(),revision=(d.installations.find { it.id==v.id }?.revision ?: 0)+1)
  dao.installation(next);queue("installations",next.id,next.revision,next,u)
 } }
 suspend fun saveEquipment(v: Equipment,u: User)=lock.withLock { db.withTransaction {
  staff(actor(u));val d=data();valid(Rules.equipment(v,d));val next=v.copy(tag=v.tag.trim().uppercase(),revision=(d.equipment.find { it.id==v.id }?.revision ?: 0)+1)
  dao.equipment(next);queue("equipment",next.id,next.revision,next.copy(photoPath=""),u) 
 } }
 suspend fun saveInstrument(v: Instrument,u: User)=lock.withLock { db.withTransaction {
  require(actor(u).role==Roles.CHIEF) { "Solo el jefe técnico administra instrumentos." };val d=data();valid(Rules.instrument(v,d));val next=v.copy(revision=(d.instruments.find { it.id==v.id }?.revision ?: 0)+1)
  dao.instrument(next);queue("instruments",next.id,next.revision,next,u)
 } }
 suspend fun saveUser(v: User,u: User)=lock.withLock { db.withTransaction {
  require(actor(u).role==Roles.CHIEF) { "Solo el jefe técnico otorga cuentas." }; val d=data();valid(Rules.user(v,d));require(v.id!=u.id || (v.active && v.role==Roles.CHIEF)) { "Conserva tu propia cuenta de jefe activa." }
  val next=v.copy(revision=(d.users.find { it.id==v.id }?.revision ?: 0)+1);dao.user(next);queue("users",next.id,next.revision,next,u)
 } }
 suspend fun createVisit(v: Visit,u: User)=lock.withLock { db.withTransaction {
  val a=actor(u);require(a.role in listOf(Roles.TECH,Roles.ENGINEER)) { "Las visitas las inicia el personal de terreno." };valid(Rules.visit(v,data()));require(v.id !in dao.visits().map { it.id })
  val next=v.copy(technicianIds=(v.technicianIds+u.id).distinct());dao.visit(next);queue("visits",next.id,next.revision,next,u)
 } }
 suspend fun startProtocol(visitId:String,equipmentId:String,u:User,replacesId:String=""):Protocol=lock.withLock { db.withTransaction {
  staff(actor(u)); val d=data(); val visit=requireNotNull(d.visits.find { it.id==visitId });require(visit.closedAt==null && (u.id in visit.technicianIds || u.role==Roles.CHIEF)) { "Debes participar en una visita abierta." }
  val equip=requireNotNull(d.equipment.find { it.id==equipmentId });val now=System.currentTimeMillis()
  val tested=if(visit.date==java.time.LocalDate.now(java.time.ZoneId.of("America/Santiago")).toString()) now else java.time.LocalDate.parse(visit.date).atTime(10,0).atZone(java.time.ZoneId.of("America/Santiago")).toInstant().toEpochMilli()
  val p=Protocol(testedAt=tested,visitId=visitId,equipmentId=equipmentId,ownerId=u.id,ownerName=u.name,tests=Rules.newTests(equip.type),replacesId=replacesId,changes=listOf(Change(u.id,u.name,now,"NUEVO",States.DRAFT)))
  valid(Rules.protocol(p,d,false));dao.protocol(p);queue("protocols",p.id,p.revision,p,u);p
 } }
 suspend fun saveDraft(v:Protocol,u:User):Protocol=lock.withLock { db.withTransaction {
  val a=actor(u);val d=data();val old=requireNotNull(d.protocols.find { it.id==v.id });require(Rules.canEdit(a,old)) { "Este protocolo no se puede editar con tu perfil o estado." }
  require(v.equipmentId==old.equipmentId && v.visitId==old.visitId && v.ownerId==old.ownerId && v.testedAt==old.testedAt && v.replacesId==old.replacesId) { "No cambies la identidad del protocolo." }
  require(v.revision==old.revision) { "Hay un cambio nuevo en el registro. Vuelve a abrirlo." }
  if(a.role==Roles.TECH) require(v.tests.map { it.manualResult }==old.tests.map { it.manualResult }) { "El resultado de devanados lo decide el ingeniero." }
  val next=v.copy(state=old.state,number=old.number,ownerName=old.ownerName,result=old.result,formalizedAt=old.formalizedAt,formalizedBy=old.formalizedBy,changes=old.changes+Change(u.id,u.name,System.currentTimeMillis(),old.state,old.state,"Edición del registro"),revision=old.revision+1)
  valid(Rules.protocol(next,d,false));dao.protocol(next);queue("protocols",next.id,next.revision,next,u);next
 } }
 suspend fun changeState(id:String,target:String,u:User,reason:String=""):Protocol=lock.withLock { db.withTransaction {
  val a=actor(u);val d=data();val p=requireNotNull(d.protocols.find { it.id==id });val eq=requireNotNull(d.equipment.find { it.id==p.equipmentId })
  when(target) {
   States.REGISTERED -> { require(Rules.canEdit(a,p)); valid(Rules.protocol(p,d,true)) }
   States.OBSERVED -> { require(Rules.canReview(a) && p.state==States.REGISTERED);require(reason.trim().isNotEmpty() && reason.length<=500) { "Indica el motivo de observación (hasta 500 caracteres)." } }
   States.VOID -> { require(Rules.canReview(a) && p.state !in listOf(States.FINAL,States.VOID));require(reason.trim().isNotEmpty() && reason.length<=500) { "Indica el motivo de anulación (hasta 500 caracteres)." } }
   States.FINAL -> {
    require(Rules.canReview(a) && p.state==States.REGISTERED) { "Solo ingeniero o jefe técnico puede formalizar un registro en terreno." }
    require(d.visits.find { it.id==p.visitId }?.closedAt!=null) { "Primero cierra la visita y entrega el acta preliminar." }
    require(p.tests.none { !it.notPerformed && it.type=="DEVANADOS" && it.manualResult !in Rules.results }) { "El ingeniero debe indicar el resultado de devanados antes de formalizar." }
    require(Rules.variations(p,d).none { it.significant } || p.historicalComment.isNotBlank()) { "Comenta la variación significativa antes de formalizar." }
   }
   else -> error("Transición inválida.")
  }
  val now=System.currentTimeMillis(); val next=p.copy(state=target,result=Rules.global(p,eq),reason=if(target in listOf(States.OBSERVED,States.VOID)) reason.trim() else "",formalizedBy=if(target==States.FINAL) u.name else p.formalizedBy,formalizedAt=if(target==States.FINAL) now else p.formalizedAt,changes=p.changes+Change(u.id,u.name,now,p.state,target,reason.trim()),revision=p.revision+1)
  dao.protocol(next);queue("protocols",next.id,next.revision,next,u);next
 } }
 suspend fun review(id:String,comment:String,manual:String,u:User)=lock.withLock { db.withTransaction {
  require(Rules.canReview(actor(u)));val d=data();val p=requireNotNull(d.protocols.find { it.id==id });require(p.state==States.REGISTERED);require(comment.length<=1000) { "El comentario admite hasta 1000 caracteres." };require(manual.isBlank() || manual in Rules.results) { "Selecciona un resultado de devanados válido." }
  val tests=p.tests.map { if(it.type=="DEVANADOS" && !it.notPerformed) it.copy(manualResult=manual) else it }
  val next=p.copy(historicalComment=comment,tests=tests,revision=p.revision+1,changes=p.changes+Change(u.id,u.name,System.currentTimeMillis(),p.state,p.state,"Revisión de ingeniería"))
  dao.protocol(next);queue("protocols",next.id,next.revision,next,u)
 } }
 suspend fun closeVisit(id:String,receipt:Receipt,u:User)=lock.withLock { db.withTransaction {
  val a=actor(u);require(a.role in listOf(Roles.TECH,Roles.ENGINEER));val d=data();val v=requireNotNull(d.visits.find { it.id==id });require(v.closedAt==null && u.id in v.technicianIds);valid(Rules.receipt(receipt))
  val ps=d.protocols.filter { it.visitId==id };require(ps.isNotEmpty()) { "La visita necesita al menos un protocolo." };require(ps.all { it.state in listOf(States.REGISTERED,States.VOID) }) { "Primero registra en terreno todos los protocolos. Hay borradores u observados pendientes." }
  val now=System.currentTimeMillis();val next=v.copy(closedAt=now,receipt=receipt.copy(receivedAt=now),revision=v.revision+1,actaJson=gson.toJson(ps.filter { it.state!=States.VOID }));dao.visit(next);queue("visits",next.id,next.revision,next,u)
 } }
 private fun api(url:String):CaseApi {
  val client=OkHttpClient.Builder().connectTimeout(8,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).build()
  return Retrofit.Builder().baseUrl(url).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(CaseApi::class.java)
 }
 suspend fun sync(userId:String?):String=syncLock.withLock {
  val catalog=api(equipmentUrl);val work=api(protocolUrl);var sent=0
  for(q in dao.pending()) {
   try {
    val service=if(q.kind in listOf("visits","protocols")) work else catalog
    service.save(q.kind,q.id,Credentials.basic(q.userId,Demo.PASSWORD),gson.fromJson(q.payload,JsonObject::class.java));dao.acknowledged(q.key,q.revision);sent++
   } catch(ex:Exception) {
    if(ex is kotlinx.coroutines.CancellationException) throw ex
    val msg=if(ex is retrofit2.HttpException && ex.code()==409) "Conflicto: el servidor conserva otro cambio. El registro local sigue guardado." else "No se pudo enviar. Revisa conexión o permisos; el registro sigue en el celular."
    dao.syncError(q.key,q.revision,msg);return@withLock msg
   }
  }
  if(userId!=null) {
   val auth=Credentials.basic(userId,Demo.PASSWORD)
   
   for(row in catalog.records(auth)+work.records(auth)) lock.withLock { db.withTransaction { merge(row) } }
  }
  "Sincronización completada: $sent cambios enviados."
 }
 private suspend fun merge(r:RemoteRecord) {
  if(dao.pending().any { it.kind==r.kind && it.id==r.id }) return 
  val d=data()
  when(r.kind) {
   "clients" -> { val v=gson.fromJson(r.payload,Client::class.java);if((d.clients.find { it.id==v.id }?.revision ?: 0)<v.revision) dao.client(v) }
   "installations" -> { val v=gson.fromJson(r.payload,Installation::class.java);if((d.installations.find { it.id==v.id }?.revision ?: 0)<v.revision) dao.installation(v) }
   "equipment" -> { var v=gson.fromJson(r.payload,Equipment::class.java);val old=d.equipment.find { it.id==v.id };if((old?.revision ?: 0)<v.revision) { v=v.copy(photoPath=old?.photoPath ?: "");dao.equipment(v) } }
   "users" -> { val v=gson.fromJson(r.payload,User::class.java);if((d.users.find { it.id==v.id }?.revision ?: 0)<v.revision) dao.user(v) }
   "instruments" -> { val v=gson.fromJson(r.payload,Instrument::class.java);if((d.instruments.find { it.id==v.id }?.revision ?: 0)<v.revision) dao.instrument(v) }
   "visits" -> { val v=gson.fromJson(r.payload,Visit::class.java);if((d.visits.find { it.id==v.id }?.revision ?: 0)<v.revision) dao.visit(v) }
   "protocols" -> { val v=gson.fromJson(r.payload,Protocol::class.java);if((d.protocols.find { it.id==v.id }?.revision ?: 0)<v.revision) dao.protocol(v) }
  }
 }
}
