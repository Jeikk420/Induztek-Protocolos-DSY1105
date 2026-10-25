package cl.induztek.protocolos

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.induztek.protocolos.caseapp.VisitLocationCapture
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class NativeGpsTest {
 private val context=ApplicationProvider.getApplicationContext<Context>()
 private val manager=context.getSystemService(LocationManager::class.java)
 private val main=Handler(Looper.getMainLooper())
 private fun shell(command:String):String {
  InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use { fd ->
   return android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).use { String(it.readBytes()) }
  }
 }
 private fun mock() {
  shell("settings put secure location_providers_allowed +gps")
  shell("settings put secure location_mode 3")
  val deadline=SystemClock.elapsedRealtime()+5000
  while(Settings.Secure.getInt(context.contentResolver,Settings.Secure.LOCATION_MODE,0)==0 && SystemClock.elapsedRealtime()<deadline) SystemClock.sleep(100)
  assertTrue("Preparación GPS: proveedores="+shell("settings get secure location_providers_allowed"),Settings.Secure.getInt(context.contentResolver,Settings.Secure.LOCATION_MODE,0)!=0)
  shell("appops set cl.induztek.protocolos android:mock_location allow")
  try { manager.removeTestProvider(LocationManager.GPS_PROVIDER) } catch(_:IllegalArgumentException) {}
  manager.addTestProvider(LocationManager.GPS_PROVIDER,false,false,false,false,true,true,true,Criteria.POWER_LOW,Criteria.ACCURACY_FINE)
  manager.setTestProviderEnabled(LocationManager.GPS_PROVIDER,true)
 }
 private fun removeMock() { try { manager.removeTestProvider(LocationManager.GPS_PROVIDER) } catch(_:IllegalArgumentException) {} }
 private fun point()=Location(LocationManager.GPS_PROVIDER).apply {
  latitude=-33.45;longitude=-70.66;accuracy=3f;time=System.currentTimeMillis();elapsedRealtimeNanos=SystemClock.elapsedRealtimeNanos()
 }

 @Test fun gpsNativoEntregaUnResultadoYPermiteReintentar() {
  mock();val executor=Executors.newSingleThreadScheduledExecutor()
  try {
   executor.scheduleAtFixedRate({ manager.setTestProviderLocation(LocationManager.GPS_PROVIDER,point()) },0,1,TimeUnit.SECONDS)
   repeat(2) {
    val done=CountDownLatch(1);val count=AtomicInteger();var found:Location?=null;var failure=""
    val request=VisitLocationCapture(context,{ found=it;count.incrementAndGet();done.countDown() },{ failure=it;done.countDown() },40000)
    main.post { request.start();request.start() }
    assertTrue("GPS no respondió: $failure",done.await(60,TimeUnit.SECONDS))
    assertEquals("",failure);assertNotNull(found);assertEquals(-33.45,found!!.latitude,0.000001);assertEquals(-70.66,found!!.longitude,0.000001);assertEquals(3f,found!!.accuracy,0f)
    SystemClock.sleep(500);assertEquals("Solo debe responder una vez por solicitud",1,count.get())
   }
  } finally { executor.shutdownNow();removeMock() }
 }
 @Test fun cancelarNoEntregaUbicacionTardia() {
  mock()
  try {
   val count=AtomicInteger();val cancelled=CountDownLatch(1)
   val request=VisitLocationCapture(context,{ count.incrementAndGet() },{ count.incrementAndGet() })
   main.post { request.start();request.cancel();cancelled.countDown() }
   assertTrue(cancelled.await(30,TimeUnit.SECONDS));manager.setTestProviderLocation(LocationManager.GPS_PROVIDER,point());SystemClock.sleep(1500)
   assertEquals(0,count.get())
  } finally { removeMock() }
 }
 @Test fun permisoDenegadoInformaYSinCoordenadas() {
  val denied=object:ContextWrapper(context) {
   override fun checkPermission(permission:String,pid:Int,uid:Int):Int = if(permission==Manifest.permission.ACCESS_FINE_LOCATION || permission==Manifest.permission.ACCESS_COARSE_LOCATION) PackageManager.PERMISSION_DENIED else super.checkPermission(permission,pid,uid)
  }
  val done=CountDownLatch(1);var error="";var coordinates=false
  main.post { VisitLocationCapture(denied,{coordinates=true;done.countDown()},{error=it;done.countDown()}).start() }
  assertTrue(done.await(30,TimeUnit.SECONDS));assertFalse(coordinates);assertTrue(error.contains("Permiso denegado"))
 }
 @Test fun ubicacionDesactivadaInformaSinBloquear() {
  shell("settings put secure location_providers_allowed -gps")
  shell("settings put secure location_providers_allowed -network")
  shell("settings put secure location_mode 0")
  try {
   
   val deadline=SystemClock.elapsedRealtime()+5000
   while(Settings.Secure.getInt(context.contentResolver,Settings.Secure.LOCATION_MODE,-1)!=0 && SystemClock.elapsedRealtime()<deadline) SystemClock.sleep(100)
   assertEquals("No se logró desactivar la ubicación para preparar la prueba",0,Settings.Secure.getInt(context.contentResolver,Settings.Secure.LOCATION_MODE,-1))
   val done=CountDownLatch(1);var error="";var coordinates=false
   main.post { VisitLocationCapture(context,{coordinates=true;done.countDown()},{error=it;done.countDown()}).start() }
   assertTrue(done.await(30,TimeUnit.SECONDS));assertFalse(coordinates);assertTrue(error.contains("desactivada"))
  } finally { shell("settings put secure location_providers_allowed +gps");shell("settings put secure location_mode 3") }
 }
 @Test fun rechazaCoordenadasViejasEInvalidas() {
  val fresh=point();assertTrue(VisitLocationCapture.usable(fresh))
  val old=point().apply { elapsedRealtimeNanos=SystemClock.elapsedRealtimeNanos()-60000000000L };assertFalse(VisitLocationCapture.usable(old))
  val invalid=point().apply { latitude=Double.NaN };assertFalse(VisitLocationCapture.usable(invalid))
  val far=point().apply { longitude=181.0 };assertFalse(VisitLocationCapture.usable(far))
  val badAccuracy=point().apply { accuracy=Float.NaN };assertFalse(VisitLocationCapture.usable(badAccuracy))
 }
 @Test fun tiempoDeEsperaCancelaYPermiteContinuar() {
  mock()
  try {
   val done=CountDownLatch(1);var error="";var coordinates=false;val count=AtomicInteger()
   
   main.post { VisitLocationCapture(context,{coordinates=true;count.incrementAndGet();done.countDown()},{error=it;count.incrementAndGet();done.countDown()},0).start() }
   assertTrue(done.await(30,TimeUnit.SECONDS));assertFalse(coordinates);assertTrue(error.contains("espera agotado"))
   manager.setTestProviderLocation(LocationManager.GPS_PROVIDER,point());SystemClock.sleep(500);assertEquals(1,count.get())
  } finally {removeMock()}
 }
}
