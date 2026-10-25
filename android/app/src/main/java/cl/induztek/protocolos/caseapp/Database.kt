package cl.induztek.protocolos.caseapp

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName="case_pending")
data class Pending(@PrimaryKey val key: String, val kind: String, val id: String, val revision: Int, val payload: String, val userId: String, val error: String = "")
class CaseConverters {
 private val gson = Gson()
 @TypeConverter fun testsJson(v: List<Test>): String = gson.toJson(v)
 @TypeConverter fun tests(v: String): List<Test> = gson.fromJson(v, object: TypeToken<List<Test>>() {}.type)
 @TypeConverter fun changesJson(v: List<Change>): String = gson.toJson(v)
 @TypeConverter fun changes(v: String): List<Change> = gson.fromJson(v, object: TypeToken<List<Change>>() {}.type)
 @TypeConverter fun idsJson(v: List<String>): String = gson.toJson(v)
 @TypeConverter fun ids(v: String): List<String> = gson.fromJson(v, object: TypeToken<List<String>>() {}.type)
 @TypeConverter fun receiptJson(v: Receipt?): String? = v?.let { gson.toJson(it) }
 @TypeConverter fun receipt(v: String?): Receipt? = v?.let { gson.fromJson(it, Receipt::class.java) }
}
@Dao interface CaseDao {
 @Query("SELECT * FROM case_clients ORDER BY name") suspend fun clients(): List<Client>
 @Query("SELECT * FROM case_installations ORDER BY name") suspend fun installations(): List<Installation>
 @Query("SELECT * FROM case_equipment ORDER BY tag") suspend fun equipment(): List<Equipment>
 @Query("SELECT * FROM case_users ORDER BY name") suspend fun users(): List<User>
 @Query("SELECT * FROM case_instruments ORDER BY expires") suspend fun instruments(): List<Instrument>
 @Query("SELECT * FROM case_visits ORDER BY date DESC") suspend fun visits(): List<Visit>
 @Query("SELECT * FROM case_protocols ORDER BY testedAt DESC, id") suspend fun protocols(): List<Protocol>
 @Query("SELECT * FROM case_pending ORDER BY rowid") suspend fun pending(): List<Pending>
 @Upsert suspend fun client(v: Client)
 @Upsert suspend fun installation(v: Installation)
 @Upsert suspend fun equipment(v: Equipment)
 @Upsert suspend fun user(v: User)
 @Upsert suspend fun instrument(v: Instrument)
 @Upsert suspend fun visit(v: Visit)
 @Upsert suspend fun protocol(v: Protocol)
 @Upsert suspend fun pending(v: Pending)
 @Query("DELETE FROM case_pending WHERE `key`=:key AND revision=:revision") suspend fun acknowledged(key: String, revision: Int)
 @Query("UPDATE case_pending SET error=:error WHERE `key`=:key AND revision=:revision") suspend fun syncError(key: String, revision: Int, error: String)
}
@Database(entities=[Client::class, Installation::class, Equipment::class, User::class, Instrument::class, Visit::class, Protocol::class, Pending::class], version=1, exportSchema=true)
@TypeConverters(CaseConverters::class)
abstract class CaseDatabase: RoomDatabase() { abstract fun dao(): CaseDao }
