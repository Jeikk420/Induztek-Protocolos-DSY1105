package cl.induztek.protocolos.caseapp

import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import kotlinx.coroutines.CancellationException
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import cl.induztek.protocolos.R
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import java.io.File

@Composable fun PhotoPreview(path:String) {
 val bitmap by produceState<Bitmap?>(null,path) {
  value=withContext(Dispatchers.IO) { try {
   val options=BitmapFactory.Options().apply { inJustDecodeBounds=true };BitmapFactory.decodeFile(path,options)
   var sample=1;while(options.outWidth/sample>1200 || options.outHeight/sample>1200) sample*=2
   BitmapFactory.decodeFile(path,BitmapFactory.Options().apply { inSampleSize=sample })
  } catch(_:Exception) { null } }
 }
 bitmap?.let { Image(it.asImageBitmap(),"Fotografía de la placa del equipo",Modifier.fillMaxWidth().height(180.dp),contentScale=ContentScale.Fit) }
}
@Composable fun PhotoField(path:String,onPhoto:(String)->Unit,notify:(String)->Unit,onLoading:(Boolean)->Unit) {
 val context=LocalContext.current
 var pendingPath by rememberSaveable { mutableStateOf("") }
 var selectedImage by rememberSaveable { mutableStateOf("") }
 val currentPhoto by rememberUpdatedState(onPhoto)
 val currentNotify by rememberUpdatedState(notify)
 val importing = selectedImage.isNotBlank()
 SideEffect { onLoading(importing) }
 val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
  if (uri == null) currentNotify("Selección cancelada. La foto anterior se conserva.")
  else {
   try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
   catch (_: SecurityException) { }
   selectedImage = uri.toString()
  }
 }
 LaunchedEffect(selectedImage) {
  if (selectedImage.isNotBlank()) {
   val uri = Uri.parse(selectedImage)
   try {
    val photo = withContext(Dispatchers.IO) { PhotoStorage.importImage(context, uri) }
    currentPhoto(photo)
    currentNotify("Imagen seleccionada. Guarda el equipo para asociarla.")
   } catch (error: Exception) {
    if (error is CancellationException) throw error
    currentNotify(if (error is IllegalArgumentException) error.message ?: "Imagen inválida." else "No se pudo guardar la imagen. Intenta con otra foto.")
   }
   try { context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
   catch (_: SecurityException) { }
   selectedImage = ""
  }
 }

 val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
  if(ok && pendingPath.isNotBlank() && File(pendingPath).length()>0) { onPhoto(pendingPath);notify("Foto capturada. Guarda el equipo para asociarla.") }
  else { if(pendingPath.isNotBlank()) File(pendingPath).delete();notify(if(ok) "La cámara no entregó una foto válida. Puedes reintentar." else "Captura cancelada. Puedes continuar sin foto.") }
  pendingPath=""
 }
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Text("Foto de la placa (opcional)",style=MaterialTheme.typography.titleSmall)
  if(path.isNotBlank()) PhotoPreview(path)
  OutlinedButton(onClick={
   try {
    val directory=File(context.filesDir,"photos").apply { mkdirs() }
    val file=File.createTempFile("placa-",".jpg",directory);pendingPath=file.absolutePath
    val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
    camera.launch(uri)
   } catch(_:Exception) { if(pendingPath.isNotBlank()) File(pendingPath).delete();pendingPath="";notify("No se pudo abrir la cámara. Comprueba que el dispositivo tenga una app de cámara.") }
  },modifier=Modifier.fillMaxWidth(),enabled=!importing && pendingPath.isBlank() && context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) { Icon(painterResource(R.drawable.ic_camera),null);Spacer(Modifier.width(8.dp));Text("Tomar fotografía") }
  OutlinedButton(onClick={
   try { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
   catch (_: Exception) { notify("No se pudo abrir la galería. Comprueba que el teléfono tenga un selector de imágenes.") }
  }, enabled=!importing && pendingPath.isBlank(), modifier=Modifier.fillMaxWidth()) {
   Icon(painterResource(R.drawable.ic_gallery),null)
   Spacer(Modifier.width(8.dp))
   Text("Elegir de la galería")
  }
  if (importing) {
   LinearProgressIndicator(Modifier.fillMaxWidth())
   Text("Guardando imagen…")
  }

 }
}
@Composable fun LocationField(latitude:Double?,longitude:Double?,accuracy:Float?,onLocation:(Double,Double,Float,Long)->Unit,notify:(String)->Unit) {
 val context=LocalContext.current
 val result by rememberUpdatedState(onLocation);val message by rememberUpdatedState(notify)
 var loading by remember { mutableStateOf(false) }
 var request by remember { mutableStateOf<VisitLocationCapture?>(null) }
 fun capture() {
  request?.cancel();loading=true
  val next=VisitLocationCapture(context,{ location ->
   loading=false;request=null
   result(location.latitude,location.longitude,location.accuracy,System.currentTimeMillis())
   message("Ubicación de la visita capturada.")
  },{ error -> loading=false;request=null;message(error) })
  request=next;next.start()
 }
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
  if(grants[Manifest.permission.ACCESS_FINE_LOCATION]==true || grants[Manifest.permission.ACCESS_COARSE_LOCATION]==true) capture()
  else message("Permiso denegado. La ubicación es opcional y puedes seguir registrando.")
 }
 DisposableEffect(context) { onDispose { request?.cancel() } }
 val fine=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
 val coarse=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Text("Ubicación de la visita (opcional)",style=MaterialTheme.typography.titleSmall)
  if(latitude!=null && longitude!=null) Text("Latitud: ${fmt(latitude)} · Longitud: ${fmt(longitude)}\nPrecisión aproximada: ${accuracy?.toInt() ?: 0} m")
  OutlinedButton(onClick={
   if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED) capture()
   else permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION))
  },enabled=!loading,modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.LocationOn,null);Spacer(Modifier.width(8.dp));Text(if(loading) "Buscando ubicación…" else "Obtener ubicación") }
  if(loading) TextButton(onClick={request?.cancel();request=null;loading=false;message("Búsqueda cancelada. Los datos de la visita se conservan.")}) { Text("Cancelar búsqueda") }
  if(coarse && !fine) TextButton(onClick={permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION))},enabled=!loading) { Text("Solicitar ubicación precisa") }
  TextButton(onClick={
   try { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
   catch(_:Exception) { message("Abre los ajustes del dispositivo y activa Ubicación.") }
  }) { Text("Abrir ajustes de ubicación") }
 }
}
