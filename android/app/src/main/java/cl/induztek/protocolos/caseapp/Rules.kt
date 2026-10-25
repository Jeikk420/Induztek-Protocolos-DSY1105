package cl.induztek.protocolos.caseapp

import java.time.*
import kotlin.math.abs

object Rules {
 val results = listOf("APROBADO", "OBSERVADO", "RECHAZADO")
 fun number(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
 fun iso(s: String): LocalDate? = try { LocalDate.parse(s) } catch (_: Exception) { null }
 fun rut(raw: String): Boolean {
  val s = raw.trim().uppercase().replace(".", "").replace("-", "")
  if (!s.matches(Regex("[0-9]{7,8}[0-9K]"))) return false
  val body = s.dropLast(1)
  if (body.toLongOrNull() == 0L || body.startsWith("0")) return false
  var sum = 0; var factor = 2
  for (i in body.indices.reversed()) { sum += body[i].digitToInt() * factor; factor = if(factor == 7) 2 else factor + 1 }
  val rest = 11 - sum % 11
  val dv = when(rest) { 11 -> '0'; 10 -> 'K'; else -> ('0'.code + rest).toChar() }
  return s.last() == dv
 }
 fun email(raw:String):Boolean {
  val s=raw.trim();if(s.length>254||s.count { it=='@' }!=1||s.any { it.isWhitespace() })return false
  val local=s.substringBefore('@');val domain=s.substringAfter('@');val labels=domain.split('.')
  if(local.isBlank()||local.length>64||local.startsWith('.')||local.endsWith('.')||".." in local)return false
  return local.matches(Regex("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+")) && labels.size>=2 && labels.all { it.isNotBlank() && it.length<=63 && it.matches(Regex("[A-Za-z0-9-]+")) && !it.startsWith('-') && !it.endsWith('-') } && labels.last().matches(Regex("[A-Za-z]{2,63}"))
 }

 private fun required(e: MutableMap<String,String>, key: String, s: String, max: Int = 120) {
  if(s.isBlank()) e[key] = "Este dato es obligatorio."
  else if(s.trim().length > max) e[key] = "Máximo $max caracteres."
 }
 private fun numeric(e: MutableMap<String,String>, key: String, s: String, positive: Boolean = false, optional: Boolean = false) {
  if(optional && s.isBlank()) return
  val n = number(s)
  if(n == null) e[key] = "Ingresa un número válido; puedes usar coma o punto."
  else if(n < 0 || (positive && n == 0.0)) e[key] = if(positive) "Debe ser mayor que cero." else "No puede ser negativo."
 }
 fun client(v: Client, data: Data): Map<String,String> {
  val e = linkedMapOf<String,String>()
  required(e,"name",v.name); required(e,"giro",v.giro); required(e,"contact",v.contact)
  if(!rut(v.rut)) e["rut"] = "RUT inválido: revisa el dígito verificador."
  if(data.clients.any { it.id != v.id && cleanRut(it.rut) == cleanRut(v.rut) }) e["rut"] = "Ya existe un cliente con este RUT."
  if(!email(v.email)) e["email"] = "Ingresa un correo con formato nombre@dominio."
  if(v.phone.count { it.isDigit() } !in 8..15 || !v.phone.trim().matches(Regex("\\+?[0-9 ()-]{8,20}"))) e["phone"] = "Ingresa un teléfono válido, con código de país."
  return e
 }
 fun cleanRut(s: String) = s.trim().uppercase().replace(".", "").replace("-", "")
 fun installation(v: Installation, data: Data): Map<String,String> {
  val e = linkedMapOf<String,String>()
  if(data.clients.none { it.id == v.clientId }) e["clientId"] = "Selecciona un cliente registrado."
  required(e,"name",v.name); required(e,"location",v.location,180); required(e,"comuna",v.comuna); required(e,"region",v.region)
  if(v.access.length > 500) e["access"] = "Máximo 500 caracteres."
  if(data.installations.any { it.id != v.id && it.clientId == v.clientId && it.name.trim().equals(v.name.trim(),true) }) e["name"] = "La instalación ya existe para este cliente."
  return e
 }
 fun equipment(v: Equipment, data: Data): Map<String,String> {
  val e = linkedMapOf<String,String>()
  if(data.installations.none { it.id == v.installationId }) e["installationId"] = "Selecciona una instalación."
  required(e,"tag",v.tag,60); required(e,"brand",v.brand); required(e,"model",v.model); required(e,"serial",v.serial)
  if(!v.tag.trim().matches(Regex("[A-Za-z0-9ÁÉÍÓÚÑáéíóúñ/_ .-]{1,60}"))) e["tag"] = "Usa un TAG legible, sin caracteres especiales."
  if(v.type !in Types.all) e["type"] = "Selecciona uno de los cinco tipos de equipo."
  if(v.classification != Types.classification(v.type)) e["type"] = "Clasificación inconsistente con el tipo."
  if(data.equipment.any { it.id != v.id && it.installationId == v.installationId && it.tag.trim().equals(v.tag.trim(),true) }) e["tag"] = "Este TAG ya existe aquí. Vuelve a Equipos y usa el registrado."
  val year = v.year.toIntOrNull()
  if(year == null || year !in 1900..LocalDate.now().year) e["year"] = "Año entre 1900 y ${LocalDate.now().year}."
  numeric(e,"highVoltage",v.highVoltage,positive=true)
  numeric(e,"lowVoltage",v.lowVoltage,positive=true,optional=v.type != "TRANSFORMADOR")
  numeric(e,"power",v.power,positive=true,optional=v.type != "TRANSFORMADOR")
  numeric(e,"current",v.current,positive=true,optional=v.type !in listOf("CELDA","INTERRUPTOR","RELE"))
  numeric(e,"contactMax",v.contactMax,positive=true,optional=v.type !in listOf("CELDA","INTERRUPTOR"))
  if(v.notes.length > 2000) e["notes"] = "Máximo 2000 caracteres."
  return e
 }
 fun instrument(v: Instrument, data: Data): Map<String,String> {
  val e = linkedMapOf<String,String>(); required(e,"name",v.name); required(e,"brand",v.brand); required(e,"model",v.model); required(e,"serial",v.serial)
  if(v.type !in listOf("AISLACION","TTR","DEVANADOS","CONTACTOS","RELE")) e["type"] = "Selecciona el uso del instrumento."
  val a = iso(v.calibrated); val b = iso(v.expires)
  if(a == null || a > LocalDate.now()) e["calibrated"] = "Fecha válida no posterior a hoy (AAAA-MM-DD)."
  if(b == null || (a != null && b < a)) e["expires"] = "Vencimiento válido, no anterior a la calibración."
  if(data.instruments.any { it.id != v.id && it.serial.trim().equals(v.serial.trim(),true) }) e["serial"] = "Ya existe un instrumento con este número de serie."
  return e
 }
 fun user(v: User, data: Data): Map<String,String> {
  val e = linkedMapOf<String,String>(); required(e,"name",v.name)
  if(!rut(v.rut)) e["rut"] = "RUT inválido."
  if(!email(v.email)) e["email"] = "Correo inválido."
  if(v.role !in listOf(Roles.TECH,Roles.ENGINEER,Roles.CHIEF,Roles.CLIENT)) e["role"] = "Perfil inválido."
  if(data.users.any { it.id != v.id && (it.email.trim().equals(v.email.trim(),true) || cleanRut(it.rut) == cleanRut(v.rut)) }) e["email"] = "El correo o RUT ya está registrado."
  if(v.role == Roles.CLIENT) {
   if(data.clients.none { it.id == v.clientId }) e["clientId"] = "Selecciona la empresa del cliente."
   required(e,"cargo",v.cargo)
  }
  return e
 }
 fun visit(v: Visit, data: Data): Map<String,String> {
  val e = linkedMapOf<String,String>()
  if(data.clients.none { it.id==v.clientId }) e["clientId"]="Selecciona un cliente registrado."
  val install = data.installations.find { it.id == v.installationId }
  if(install == null || install.clientId != v.clientId) e["installationId"] = "Selecciona una instalación del cliente."
  required(e,"ot",v.ot,60)
  val day = iso(v.date)
  if(day == null || day > LocalDate.now()) e["date"] = "Fecha válida no futura (AAAA-MM-DD)."
  if(data.visits.any { it.id != v.id && it.ot.trim().equals(v.ot.trim(),true) && it.installationId == v.installationId }) e["ot"] = "Ya existe esta OT en la instalación."
  if(data.users.none { it.id == v.engineerId && it.active && it.role == Roles.ENGINEER }) e["engineerId"] = "Asigna un ingeniero activo, aunque no asista."
  if(v.technicianIds.isEmpty() || v.technicianIds.any { id -> data.users.none { it.id == id && it.active && it.role in listOf(Roles.TECH,Roles.ENGINEER) } }) e["technicianIds"] = "Selecciona al menos un técnico o ingeniero participante."
  if(v.kind !in listOf("Solo pruebas","Pruebas y mantenimiento")) e["kind"] = "Tipo de visita inválido."
  if((v.latitude == null) != (v.longitude == null) || (v.latitude != null && (!v.latitude.isFinite() || abs(v.latitude)>90)) || (v.longitude != null && (!v.longitude.isFinite() || abs(v.longitude)>180))) e["location"] = "Coordenadas inválidas."
  return e
 }
 fun receipt(v: Receipt): Map<String,String> {
  val e=linkedMapOf<String,String>(); required(e,"receiptName",v.name); required(e,"receiptCargo",v.cargo)
  if(!rut(v.rut)) e["receiptRut"]="RUT inválido: revisa el dígito verificador."
  return e
 }
 fun readings(equipmentType: String, test: String): List<Reading> {
  val r = mutableListOf<Reading>()
  when(test) {
   "AISLACION" -> {
    val phases = when(equipmentType) { "TRANSFORMADOR" -> listOf("AT-BT","AT-T","BT-T"); "CABLE" -> listOf("A","B","C"); else -> listOf("Equipo") }
    for(p in phases) {
     r.add(Reading("${p}_1", "$p · Lectura a 1 minuto", "MΩ"))
     if(equipmentType in listOf("TRANSFORMADOR","CABLE")) r.add(Reading("${p}_10", "$p · Lectura a 10 minutos", "MΩ"))
     r.add(Reading("${p}_kv", "$p · Tensión de prueba", "kV"))
    }
   }
   "TTR" -> {
    for(p in listOf("U","V","W")) { r.add(Reading("${p}_theory","Fase $p · Relación teórica","relación")); r.add(Reading("${p}_measured","Fase $p · Relación medida","relación")) }
    r.add(Reading("tap","Posición del cambiador de derivaciones","posición"))
   }
   "DEVANADOS" -> for(w in listOf("AT","BT")) for(p in listOf("U","V","W")) r.add(Reading("${w}_$p","Devanado $w · Fase $p","mΩ"))
   "CONTACTOS" -> for(p in listOf("A","B","C")) r.add(Reading(p,"Polo $p","µΩ"))
   "RELE" -> { r.add(Reading("adjust","Ajuste de corriente","A")); r.add(Reading("measured","Corriente medida de operación","A")); r.add(Reading("expected","Tiempo esperado","ms")); r.add(Reading("time","Tiempo medido","ms")) }
  }
  return r
 }
 fun newTests(type: String) = Types.tests(type).map { Test(it, values=readings(type,it)) }
 fun test(t: Test, equip: Equipment, instruments: List<Instrument>, testedAt: Long): Map<String,String> {
  val e=linkedMapOf<String,String>()
  if(t.notPerformed) { required(e,"reason",t.reason,500); return e }
  val instrument = instruments.find { it.id == t.instrumentId }
  val day = Instant.ofEpochMilli(testedAt).atZone(ZoneId.of("America/Santiago")).toLocalDate()
  if(instrument == null || instrument.type != t.type) e["instrumentId"]="Selecciona un instrumento para esta prueba."
  else {
   val calibrated=iso(instrument.calibrated);val expires=iso(instrument.expires)
   if(calibrated==null || expires==null || day<calibrated || day>expires) e["instrumentId"]="Calibración no vigente para la fecha de prueba: ${instrument.expires}."
  }
  val expected = readings(equip.type,t.type)
  if(t.values.map { it.key } != expected.map { it.key } || t.values.any { val expectedUnit=expected.find { v -> v.key == it.key }?.unit;it.unit!=expectedUnit && !(expectedUnit=="MΩ" && it.unit=="GΩ") }) e["values"]="Las mediciones deben corresponder al tipo y mantener sus unidades."
  for(v in t.values) {
   if(t.type == "RELE" && !t.relayOperates && v.key in listOf("measured","time")) continue
   if(v.unit=="GΩ" && number(v.text)!=null && ohms(v)==null) e[v.key]="Valor demasiado grande para calcular."
   numeric(e,v.key,v.text,positive=v.key.endsWith("_kv") || v.key.endsWith("_theory") || v.key in listOf("adjust","expected"))
   if(v.key=="tap" && (number(v.text)?.rem(1.0) != 0.0 || (number(v.text) ?: -1.0)<1)) e[v.key]="La posición debe ser un entero mayor que cero."
  }
  fun finiteRatio(aKey:String,bKey:String) {
   val a=number(t.values.find { it.key==aKey }?.text ?: "");val b=number(t.values.find { it.key==bKey }?.text ?: "")
   if(a!=null && b!=null && a>0 && !(kotlin.math.abs(b-a)/a*100).isFinite()) e[bKey]="Estos valores son demasiado extremos para calcular."
  }
  if(t.type=="TTR") for(phase in listOf("U","V","W")) finiteRatio("${phase}_theory","${phase}_measured")
  if(t.type=="RELE" && t.relayOperates) finiteRatio("expected","time")
  if(t.afterMaintenance) {
   if(t.previousValues.isEmpty()) e["previousValues"]="Conserva las mediciones previas antes de registrar las posteriores."
   else {
    val previous = test(t.copy(values=t.previousValues,previousValues=emptyList(),afterMaintenance=false),equip,instruments,testedAt)
    if(previous.isNotEmpty()) e["previousValues"]="Revisa las mediciones previas: ${previous.values.first() }"
   }
  }
  if(t.comment.length>1000) e["comment"]="Máximo 1000 caracteres."
  if(t.reason.length>500) e["reason"]="Máximo 500 caracteres."
  return e
 }
 fun testResult(t: Test, equip: Equipment): String {
  if(t.notPerformed) return "NO_REALIZADA"
  fun n(key:String) = number(t.values.find { it.key==key }?.text ?: "") ?: returnNaN()
  val outcomes=mutableListOf<String>()
  when(t.type) {
   "AISLACION" -> {
    for(v in t.values.filter { it.key.endsWith("_1") }) {
     val one=ohms(v) ?: return "OBSERVADO"
     val ten=t.values.find { it.key==v.key.removeSuffix("_1")+"_10" }
     if(one<100) outcomes.add("RECHAZADO")
     else if(ten == null) outcomes.add("APROBADO")
     else { val ip=(ohms(ten) ?: return "OBSERVADO")/one; outcomes.add(if(ip>=2) "APROBADO" else if(ip>=1) "OBSERVADO" else "RECHAZADO") }
    }
   }
   "TTR" -> for(p in listOf("U","V","W")) { val theory=n("${p}_theory"); val error=abs(n("${p}_measured")-theory)/theory*100; outcomes.add(if(!error.isFinite()) "OBSERVADO" else if(error<=0.5+1e-9) "APROBADO" else if(error<=1+1e-9) "OBSERVADO" else "RECHAZADO") }
   "CONTACTOS" -> { val max=number(equip.contactMax) ?: return "OBSERVADO"; for(v in t.values) { val ratio=(number(v.text) ?: return "OBSERVADO")/max; outcomes.add(if(ratio<=1+1e-9) "APROBADO" else if(ratio<=1.2+1e-9) "OBSERVADO" else "RECHAZADO") } }
   "RELE" -> { val error=abs(n("time")-n("expected"))/n("expected")*100; outcomes.add(if(!t.relayOperates) "RECHAZADO" else if(!error.isFinite()) "OBSERVADO" else if(error<=5+1e-9) "APROBADO" else if(error<=10+1e-9) "OBSERVADO" else "RECHAZADO") }
   "DEVANADOS" -> outcomes.add(t.manualResult.takeIf { it in results } ?: "OBSERVADO")
  }
  return worst(outcomes)
 }
 private fun returnNaN() = Double.NaN
 fun worst(values: List<String>): String = when { "RECHAZADO" in values -> "RECHAZADO"; "OBSERVADO" in values -> "OBSERVADO"; "APROBADO" in values -> "APROBADO"; else -> "SIN_RESULTADO" }
 fun global(p: Protocol,e: Equipment) = worst(p.tests.filter { !it.notPerformed }.map { testResult(it,e) })
 fun protocol(p: Protocol, data: Data, complete: Boolean): Map<String,String> {
  val errors=linkedMapOf<String,String>(); val equip=data.equipment.find { it.id==p.equipmentId }; val visit=data.visits.find { it.id==p.visitId }
  if(equip==null) errors["equipmentId"]="Equipo no encontrado."
  if(visit==null || equip?.installationId != visit.installationId) errors["visitId"]="El equipo debe pertenecer a la instalación de la visita."
  if(p.testedAt>System.currentTimeMillis()+60000) errors["testedAt"]="La fecha no puede ser futura."
  if(visit!=null && Instant.ofEpochMilli(p.testedAt).atZone(ZoneId.of("America/Santiago")).toLocalDate().toString()!=visit.date) errors["testedAt"]="La prueba debe registrarse en la fecha de la visita."
  if(data.protocols.any { it.id!=p.id && it.visitId==p.visitId && it.equipmentId==p.equipmentId }) errors["equipmentId"]="Este equipo ya tiene un protocolo en esta visita."
  if(p.replacesId.isNotBlank() && data.protocols.none { it.id==p.replacesId && it.state==States.FINAL && it.equipmentId==p.equipmentId }) errors["replacesId"]="Solo puedes reemplazar un protocolo formalizado del mismo equipo."
  if(p.replacesId.isNotBlank() && data.protocols.any { it.id!=p.id && it.replacesId==p.replacesId && it.state!=States.VOID }) errors["replacesId"]="El original ya tiene un reemplazo."
  if(p.notes.length>2000) errors["notes"]="Máximo 2000 caracteres."
  if(p.maintenance.length>1000) errors["maintenance"]="Máximo 1000 caracteres."
  if(p.historicalComment.length>1000) errors["historicalComment"]="Máximo 1000 caracteres."
  if(!complete || equip==null) return errors
  val temp=number(p.temperature); val hum=number(p.humidity)
  if(temp==null || temp !in -50.0..80.0) errors["temperature"]="Ingresa temperatura entre -50 y 80 °C."
  if(hum==null || hum !in 0.0..100.0) errors["humidity"]="Humedad entre 0 y 100 %."
  if(Types.tests(equip.type).toSet()!=p.tests.map { it.type }.toSet() || p.tests.size!=Types.tests(equip.type).size) errors["tests"]="Cada prueba requerida debe estar realizada o tener motivo de no realización."
  if(p.tests.all { it.notPerformed }) errors["tests"]="Completa al menos una prueba para registrar en terreno."
  for(t in p.tests) for((key,value) in test(t,equip,data.instruments,p.testedAt)) errors["${t.type}.$key"] = value
  if(visit?.kind=="Pruebas y mantenimiento") required(errors,"maintenance",p.maintenance,1000)
  if(visit?.kind!="Pruebas y mantenimiento" && p.tests.any { it.afterMaintenance }) errors["maintenance"]="Las mediciones posteriores requieren una visita con mantenimiento."
  return errors
 }
 fun canEdit(user: User,p: Protocol): Boolean = user.active && user.role in listOf(Roles.TECH,Roles.ENGINEER,Roles.CHIEF) && p.state in listOf(States.DRAFT,States.OBSERVED) && (user.role!=Roles.TECH || p.ownerId==user.id)
 fun canCreate(user: User) = user.active && user.role!=Roles.CLIENT
 fun canReview(user: User) = user.active && user.role in listOf(Roles.ENGINEER,Roles.CHIEF)
 fun visible(user: User,data: Data): Data {
  if(user.role!=Roles.CLIENT) return data
  val installations=data.installations.filter { it.clientId==user.clientId }; val ids=installations.map { it.id }.toSet()
  val equipment=data.equipment.filter { it.installationId in ids }; val eqIds=equipment.map { it.id }.toSet()
  return data.copy(clients=data.clients.filter { it.id==user.clientId },installations=installations,equipment=equipment,users=listOf(user),instruments=emptyList(),visits=data.visits.filter { it.clientId==user.clientId && it.closedAt!=null },protocols=data.protocols.filter { it.equipmentId in eqIds && it.state !in listOf(States.DRAFT,States.VOID) })
 }
 fun previous(p: Protocol,data: Data) = data.protocols.filter { it.id!=p.id && it.equipmentId==p.equipmentId && it.testedAt<p.testedAt && it.state !in listOf(States.DRAFT,States.VOID) }.maxByOrNull { it.testedAt }
 fun variations(p: Protocol,data: Data): List<Variation> {
  val old=previous(p,data) ?: return emptyList(); val out=mutableListOf<Variation>()
  for(t in p.tests.filter { !it.notPerformed }) {
   val before=old.tests.find { it.type==t.type && !it.notPerformed } ?: continue
   for(v in t.values) {
    if(v.key.endsWith("_kv") || v.key.endsWith("_theory") || v.key in listOf("tap","adjust","expected")) continue
    val beforeValue=before.values.find { it.key==v.key } ?: continue
    if(v.unit!=beforeValue.unit && !(v.unit in listOf("MΩ","GΩ") && beforeValue.unit in listOf("MΩ","GΩ"))) continue
    val a=ohms(beforeValue) ?: continue;val b=ohms(v) ?: continue
    out.add(Variation("${Types.testLabel(t.type)} · ${v.label} (${if(v.unit=="GΩ") "MΩ" else v.unit})",a,b,if(a==0.0) null else (b-a)/abs(a)*100))
   }
  }
  return out
 }
 fun late(p: Protocol,today: LocalDate=LocalDate.now()): Boolean {
  if(p.state in listOf(States.FINAL,States.VOID)) return false
  var due=Instant.ofEpochMilli(p.testedAt).atZone(ZoneId.of("America/Santiago")).toLocalDate(); var days=0
  while(days<2) { due=due.plusDays(1); if(due.dayOfWeek !in listOf(DayOfWeek.SATURDAY,DayOfWeek.SUNDAY)) days++ }
  return today>due 
 }
 fun ohms(r:Reading):Double? { val n=number(r.text) ?: return null;return (if(r.unit=="GΩ") n*1000 else n).takeIf { it.isFinite() } }
 fun calculated(t:Test,e:Equipment):List<String> {
  if(t.notPerformed) return emptyList()
  val out=mutableListOf<String>()
  when(t.type) {
   "AISLACION" -> for(one in t.values.filter { it.key.endsWith("_1") }) {
    val ten=t.values.find { it.key==one.key.removeSuffix("_1")+"_10" } ?: continue
    val a=ohms(one);val b=ohms(ten)
    if(a!=null && b!=null) out.add("${one.key.removeSuffix("_1")} · IP = ${if(a==0.0) "no calculable (lectura 1 min = 0)" else "%.3f".format(java.util.Locale.US,b/a)}")
   }
   "TTR" -> for(phase in listOf("U","V","W")) {
    val a=number(t.values.find { it.key=="${phase}_theory" }?.text ?: "");val b=number(t.values.find { it.key=="${phase}_measured" }?.text ?: "")
    if(a!=null && b!=null && a>0) out.add("Fase $phase · Error TTR = %.3f %%".format(java.util.Locale.US,abs(b-a)/a*100))
   }
   "CONTACTOS" -> { val max=number(e.contactMax);if(max!=null && max>0) for(v in t.values) number(v.text)?.let { out.add("${v.label} · %.2f %% del máximo de placa".format(java.util.Locale.US,it/max*100)) } }
   "RELE" -> { val a=number(t.values.find { it.key=="expected" }?.text ?: "");val b=number(t.values.find { it.key=="time" }?.text ?: "");if(a!=null && b!=null && a>0 && t.relayOperates) out.add("Diferencia de tiempo = %.2f %%".format(java.util.Locale.US,abs(b-a)/a*100)) }
  }
  return out
 }

}
