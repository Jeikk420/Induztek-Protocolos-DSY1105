package cl.induztek.protocolos
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.induztek.protocolos.caseapp.*
class MainActivity:ComponentActivity() {
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState);enableEdgeToEdge();setContent { CaseTheme { CaseApp(viewModel(factory=CaseViewModel.Factory)) } } }
}
