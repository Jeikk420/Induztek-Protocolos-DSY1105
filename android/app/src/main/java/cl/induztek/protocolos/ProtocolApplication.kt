package cl.induztek.protocolos
import android.app.Application
import androidx.room.Room
import cl.induztek.protocolos.caseapp.*
class ProtocolApplication:Application() {
 val caseDatabase by lazy { Room.databaseBuilder(this,CaseDatabase::class.java,"induztek-case-v2.db").build() }
 val caseRepository by lazy { CaseRepository(caseDatabase,this) }
 override fun onCreate() { super.onCreate();CaseWorker.schedule(this) }
}
