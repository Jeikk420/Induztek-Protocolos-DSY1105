package cl.induztek.protocolos.caseapp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
private val palette=lightColorScheme(
 primary=Color(0xFF007F8B),onPrimary=Color.White,primaryContainer=Color(0xFFD0F3F5),onPrimaryContainer=Color(0xFF003D43),
 secondary=Color(0xFF42666A),onSecondary=Color.White,secondaryContainer=Color(0xFFDDEBEC),onSecondaryContainer=Color(0xFF19383B),
 background=Color(0xFFF1F5F7),onBackground=Color(0xFF202C30),surface=Color.White,onSurface=Color(0xFF202C30),
 surfaceVariant=Color(0xFFE3E8EA),onSurfaceVariant=Color(0xFF46565A),outline=Color(0xFF708287),
 error=Color(0xFFBA1A1A),onError=Color.White,errorContainer=Color(0xFFFFDAD6),onErrorContainer=Color(0xFF410002))
@Composable fun CaseTheme(content:@Composable ()->Unit) { MaterialTheme(colorScheme=palette,shapes=Shapes(extraSmall=RoundedCornerShape(8.dp),small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(18.dp),large=RoundedCornerShape(22.dp),extraLarge=RoundedCornerShape(28.dp)),content=content) }
