package cl.induztek.protocolos
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.induztek.protocolos.caseapp.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RoomPersistenceTest {
 @Test fun visitaProtocoloActaYColaPersistenAlReabrir()=runTest {
  val ctx=ApplicationProvider.getApplicationContext<Context>();val name="case-test-${System.nanoTime()}.db"
  fun open()=Room.databaseBuilder(ctx,CaseDatabase::class.java,name).build()
  val first=open();val repo=CaseRepository(first,ctx);repo.initialize();val user=Demo.users.first{it.id=="t1"}
  val date=LocalDate.now(java.time.ZoneId.of("America/Santiago"));repo.saveInstrument(Demo.data().instruments.first{it.id=="i1"}.copy(calibrated=date.toString(),expires=date.plusYears(1).toString()),Demo.users.first{it.id=="j1"})
  val visit=Visit(clientId="cl6",installationId="in6",date=date.toString(),ot="OT-TEST",engineerId="e1",technicianIds=listOf("t1"))
  repo.createVisit(visit,user);val p=repo.startProtocol(visit.id,"eq7",user)
  val filled=p.copy(temperature="20",humidity="50",tests=p.tests.map{t->t.copy(instrumentId="i1",values=t.values.map{it.copy(text=if(it.key.endsWith("_1"))"600" else if(it.key.endsWith("_10"))"1200" else "5")})})
  repo.saveDraft(filled,user);repo.changeState(p.id,States.REGISTERED,user);repo.closeVisit(visit.id,Receipt("Marcela Fuentes","14.226.791-8","Jefa"),user);first.close()
  val second=open()
  try{val d=CaseRepository(second,ctx).data();assertEquals(States.REGISTERED,d.protocols.first{it.id==p.id}.state);val v=d.visits.first{it.id==visit.id};assertNotNull(v.receipt);assertNotNull(v.closedAt);assertTrue(v.actaJson.contains(p.id));assertTrue(second.dao().pending().size>=5)}finally{second.close();ctx.deleteDatabase(name)}
 }
 @Test fun clienteNoPuedeCrearDatos()=runTest {
  val ctx=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(ctx,CaseDatabase::class.java).build()
  try{val repo=CaseRepository(db,ctx);repo.initialize();try{repo.saveClient(Demo.data().clients.first(),Demo.users.first{it.id=="c1"});fail("Debió rechazar al cliente")}catch(_:IllegalArgumentException){}}finally{db.close()}
 }
 @Test fun confirmacionAntiguaConservaCambioNuevo()=runTest {
  val ctx=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(ctx,CaseDatabase::class.java).build()
  try{db.dao().pending(Pending("protocols:p:1","protocols","p",1,"{}","t1"));db.dao().pending(Pending("protocols:p:2","protocols","p",2,"{}","t1"));db.dao().acknowledged("protocols:p:1",1);assertEquals(2,db.dao().pending().single().revision)}finally{db.close()}
 }
 @Test fun coordenadasYFormularioPersistenAlReabrir()=runTest {
  val ctx=ApplicationProvider.getApplicationContext<Context>();val name="geo-test-${System.nanoTime()}.db"
  fun open()=Room.databaseBuilder(ctx,CaseDatabase::class.java,name).build()
  val first=open();val repo=CaseRepository(first,ctx);repo.initialize()
  val visit=Visit(clientId="cl6",installationId="in6",date=LocalDate.now(java.time.ZoneId.of("America/Santiago")).toString(),ot="OT-GPS-PERSISTE",engineerId="e1",technicianIds=listOf("t1"),latitude=-33.45,longitude=-70.66,accuracy=3f,locatedAt=System.currentTimeMillis())
  repo.createVisit(visit,Demo.users.first{it.id=="t1"});first.close()
  val second=open()
  try {
   val restored=CaseRepository(second,ctx).data().visits.first{it.id==visit.id}
   assertEquals(visit.ot,restored.ot);assertEquals(visit.clientId,restored.clientId);assertEquals(visit.engineerId,restored.engineerId)
   assertEquals(-33.45,restored.latitude!!,0.0);assertEquals(-70.66,restored.longitude!!,0.0);assertEquals(3f,restored.accuracy!!,0f);assertEquals(visit.locatedAt,restored.locatedAt)
  } finally {second.close();ctx.deleteDatabase(name)}
 }

}
