package cl.induztek.protocolos.caseapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

class VisitLocationCapture(
 private val context:Context,
 private val onLocation:(Location)->Unit,
 private val onError:(String)->Unit,
 private val timeoutMs:Long=20000
) {
 private val handler=Handler(Looper.getMainLooper())
 private val signals=mutableListOf<CancellationSignal>()
 private var finished=false
 private var started=false
 private var remaining=0
 private val timeout=Runnable { fail("Tiempo de espera agotado. Puedes reintentar o continuar sin GPS.") }

 fun start() {
  check(Looper.myLooper()==Looper.getMainLooper()) { "La búsqueda debe comenzar en el hilo de la interfaz." }
  if(started || finished) return
  started=true
  val fine=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
  val coarse=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
  if(!fine && !coarse) { fail("Permiso denegado. Puedes guardar la visita sin coordenadas.");return }
  val manager=context.getSystemService(LocationManager::class.java)
  try {
   if(!enabled(manager)) { fail("La ubicación está desactivada. Actívala en los ajustes del dispositivo.");return }
   val available=manager.getProviders(true)
   val providers=mutableListOf<String>()
   
   if((fine || Build.VERSION.SDK_INT>=31) && LocationManager.GPS_PROVIDER in available) providers.add(LocationManager.GPS_PROVIDER)
   if("fused" in available) providers.add("fused")
   if(LocationManager.NETWORK_PROVIDER in available) providers.add(LocationManager.NETWORK_PROVIDER)
   if(providers.isEmpty()) { fail(if(fine) "No hay un proveedor de ubicación disponible. Puedes continuar sin GPS." else "Con permiso aproximado no hay una fuente disponible. Concede ubicación precisa o continúa sin GPS.");return }
   remaining=providers.size
   handler.postDelayed(timeout,timeoutMs)
   for(provider in providers) {
    if(finished) break
    val signal=CancellationSignal();signals.add(signal)
    try {
     LocationManagerCompat.getCurrentLocation(manager,provider,signal,ContextCompat.getMainExecutor(context)) { location ->
      if(!finished) {
       if(!enabled(manager)) fail("La ubicación está desactivada. Actívala en los ajustes del dispositivo.")
       else if(location!=null && usable(location)) {
        finished=true;cleanup();onLocation(location)
       } else unavailable()
      }
     }
    } catch(_:SecurityException) { unavailable() }
    catch(_:IllegalArgumentException) { unavailable() }
   }
  } catch(_:SecurityException) { fail("El permiso de ubicación fue retirado. Puedes continuar sin GPS.") }
  catch(_:Exception) { fail("No se pudo obtener ubicación. Reintenta o continúa sin GPS.") }
 }

 private fun unavailable() {
  remaining--
  if(remaining<=0) fail("No se obtuvo una ubicación reciente. Reintenta al aire libre o configura Location en el emulador.")
 }
 @Suppress("DEPRECATION")
 private fun enabled(manager:LocationManager):Boolean {
  
  if(Build.VERSION.SDK_INT<28) return Settings.Secure.getInt(context.contentResolver,Settings.Secure.LOCATION_MODE,Settings.Secure.LOCATION_MODE_OFF)!=Settings.Secure.LOCATION_MODE_OFF
  return LocationManagerCompat.isLocationEnabled(manager)
 }
 private fun fail(message:String) {
  if(finished) return
  finished=true;cleanup();onError(message)
 }
 private fun cleanup() {
  handler.removeCallbacks(timeout)
  for(signal in signals) signal.cancel()
  signals.clear()
 }
 fun cancel() { finished=true;cleanup() }

 companion object {
  internal fun usable(location:Location):Boolean {
   val age=SystemClock.elapsedRealtimeNanos()-location.elapsedRealtimeNanos
   return location.latitude.isFinite() && location.latitude in -90.0..90.0 &&
    location.longitude.isFinite() && location.longitude in -180.0..180.0 &&
    location.hasAccuracy() && location.accuracy.isFinite() && location.accuracy>=0f &&
    age in 0..30000000000L
  }
 }
}
