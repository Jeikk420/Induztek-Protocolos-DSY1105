package cl.induztek.protocolos.caseapp

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()
object Roles {
 const val TECH = "TECNICO"; const val ENGINEER = "INGENIERO"; const val CHIEF = "JEFE"; const val CLIENT = "CLIENTE"
 fun label(role: String) = when(role) { TECH -> "Técnico"; ENGINEER -> "Ingeniero"; CHIEF -> "Jefe técnico"; else -> "Cliente" }
}
object States {
 const val DRAFT = "BORRADOR"; const val REGISTERED = "REGISTRADO"; const val OBSERVED = "OBSERVADO"; const val FINAL = "FORMALIZADO"; const val VOID = "ANULADO"
 val all = listOf(DRAFT, REGISTERED, OBSERVED, FINAL, VOID)
 fun label(s: String) = when(s) { DRAFT -> "Borrador"; REGISTERED -> "Registrado en terreno"; OBSERVED -> "Observado"; FINAL -> "Formalizado"; else -> "Anulado" }
}
object Types {
 val all = listOf("TRANSFORMADOR", "INTERRUPTOR", "CELDA", "CABLE", "RELE")
 fun label(t: String) = when(t) { "TRANSFORMADOR" -> "Transformador"; "INTERRUPTOR" -> "Interruptor"; "CELDA" -> "Celda MT"; "CABLE" -> "Cable de poder"; else -> "Relé de protección" }
 fun classification(t: String) = if(t == "RELE") "Secundario" else "Primario"
 fun tests(t: String) = when(t) {
  "TRANSFORMADOR" -> listOf("AISLACION", "TTR", "DEVANADOS")
  "INTERRUPTOR", "CELDA" -> listOf("AISLACION", "CONTACTOS")
  "CABLE" -> listOf("AISLACION")
  else -> listOf("RELE")
 }
 fun testLabel(t: String) = when(t) { "AISLACION" -> "Resistencia de aislación"; "TTR" -> "Relación de transformación"; "DEVANADOS" -> "Resistencia de devanados"; "CONTACTOS" -> "Resistencia de contactos"; else -> "Operación de relé" }
}
@Entity(tableName="case_clients")
data class Client(@PrimaryKey val id: String = newId(), val name: String = "", val rut: String = "", val giro: String = "", val contact: String = "", val phone: String = "", val email: String = "", val revision: Int = 1)
@Entity(tableName="case_installations")
data class Installation(@PrimaryKey val id: String = newId(), val clientId: String = "", val name: String = "", val location: String = "", val comuna: String = "", val region: String = "Metropolitana", val access: String = "", val revision: Int = 1)
@Entity(tableName="case_equipment")
data class Equipment(@PrimaryKey val id: String = newId(), val installationId: String = "", val tag: String = "", val type: String = "TRANSFORMADOR", val brand: String = "", val model: String = "", val serial: String = "", val year: String = "", val power: String = "", val highVoltage: String = "", val lowVoltage: String = "", val current: String = "", val contactMax: String = "", val notes: String = "", val photoPath: String = "", val classification: String = Types.classification(type), val revision: Int = 1)
@Entity(tableName="case_users")
data class User(@PrimaryKey val id: String = newId(), val name: String = "", val rut: String = "", val role: String = Roles.TECH, val email: String = "", val clientId: String = "", val cargo: String = "", val active: Boolean = true, val revision: Int = 1)
@Entity(tableName="case_instruments")
data class Instrument(@PrimaryKey val id: String = newId(), val name: String = "", val type: String = "AISLACION", val brand: String = "", val model: String = "", val serial: String = "", val calibrated: String = "", val expires: String = "", val revision: Int = 1)
data class Receipt(val name: String = "", val rut: String = "", val cargo: String = "", val receivedAt: Long = 0)
@Entity(tableName="case_visits")
data class Visit(@PrimaryKey val id: String = newId(), val clientId: String = "", val installationId: String = "", val date: String = "", val ot: String = "", val engineerId: String = "", val technicianIds: List<String> = emptyList(), val kind: String = "Solo pruebas", val closedAt: Long? = null, val receipt: Receipt? = null, val latitude: Double? = null, val longitude: Double? = null, val accuracy: Float? = null, val locatedAt: Long? = null, val revision: Int = 1, val actaJson: String = "")
data class Reading(val key: String, val label: String, val unit: String, val text: String = "")
data class Test(val type: String, val instrumentId: String = "", val values: List<Reading> = emptyList(), val previousValues: List<Reading> = emptyList(), val afterMaintenance: Boolean = false, val notPerformed: Boolean = false, val reason: String = "", val manualResult: String = "", val relayOperates: Boolean = true, val comment: String = "")
data class Change(val userId: String, val userName: String, val at: Long, val from: String, val to: String, val reason: String = "")
@Entity(tableName="case_protocols")
data class Protocol(@PrimaryKey val id: String = newId(), val number: String = "PT-" + id.uppercase(), val visitId: String = "", val equipmentId: String = "", val ownerId: String = "", val ownerName: String = "", val testedAt: Long = System.currentTimeMillis(), val temperature: String = "", val humidity: String = "", val state: String = States.DRAFT, val result: String = "OBSERVADO", val tests: List<Test> = emptyList(), val notes: String = "", val maintenance: String = "", val historicalComment: String = "", val reason: String = "", val formalizedBy: String = "", val formalizedAt: Long? = null, val replacesId: String = "", val changes: List<Change> = emptyList(), val revision: Int = 1)
data class Data(val clients: List<Client> = emptyList(), val installations: List<Installation> = emptyList(), val equipment: List<Equipment> = emptyList(), val users: List<User> = emptyList(), val instruments: List<Instrument> = emptyList(), val visits: List<Visit> = emptyList(), val protocols: List<Protocol> = emptyList())
data class Variation(val label: String, val old: Double, val current: Double, val percent: Double?) { val significant: Boolean get() = percent != null && kotlin.math.abs(percent) > 25.0 }
