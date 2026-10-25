package cl.induztek.protocolos;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController @RequestMapping("/api/v2")
public class CaseController {
 private final CaseStore store;private final CaseSecurity security;
 private final CatalogGateway gateway;
 public CaseController(CaseStore store,CaseSecurity security ,CatalogGateway gateway){this.store=store;this.security=security;this.gateway=gateway;}
 @GetMapping("/me") public JsonNode me(@RequestHeader(value="Authorization",required=false) String auth){return security.user(auth);}
 @GetMapping("/records") public List<CaseRecord> records(@RequestHeader(value="Authorization",required=false) String auth){
  JsonNode u=security.user(auth);var all=store.all();if(!CaseRules.client(u))return all;
  var catalog=gateway.records(auth);
  Set<String> installations=new HashSet<>(),equipment=new HashSet<>();
  for(var r:catalog)if(r.kind().equals("installations")&&CaseRules.text(r.payload(),"clientId").equals(CaseRules.text(u,"clientId")))installations.add(r.id());
  for(var r:catalog)if(r.kind().equals("equipment")&&installations.contains(CaseRules.text(r.payload(),"installationId")))equipment.add(r.id());
  return all.stream().filter(r->switch(r.kind()){
   case "clients"->r.id().equals(CaseRules.text(u,"clientId"));
   case "installations"->installations.contains(r.id());
   case "equipment"->equipment.contains(r.id());
   case "users"->r.id().equals(CaseRules.text(u,"id"));
   case "visits"->CaseRules.text(r.payload(),"clientId").equals(CaseRules.text(u,"clientId"))&&r.payload().hasNonNull("closedAt");
   case "protocols"->equipment.contains(CaseRules.text(r.payload(),"equipmentId"))&&!List.of("BORRADOR","ANULADO").contains(CaseRules.text(r.payload(),"state"));
   default->false;
  }).toList();
 }
 @PutMapping("/records/{kind}/{id}") public synchronized CaseRecord save(@PathVariable String kind,@PathVariable String id,@RequestHeader(value="Authorization",required=false) String auth,@RequestBody JsonNode payload){
  JsonNode u=security.user(auth);CaseRules.need(CaseStore.KINDS.contains(kind),"Recurso desconocido");
  if(CaseRules.client(u))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"El cliente tiene acceso de consulta");
  if(List.of("users","instruments").contains(kind)&&!CaseRules.text(u,"role").equals("JEFE"))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el jefe administra este recurso");
  var old=store.get(kind,id);
  
  if(old!=null&&old.revision()==payload.path("revision").asInt()&&old.payload().equals(payload))return old;
  if(payload.path("revision").asInt()!=(old==null?1:old.revision()+1))throw new ResponseStatusException(HttpStatus.CONFLICT,"Otro cambio ya ocupa esta revisión. El registro local se conserva");
  CaseRules.identity(id,payload,old);CaseRules.structure(kind,payload);
  CaseRules.work(kind,id,payload,u,old,gateway.records(auth),store.all());
  return store.save(kind,id,payload);
 }
}
