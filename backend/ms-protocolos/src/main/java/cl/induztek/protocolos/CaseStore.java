package cl.induztek.protocolos;

import com.fasterxml.jackson.databind.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
public class CaseStore {
 private final JdbcTemplate db;
 private final ObjectMapper json;
 public CaseStore(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
 private JsonNode payload(String text){try{return json.readTree(text);}catch(Exception e){throw new IllegalStateException("Registro guardado ilegible",e);}}
 public List<CaseRecord> all(){
  return db.query("SELECT kind, record_id, revision, payload FROM case_records ORDER BY kind, record_id",(row,index)->new CaseRecord(row.getString("kind"),row.getString("record_id"),row.getInt("revision"),payload(row.getString("payload"))));
 }
 public CaseRecord get(String kind,String id){
  var found=db.query("SELECT kind, record_id, revision, payload FROM case_records WHERE record_key=?",(row,index)->new CaseRecord(row.getString("kind"),row.getString("record_id"),row.getInt("revision"),payload(row.getString("payload"))),kind+":"+id);
  return found.isEmpty()?null:found.get(0);
 }
 public CaseRecord save(String kind,String id,JsonNode value){
  int revision=value.path("revision").asInt();
  db.update("MERGE INTO case_records (record_key, kind, record_id, revision, payload) KEY(record_key) VALUES (?, ?, ?, ?, ?)",kind+":"+id,kind,id,revision,value.toString());
  return new CaseRecord(kind,id,revision,value);
 }
 @PostConstruct public void seed(){
  db.execute("CREATE TABLE IF NOT EXISTS case_records (record_key VARCHAR(160) PRIMARY KEY, kind VARCHAR(30) NOT NULL, record_id VARCHAR(80) NOT NULL, revision INT NOT NULL, payload CLOB NOT NULL)");
  try(var input=getClass().getResourceAsStream("/case-demo.json")){
   if(input==null)throw new IllegalStateException("Faltan los datos ficticios");
   JsonNode data=json.readTree(input);
   for(String kind:KINDS)for(JsonNode item:data.path(kind))if(get(kind,item.path("id").asText())==null)save(kind,item.path("id").asText(),item);
  }catch(Exception e){throw new IllegalStateException("No se pudieron cargar los datos ficticios",e);}
 }
 public static final List<String> KINDS=List.of("visits","protocols");
}
