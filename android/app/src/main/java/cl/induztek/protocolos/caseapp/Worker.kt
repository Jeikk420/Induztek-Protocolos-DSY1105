package cl.induztek.protocolos.caseapp
import android.content.Context
import androidx.work.*
import cl.induztek.protocolos.ProtocolApplication
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit
class CaseWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
 override suspend fun doWork():Result {
  val repo=(applicationContext as ProtocolApplication).caseRepository
  return try { repo.sync(repo.prefs.getString("last_user",null));if(repo.pending().isEmpty()) Result.success() else Result.retry() }
  catch(ex:Exception) { if(ex is CancellationException) throw ex;Result.retry() }
 }
 companion object {
  fun schedule(context:Context) {
   val constraints=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
   val request=OneTimeWorkRequestBuilder<CaseWorker>().setConstraints(constraints).setInitialDelay(5,TimeUnit.SECONDS).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build()
   WorkManager.getInstance(context).enqueueUniqueWork("case_sync",ExistingWorkPolicy.KEEP,request)
  }
 }
}
