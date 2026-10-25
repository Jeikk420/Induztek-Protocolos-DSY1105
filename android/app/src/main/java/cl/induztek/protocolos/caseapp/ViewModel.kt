package cl.induztek.protocolos.caseapp

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.induztek.protocolos.ProtocolApplication
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class CaseViewModel(private val app:ProtocolApplication, private val saved:SavedStateHandle):ViewModel() {
 val repo=app.caseRepository
 private val _data=MutableStateFlow(Data());val data=_data.asStateFlow()
 private val _user=MutableStateFlow<User?>(null);val user=_user.asStateFlow()
 private val _busy=MutableStateFlow(false);val busy=_busy.asStateFlow()
 private val _message=MutableStateFlow<String?>(null);val message=_message.asStateFlow()
 private val _draft=MutableStateFlow<Protocol?>(null);val draft=_draft.asStateFlow()
 private val _pending=MutableStateFlow<List<Pending>>(emptyList());val pending=_pending.asStateFlow()
 private val _errors=MutableStateFlow<Map<String,String>>(emptyMap());val errors=_errors.asStateFlow()
 private val _ready=MutableStateFlow(false);val ready=_ready.asStateFlow()
 private var autoSave:Job?=null
 init { viewModelScope.launch {
  repo.initialize();refresh();val restored:String?=saved["session"];if(restored!=null) _user.value=_data.value.users.find { it.id==restored && it.active };_ready.value=true
  repo.db.invalidationTracker.createFlow("case_clients","case_installations","case_equipment","case_users","case_instruments","case_visits","case_protocols","case_pending").collect { refresh() }
 } }
 private suspend fun refresh() {
  _data.value=repo.data();_pending.value=repo.pending()
  val current=_user.value
  if(current!=null) { val actual=_data.value.users.find { it.id==current.id };if(actual==null || !actual.active) logout() else _user.value=actual }
 }
 fun notify(s:String) { _message.value=s }
 fun clearMessage() { _message.value=null }
 fun clearErrors() { _errors.value=emptyMap() }
 fun login(email:String,password:String) {
  val e=linkedMapOf<String,String>()
  if(!Rules.email(email)) e["email"]="Ingresa un correo válido."
  val u=_data.value.users.find { it.email.equals(email.trim(),true) }
  if(password!=Demo.PASSWORD) e["password"]="Contraseña incorrecta."
  if(e.isEmpty() && (u==null || !u.active)) e["email"]="Cuenta inexistente o desactivada."
  _errors.value=e
  if(e.isEmpty() && u!=null) { _user.value=u;saved["session"]=u.id;repo.prefs.edit().putString("last_user",u.id).apply();CaseWorker.schedule(app);notify("Bienvenido, ${u.name.substringBefore(' ')}.") }
 }
 fun logout() {
  autoSave?.cancel();val last=_draft.value;val u=_user.value
  if(last!=null&&u!=null&&Rules.canEdit(u,last)) viewModelScope.launch { try { repo.saveDraft(last,u);refresh();CaseWorker.schedule(app) } catch(_:Exception) {} }
  _draft.value=null;_user.value=null;saved["session"]=null;_errors.value=emptyMap();repo.prefs.edit().remove("last_user").apply() }
 private fun action(block:suspend (User)->Unit) {
  if(_busy.value) return
  val u=_user.value ?: return;_busy.value=true
  viewModelScope.launch {
   try { block(u);refresh();CaseWorker.schedule(app) }
   catch(ex:Exception) { if(ex is CancellationException) throw ex;notify(ex.message ?: "No se pudo completar la operación.") }
   finally { _busy.value=false }
  }
 }
 private fun checked(e:Map<String,String>,block:()->Unit) { _errors.value=e;if(e.isEmpty()) block() else notify("Revisa los campos indicados antes de guardar.") }
 fun client(v:Client,done:()->Unit)=checked(Rules.client(v,_data.value)) { action { repo.saveClient(v,it);notify("Cliente guardado.");done() } }
 fun installation(v:Installation,done:()->Unit)=checked(Rules.installation(v,_data.value)) { action { repo.saveInstallation(v,it);notify("Instalación guardada.");done() } }
 fun equipment(v:Equipment,done:()->Unit)=checked(Rules.equipment(v,_data.value)) { action { repo.saveEquipment(v,it);notify("Equipo guardado.");done() } }
 fun instrument(v:Instrument,done:()->Unit)=checked(Rules.instrument(v,_data.value)) { action { repo.saveInstrument(v,it);notify("Instrumento guardado.");done() } }
 fun account(v:User,done:()->Unit)=checked(Rules.user(v,_data.value)) { action { repo.saveUser(v,it);notify("Cuenta guardada.");done() } }
 fun visit(v:Visit,done:(String)->Unit)=checked(Rules.visit(v,_data.value)) { action { repo.createVisit(v,it);notify("Visita abierta y guardada en el celular.");done(v.id) } }
 fun start(visitId:String,equipmentId:String,replacesId:String,done:(String)->Unit)=action { val p=repo.startProtocol(visitId,equipmentId,it,replacesId);_draft.value=p;_errors.value=emptyMap();done(p.id) }
 fun openDraft(id:String) { val found=_data.value.protocols.find { it.id==id };if(found!=null) _draft.value=found else if(_draft.value?.id!=id) _draft.value=null;_errors.value=emptyMap() }
 fun update(v:Protocol) {
  _draft.value=v;_errors.value=emptyMap();autoSave?.cancel()
  autoSave=viewModelScope.launch {
   delay(900)
   try {
    val u=_user.value ?: return@launch;val current=_draft.value ?: return@launch
    val persisted=repo.saveDraft(current,u)
    if(_draft.value?.id==persisted.id) _draft.value=_draft.value?.copy(revision=persisted.revision,changes=persisted.changes)
    refresh();CaseWorker.schedule(app)
   } catch(ex:Exception) { if(ex is CancellationException) throw ex;notify("No se guardó este cambio: ${ex.message}") }
  }
 }
 fun flushDraft(id:String) {
  val last=_draft.value;val u=_user.value
  if(last==null||last.id!=id||u==null||!Rules.canEdit(u,last))return
  autoSave?.cancel()
  autoSave=viewModelScope.launch { try { val persisted=repo.saveDraft(last,u);if(_draft.value?.id==id) _draft.value=persisted;refresh();CaseWorker.schedule(app) } catch(ex:Exception) { if(ex is CancellationException)throw ex;notify("No se guardó el último cambio: ${ex.message}") } }
 }
 fun saveDraft(done:()->Unit) { autoSave?.cancel();action { val v=_draft.value ?: return@action;_draft.value=repo.saveDraft(v,it);notify("Borrador guardado. Puedes volver a editarlo.");done() } }
 fun register(done:()->Unit) {
  autoSave?.cancel();val p=_draft.value ?: return
  checked(Rules.protocol(p,_data.value,true)) { action { val persisted=repo.saveDraft(p,it);_draft.value=repo.changeState(persisted.id,States.REGISTERED,it);notify("Protocolo registrado en terreno.");done() } }
 }
 fun transition(id:String,target:String,reason:String="")=action { repo.changeState(id,target,it,reason);notify("Estado actualizado: ${States.label(target)}.") }
 fun review(id:String,comment:String,manual:String)=action { repo.review(id,comment,manual,it);notify("Revisión guardada.") }
 fun close(id:String,r:Receipt)=checked(Rules.receipt(r)) { action { repo.closeVisit(id,r,it);notify("Visita cerrada. Comparte el acta preliminar con el cliente.") } }
 fun sync()=action { notify(repo.sync(it.id)) }
 fun connection(a:String,b:String) {
  fun url(s:String)=try { val u=java.net.URI(s);u.scheme in listOf("http","https") && !u.host.isNullOrBlank() && u.userInfo==null && u.query==null && u.fragment==null } catch(_:Exception) { false }
  val e=linkedMapOf<String,String>();if(!url(a)) e["url1"]="URL válida, por ejemplo http://10.0.2.2:8081/";if(!url(b)) e["url2"]="URL válida, por ejemplo http://10.0.2.2:8082/"
  checked(e) { repo.equipmentUrl=a.trim().trimEnd('/')+"/";repo.protocolUrl=b.trim().trimEnd('/')+"/";notify("Conexión guardada.") }
 }
 companion object { val Factory:ViewModelProvider.Factory=viewModelFactory { initializer { CaseViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ProtocolApplication,createSavedStateHandle()) } } }
}
