package cl.induztek.protocolos;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
@Service
public class CatalogGateway {
 private final RestClient client;
 public CatalogGateway(@Value("${equipos.base-url:http://localhost:8081}") String url){
  var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build());factory.setReadTimeout(Duration.ofSeconds(6));
  client=RestClient.builder().baseUrl(url).requestFactory(factory).build();
 }
 public JsonNode user(String auth){return get("/api/v2/me",auth,JsonNode.class);}
 public List<CaseRecord> records(String auth){return Arrays.asList(get("/api/v2/records",auth,CaseRecord[].class));}
 private <T>T get(String path,String auth,Class<T> type){
  try{return client.get().uri(path).header("Authorization",auth).retrieve().body(type);}
  catch(RestClientResponseException e){throw new ResponseStatusException(e.getStatusCode(),"No se pudo verificar la cuenta en el catálogo");}
  catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Inicia ms-equipos antes de sincronizar protocolos");}
 }
}
