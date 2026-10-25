package cl.induztek.protocolos;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
@Service
public class CaseSecurity {
 private final CatalogGateway source;
 public CaseSecurity(CatalogGateway source){this.source=source;}
 public JsonNode user(String auth){
  if(auth==null||!auth.startsWith("Basic "))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Ingresa con una cuenta de demostración");
  String[] credentials;
  try{credentials=new String(Base64.getDecoder().decode(auth.substring(6)),StandardCharsets.UTF_8).split(":",2);}catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Credenciales inválidas");}
  if(credentials.length!=2||!credentials[1].equals(CaseRules.PASSWORD))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Credenciales inválidas");
  JsonNode u=source.user(auth);
  if(u==null||!u.path("active").asBoolean())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Cuenta inexistente o desactivada");return u;
 }
}
