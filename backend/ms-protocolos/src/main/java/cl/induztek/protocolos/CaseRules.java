package cl.induztek.protocolos;
import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;

public final class CaseRules {
 private CaseRules() {}
 public static final String PASSWORD="Induztek2026!";
 public static final ZoneId ZONE=ZoneId.of("America/Santiago");
 public static String text(JsonNode n,String key){return n.path(key).asText("");}
 public static void need(boolean ok,String message){if(!ok)throw new IllegalArgumentException(message);}
 public static boolean client(JsonNode u){return text(u,"role").equals("CLIENTE");}
 public static boolean review(JsonNode u){return List.of("INGENIERO","JEFE").contains(text(u,"role"));}
 public static JsonNode find(List<CaseRecord> records,String kind,String id){return records.stream().filter(r->r.kind().equals(kind)&&r.id().equals(id)).findFirst().map(CaseRecord::payload).orElse(null);}
 public static JsonNode reference(List<CaseRecord> records,String kind,String id){JsonNode v=find(records,kind,id);need(v!=null,"No existe "+kind+": "+id);return v;}
 public static boolean rut(String raw){
  String s=raw.trim().toUpperCase().replace(".","").replace("-","");if(!s.matches("[0-9]{7,8}[0-9K]"))return false;
  String body=s.substring(0,s.length()-1);if(body.startsWith("0"))return false;
  int sum=0,factor=2;for(int i=body.length()-1;i>=0;i--){sum+=(body.charAt(i)-'0')*factor;factor=factor==7?2:factor+1;}
  int dv=11-sum%11;char expected=dv==11?'0':dv==10?'K':(char)('0'+dv);return s.charAt(s.length()-1)==expected;
 }
 public static void required(JsonNode n,String key,int max){String s=text(n,key);need(!s.isBlank()&&s.trim().length()<=max,"Revisa el campo "+key);}
 public static double number(String raw){try{double v=Double.parseDouble(raw.trim().replace(',','.'));need(Double.isFinite(v),"Número no finito");return v;}catch(NumberFormatException e){throw new IllegalArgumentException("Valor numérico inválido");}}
 public static double numeric(JsonNode n,String key,boolean positive){double v=number(text(n,key));need(v>=0&&(!positive||v>0),"Revisa "+key);return v;}
 public static LocalDate date(String s){try{return LocalDate.parse(s);}catch(Exception e){throw new IllegalArgumentException("Fecha inválida: usa AAAA-MM-DD");}}
 public static String cleanRut(String s){return s.trim().replace(".","").replace("-","").toUpperCase();}
 public static boolean email(String raw){
  String s=raw.trim();if(s.length()>254||s.chars().filter(c->c=='@').count()!=1||s.chars().anyMatch(Character::isWhitespace))return false;
  String[] parts=s.split("@",-1);String local=parts[0];String[] labels=parts[1].split("\\.",-1);
  if(local.isBlank()||local.length()>64||local.startsWith(".")||local.endsWith(".")||local.contains("..")||!local.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+")||labels.length<2)return false;
  for(String label:labels)if(label.isBlank()||label.length()>63||!label.matches("[A-Za-z0-9-]+")||label.startsWith("-")||label.endsWith("-"))return false;
  return labels[labels.length-1].matches("[A-Za-z]{2,63}");
 }

 public static void structure(String kind,JsonNode v){
  need(v.isObject(),"Formato del registro inválido");
  List<String> fields=switch(kind){
   case "clients"->List.of("name","rut","giro","contact","phone","email");
   case "installations"->List.of("clientId","name","location","comuna","region","access");
   case "equipment"->List.of("installationId","tag","type","brand","model","serial","year","power","highVoltage","lowVoltage","current","contactMax","notes","photoPath","classification");
   case "users"->List.of("name","rut","role","email","clientId","cargo");
   case "instruments"->List.of("name","type","brand","model","serial","calibrated","expires");
   case "visits"->List.of("clientId","installationId","date","ot","engineerId","kind","actaJson");
   case "protocols"->List.of("number","visitId","equipmentId","ownerId","ownerName","temperature","humidity","state","result","notes","maintenance","historicalComment","reason","formalizedBy","replacesId");
   default->throw new IllegalArgumentException("Recurso desconocido");
  };
  for(String field:fields)need(v.path(field).isTextual(),"El campo "+field+" debe ser texto");
  if(kind.equals("users"))need(v.path("active").isBoolean(),"Estado de cuenta inválido");
  if(kind.equals("visits"))need(v.path("technicianIds").isArray(),"Participantes inválidos");
  if(kind.equals("protocols")){
   need(v.path("testedAt").isIntegralNumber()&&v.path("testedAt").asLong()>0&&v.path("tests").isArray()&&v.path("changes").isArray(),"Formato de protocolo inválido");
   for(JsonNode t:v.path("tests")){
    for(String field:List.of("type","instrumentId","reason","manualResult","comment"))need(t.path(field).isTextual(),"Formato de prueba inválido");
    for(String field:List.of("notPerformed","afterMaintenance","relayOperates"))need(t.path(field).isBoolean(),"Estado de prueba inválido");
    for(String list:List.of("values","previousValues")){need(t.path(list).isArray(),"Formato de lecturas inválido");for(JsonNode r:t.path(list))for(String field:List.of("key","label","unit","text"))need(r.path(field).isTextual(),"Formato de lectura inválido");}
   }
  }
 }
 public static void identity(String id,JsonNode v,CaseRecord old){
  need(v.isObject(),"El registro debe ser un objeto JSON");
  need(id.matches("[A-Za-z0-9-]{1,80}")&&id.equals(text(v,"id")),"Identificador inválido");
  need(v.path("revision").isInt()&&v.path("revision").asInt()==(old==null?1:old.revision()+1),"Revisión inválida");
 }
 private static void unique(List<CaseRecord> all,String kind,String id,java.util.function.Predicate<JsonNode> same,String msg){need(all.stream().noneMatch(r->r.kind().equals(kind)&&!r.id().equals(id)&&same.test(r.payload())),msg);}
 public static void catalog(String kind,String id,JsonNode v,JsonNode u,List<CaseRecord> all){
  need(!client(u),"El cliente solo consulta");
  if(List.of("users","instruments").contains(kind))need(text(u,"role").equals("JEFE"),"Solo el jefe técnico administra este recurso");
  switch(kind){
   case "clients" -> {
    for(String key:List.of("name","giro","contact"))required(v,key,120);
    need(rut(text(v,"rut")),"RUT inválido");need(email(text(v,"email")),"Correo inválido");need(text(v,"phone").chars().filter(Character::isDigit).count()>=8&&text(v,"phone").chars().filter(Character::isDigit).count()<=15&&text(v,"phone").matches("\\+?[0-9 ()-]{8,20}"),"Teléfono inválido");
    unique(all,kind,id,n->cleanRut(text(n,"rut")).equals(cleanRut(text(v,"rut"))),"RUT de cliente duplicado");
   }
   case "installations" -> {
    reference(all,"clients",text(v,"clientId"));for(String key:List.of("name","comuna","region"))required(v,key,120);required(v,"location",180);need(text(v,"access").length()<=500,"Observaciones demasiado largas");
    unique(all,kind,id,n->text(n,"clientId").equals(text(v,"clientId"))&&text(n,"name").trim().equalsIgnoreCase(text(v,"name").trim()),"Instalación duplicada");
   }
   case "equipment" -> {
    reference(all,"installations",text(v,"installationId"));required(v,"tag",60);for(String key:List.of("brand","model","serial"))required(v,key,120);
    String type=text(v,"type");need(List.of("TRANSFORMADOR","INTERRUPTOR","CELDA","CABLE","RELE").contains(type),"Tipo inválido");need(text(v,"classification").equals(type.equals("RELE")?"Secundario":"Primario"),"Clasificación inválida");
    int year;try{year=Integer.parseInt(text(v,"year"));}catch(Exception e){throw new IllegalArgumentException("Año inválido");}need(year>=1900&&year<=LocalDate.now(ZONE).getYear(),"Año fuera de rango");numeric(v,"highVoltage",true);
    if(type.equals("TRANSFORMADOR")){numeric(v,"power",true);numeric(v,"lowVoltage",true);}if(List.of("CELDA","INTERRUPTOR","RELE").contains(type))numeric(v,"current",true);if(List.of("CELDA","INTERRUPTOR").contains(type))numeric(v,"contactMax",true);
    need(text(v,"notes").length()<=2000,"Observaciones demasiado largas");need(text(v,"photoPath").isBlank(),"La foto local no se envía como una ruta del dispositivo");
    unique(all,kind,id,n->text(n,"installationId").equals(text(v,"installationId"))&&text(n,"tag").trim().equalsIgnoreCase(text(v,"tag").trim()),"TAG duplicado en la instalación");
   }
   case "instruments" -> {
    for(String key:List.of("name","brand","model","serial"))required(v,key,120);need(List.of("AISLACION","TTR","DEVANADOS","CONTACTOS","RELE").contains(text(v,"type")),"Uso de instrumento inválido");
    LocalDate a=date(text(v,"calibrated")),b=date(text(v,"expires"));need(!a.isAfter(LocalDate.now(ZONE))&&!b.isBefore(a),"Fechas de calibración inválidas");
    unique(all,kind,id,n->text(n,"serial").trim().equalsIgnoreCase(text(v,"serial").trim()),"Serie duplicada");
   }
   case "users" -> {
    required(v,"name",120);need(rut(text(v,"rut")),"RUT inválido");need(email(text(v,"email")),"Correo inválido");need(List.of("TECNICO","INGENIERO","JEFE","CLIENTE").contains(text(v,"role")),"Perfil inválido");
    if(client(v)){reference(all,"clients",text(v,"clientId"));required(v,"cargo",120);}if(id.equals(text(u,"id")))need(v.path("active").asBoolean()&&text(v,"role").equals("JEFE"),"Conserva tu cuenta de jefe activa");
    unique(all,kind,id,n->text(n,"email").trim().equalsIgnoreCase(text(v,"email").trim())||cleanRut(text(n,"rut")).equals(cleanRut(text(v,"rut"))),"Correo o RUT duplicado");
   }
   default -> throw new IllegalArgumentException("Recurso desconocido");
  }
 }
 public static List<String> tests(String type){return switch(type){case "TRANSFORMADOR"->List.of("AISLACION","TTR","DEVANADOS");case "CELDA","INTERRUPTOR"->List.of("AISLACION","CONTACTOS");case "CABLE"->List.of("AISLACION");case "RELE"->List.of("RELE");default->throw new IllegalArgumentException("Tipo inválido");};}
 public static List<String> keys(String type,String test){
  var keys=new ArrayList<String>();switch(test){
   case "AISLACION"->{List<String> phases=type.equals("TRANSFORMADOR")?List.of("AT-BT","AT-T","BT-T"):type.equals("CABLE")?List.of("A","B","C"):List.of("Equipo");for(String p:phases){keys.add(p+"_1");if(List.of("TRANSFORMADOR","CABLE").contains(type))keys.add(p+"_10");keys.add(p+"_kv");}}
   case "TTR"->{for(String p:List.of("U","V","W")){keys.add(p+"_theory");keys.add(p+"_measured");}keys.add("tap");}
   case "DEVANADOS"->{for(String w:List.of("AT","BT"))for(String p:List.of("U","V","W"))keys.add(w+"_"+p);}
   case "CONTACTOS"->keys.addAll(List.of("A","B","C"));case "RELE"->keys.addAll(List.of("adjust","measured","expected","time"));
  }return keys;
 }
 public static JsonNode reading(JsonNode t,String key){for(JsonNode v:t.path("values"))if(text(v,"key").equals(key))return v;throw new IllegalArgumentException("Falta medición "+key);}
 public static double val(JsonNode t,String key){return number(text(reading(t,key),"text"));}
 public static double ohms(JsonNode t,String key){JsonNode r=reading(t,key);double n=number(text(r,"text"));double converted=text(r,"unit").equals("GΩ")?n*1000:n;need(Double.isFinite(converted),"Valor demasiado grande");return converted;}
 public static void test(JsonNode t,JsonNode equipment,List<CaseRecord> catalog,LocalDate day){
  if(t.path("notPerformed").asBoolean()){required(t,"reason",500);return;}
  String type=text(t,"type");JsonNode i=reference(catalog,"instruments",text(t,"instrumentId"));need(text(i,"type").equals(type),"Instrumento incorrecto para la prueba");need(!day.isBefore(date(text(i,"calibrated")))&&!day.isAfter(date(text(i,"expires"))),"Calibración vencida o no vigente");
  String equipmentType=text(equipment,"type");var actual=new ArrayList<String>();for(JsonNode v:t.path("values"))actual.add(text(v,"key"));need(actual.equals(keys(equipmentType,type)),"Mediciones no corresponden al equipo");
  for(JsonNode r:t.path("values")){
   String key=text(r,"key"),unit=text(r,"unit");String expected=key.endsWith("_kv")?"kV":type.equals("AISLACION")?"MΩ":type.equals("TTR")?(key.equals("tap")?"posición":"relación"):type.equals("DEVANADOS")?"mΩ":type.equals("CONTACTOS")?"µΩ":List.of("adjust","measured").contains(key)?"A":"ms";
   need(unit.equals(expected)||(expected.equals("MΩ")&&unit.equals("GΩ")),"Unidad incorrecta");
   if(type.equals("RELE")&&!t.path("relayOperates").asBoolean()&&List.of("measured","time").contains(key))continue;
   double n=number(text(r,"text"));boolean positive=key.endsWith("_kv")||key.endsWith("_theory")||List.of("adjust","expected","tap").contains(key);need(n>=0&&(!positive||n>0),"Medición negativa o cero no permitido");if(key.equals("tap"))need(n==Math.floor(n),"Posición de tap no entera");
  }
  if(t.path("afterMaintenance").asBoolean()){
   var previous=t.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)previous).set("values",t.path("previousValues"));((com.fasterxml.jackson.databind.node.ObjectNode)previous).put("afterMaintenance",false);test(previous,equipment,catalog,day);
  }
 }
 public static String worst(List<String> outcomes){return outcomes.contains("RECHAZADO")?"RECHAZADO":outcomes.contains("OBSERVADO")?"OBSERVADO":outcomes.contains("APROBADO")?"APROBADO":"SIN_RESULTADO";}
 public static String result(JsonNode t,JsonNode e){
  if(t.path("notPerformed").asBoolean())return "NO_REALIZADA";var results=new ArrayList<String>();switch(text(t,"type")){
   case "AISLACION"->{for(JsonNode v:t.path("values")){String k=text(v,"key");if(!k.endsWith("_1"))continue;double one=ohms(t,k);if(one<100){results.add("RECHAZADO");continue;}if(List.of("CELDA","INTERRUPTOR").contains(text(e,"type"))){results.add("APROBADO");continue;}double ip=ohms(t,k.substring(0,k.length()-2)+"_10")/one;results.add(ip>=2?"APROBADO":ip>=1?"OBSERVADO":"RECHAZADO");}}
   case "TTR"->{for(String p:List.of("U","V","W")){double theory=val(t,p+"_theory"),error=Math.abs(val(t,p+"_measured")-theory)/theory*100;results.add(error<=0.5+1e-9?"APROBADO":error<=1+1e-9?"OBSERVADO":"RECHAZADO");}}
   case "CONTACTOS"->{double max=number(text(e,"contactMax"));for(JsonNode v:t.path("values")){double ratio=number(text(v,"text"))/max;results.add(ratio<=1+1e-9?"APROBADO":ratio<=1.2+1e-9?"OBSERVADO":"RECHAZADO");}}
   case "RELE"->{if(!t.path("relayOperates").asBoolean())results.add("RECHAZADO");else{double error=Math.abs(val(t,"time")-val(t,"expected"))/val(t,"expected")*100;results.add(error<=5+1e-9?"APROBADO":error<=10+1e-9?"OBSERVADO":"RECHAZADO");}}
   case "DEVANADOS"->results.add(List.of("APROBADO","OBSERVADO","RECHAZADO").contains(text(t,"manualResult"))?text(t,"manualResult"):"OBSERVADO");
  }return worst(results);
 }
 public static String global(JsonNode p,JsonNode e){var results=new ArrayList<String>();for(JsonNode t:p.path("tests"))if(!t.path("notPerformed").asBoolean())results.add(result(t,e));return worst(results);}
 public static void protocolComplete(JsonNode p,JsonNode equip,JsonNode visit,List<CaseRecord> catalog){
  double temp=number(text(p,"temperature")),hum=number(text(p,"humidity"));need(temp>=-50&&temp<=80&&hum>=0&&hum<=100,"Condiciones ambientales inválidas");
  var actual=new ArrayList<String>();boolean performed=false;LocalDate day=Instant.ofEpochMilli(p.path("testedAt").asLong()).atZone(ZONE).toLocalDate();need(day.equals(date(text(visit,"date"))),"La prueba debe tener la fecha de la visita");
  for(JsonNode t:p.path("tests")){actual.add(text(t,"type"));if(!t.path("notPerformed").asBoolean())performed=true;test(t,equip,catalog,day);if(t.path("afterMaintenance").asBoolean())need(text(visit,"kind").equals("Pruebas y mantenimiento"),"Repetición fuera de una visita de mantenimiento");}
  need(actual.equals(tests(text(equip,"type")))&&performed,"Pruebas incompletas: realiza al menos una y justifica las demás");if(text(visit,"kind").equals("Pruebas y mantenimiento"))required(p,"maintenance",1000);
 }
 public static boolean significant(JsonNode p,List<CaseRecord> work){
  JsonNode previous=null;for(CaseRecord r:work){JsonNode n=r.payload();if(r.kind().equals("protocols")&&!r.id().equals(text(p,"id"))&&text(n,"equipmentId").equals(text(p,"equipmentId"))&&n.path("testedAt").asLong()<p.path("testedAt").asLong()&&!List.of("BORRADOR","ANULADO").contains(text(n,"state"))&&(previous==null||n.path("testedAt").asLong()>previous.path("testedAt").asLong()))previous=n;}
  if(previous==null)return false;
  for(JsonNode t:p.path("tests"))if(!t.path("notPerformed").asBoolean())for(JsonNode old:previous.path("tests"))if(text(t,"type").equals(text(old,"type"))&&!old.path("notPerformed").asBoolean())for(JsonNode v:t.path("values")){
   String k=text(v,"key");if(k.endsWith("_kv")||k.endsWith("_theory")||List.of("tap","adjust","expected").contains(k))continue;
   try{double a=ohms(old,k),b=ohms(t,k);if(a!=0&&Math.abs((b-a)/Math.abs(a)*100)>25)return true;}catch(IllegalArgumentException ignored){}
  }return false;
 }
 public static void work(String kind,String id,JsonNode v,JsonNode u,CaseRecord old,List<CaseRecord> catalog,List<CaseRecord> work){
  need(!client(u),"El usuario cliente solo consulta");String role=text(u,"role"),uid=text(u,"id");
  if(kind.equals("visits")){
   need(List.of("TECNICO","INGENIERO").contains(role),"Solo el personal de terreno abre o cierra visitas");JsonNode installation=reference(catalog,"installations",text(v,"installationId"));need(text(installation,"clientId").equals(text(v,"clientId")),"Instalación de otro cliente");required(v,"ot",60);need(!date(text(v,"date")).isAfter(LocalDate.now(ZONE)),"Fecha futura");need(List.of("Solo pruebas","Pruebas y mantenimiento").contains(text(v,"kind")),"Tipo de visita inválido");
   JsonNode engineer=reference(catalog,"users",text(v,"engineerId"));need(text(engineer,"role").equals("INGENIERO")&&engineer.path("active").asBoolean(),"Ingeniero responsable inválido");boolean participant=false;need(v.path("technicianIds").isArray()&&!v.path("technicianIds").isEmpty(),"Faltan participantes");
   for(JsonNode user:v.path("technicianIds")){JsonNode person=reference(catalog,"users",user.asText());need(person.path("active").asBoolean()&&List.of("TECNICO","INGENIERO").contains(text(person,"role")),"Participante inválido");if(user.asText().equals(uid))participant=true;}need(participant,"No participas en esta visita");
   unique(work,kind,id,n->text(n,"installationId").equals(text(v,"installationId"))&&text(n,"ot").trim().equalsIgnoreCase(text(v,"ot").trim()),"OT duplicada");
   if(old!=null){need(!old.payload().hasNonNull("closedAt"),"Visita ya cerrada");for(String field:List.of("clientId","installationId","date","ot","kind","engineerId","technicianIds"))need(old.payload().path(field).equals(v.path(field)),"No cambies la identificación de la visita");}
   if(v.hasNonNull("closedAt")){
    need(old!=null,"La visita debe abrirse antes de cerrarse");JsonNode receipt=v.path("receipt");required(receipt,"name",120);required(receipt,"cargo",120);need(rut(text(receipt,"rut")),"RUT del receptor inválido");
    int count=0;for(CaseRecord p:work)if(p.kind().equals("protocols")&&text(p.payload(),"visitId").equals(id)){count++;need(List.of("REGISTRADO","ANULADO").contains(text(p.payload(),"state")),"Hay protocolos sin registrar");}need(count>0,"La visita no tiene protocolos");
    need(v.path("closedAt").isNumber()&&v.path("closedAt").asLong()<=System.currentTimeMillis()+60000&&receipt.path("receivedAt").asLong()==v.path("closedAt").asLong(),"Fecha de recepción inválida");
    try {JsonNode snapshot=new ObjectMapper().readTree(text(v,"actaJson"));var expected=new ArrayList<JsonNode>();for(var p:work)if(p.kind().equals("protocols")&&text(p.payload(),"visitId").equals(id)&&!text(p.payload(),"state").equals("ANULADO"))expected.add(p.payload());need(snapshot.isArray()&&snapshot.size()==expected.size(),"Acta incompleta");for(var p:expected){boolean found=false;for(JsonNode item:snapshot)if(item.equals(p))found=true;need(found,"El acta no coincide con los protocolos registrados");}}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalArgumentException("Acta inválida");}

   }
   if(v.hasNonNull("latitude")||v.hasNonNull("longitude"))need(v.path("latitude").isNumber()&&v.path("longitude").isNumber()&&Math.abs(v.path("latitude").asDouble())<=90&&Math.abs(v.path("longitude").asDouble())<=180,"Coordenadas inválidas");
   return;
  }
  need(kind.equals("protocols"),"Recurso desconocido");JsonNode equip=reference(catalog,"equipment",text(v,"equipmentId")),visit=reference(work,"visits",text(v,"visitId"));need(text(equip,"installationId").equals(text(visit,"installationId")),"Equipo ajeno a la visita");
  JsonNode owner=reference(catalog,"users",text(v,"ownerId"));need(!client(owner)&&text(owner,"name").equals(text(v,"ownerName")),"Responsable inválido");need(v.path("testedAt").isNumber()&&v.path("testedAt").asLong()<=System.currentTimeMillis()+60000,"Fecha de prueba inválida");
  unique(work,kind,id,n->text(n,"visitId").equals(text(v,"visitId"))&&text(n,"equipmentId").equals(text(v,"equipmentId")),"El equipo ya tiene protocolo en esta visita");
  unique(work,kind,id,n->text(n,"number").equals(text(v,"number")),"Número de protocolo duplicado");
  need(v.path("tests").isArray()&&v.path("tests").size()<=3,"Formato de pruebas inválido");for(JsonNode t:v.path("tests"))need(t.isObject()&&t.path("values").isArray()&&t.path("previousValues").isArray(),"Formato de mediciones inválido");
  String state=text(v,"state"),before=old==null?"NUEVO":text(old.payload(),"state");need(List.of("BORRADOR","REGISTRADO","OBSERVADO","FORMALIZADO","ANULADO").contains(state),"Estado inválido");
  need(text(v,"notes").length()<=2000&&text(v,"maintenance").length()<=1000&&text(v,"historicalComment").length()<=1000,"Texto demasiado largo");
  if(old==null){need(state.equals("BORRADOR")&&text(v,"ownerId").equals(uid)&&!visit.hasNonNull("closedAt"),"Solo puedes crear un borrador propio en una visita abierta");boolean included=false;for(JsonNode a:visit.path("technicianIds"))if(a.asText().equals(uid))included=true;need(included||role.equals("JEFE"),"No participas en la visita");required(v,"number",80);}
  else {
   if(!List.of("BORRADOR","OBSERVADO").contains(before)||!state.equals(before)){for(String field:List.of("temperature","humidity","notes","maintenance"))need(old.payload().path(field).equals(v.path(field)),"Cambia las mediciones solo al editar un borrador u observado");}
   need(!List.of("FORMALIZADO","ANULADO").contains(before),"Protocolo bloqueado");for(String field:List.of("number","visitId","equipmentId","ownerId","ownerName","testedAt","replacesId"))need(old.payload().path(field).equals(v.path(field)),"No cambies la identidad del protocolo");
   if(state.equals(before)&&List.of("BORRADOR","OBSERVADO").contains(before)){need(!role.equals("TECNICO")||text(v,"ownerId").equals(uid),"Solo puedes editar tus propios protocolos");if(role.equals("TECNICO")){var oldTests=old.payload().path("tests");for(int i=0;i<v.path("tests").size();i++)need(text(v.path("tests").get(i),"manualResult").equals(text(oldTests.path(i),"manualResult")),"El resultado de devanados es de ingeniería");}}
   else if(state.equals("REGISTRADO")&&List.of("BORRADOR","OBSERVADO").contains(before)){need(!role.equals("TECNICO")||text(v,"ownerId").equals(uid),"Solo puedes registrar tus propios protocolos");protocolComplete(v,equip,visit,catalog);need(text(v,"result").equals(global(v,equip)),"Resultado global incorrecto");}
   else if(state.equals("REGISTRADO")&&before.equals("REGISTRADO")){need(review(u),"Solo ingeniería guarda la revisión");for(String field:List.of("temperature","humidity","notes","maintenance"))need(old.payload().path(field).equals(v.path(field)),"La revisión no cambia las mediciones");need(v.path("tests").size()==old.payload().path("tests").size(),"No cambies las pruebas durante la revisión");for(int i=0;i<v.path("tests").size();i++){var t=v.path("tests").get(i).deepCopy();var oldTest=old.payload().path("tests").path(i).deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)t).remove("manualResult");((com.fasterxml.jackson.databind.node.ObjectNode)oldTest).remove("manualResult");need(t.equals(oldTest),"La revisión no cambia las pruebas");}}
   else if(state.equals("FORMALIZADO")&&before.equals("REGISTRADO")){need(review(u)&&visit.hasNonNull("closedAt"),"Solo ingeniería formaliza después del cierre de visita");need(text(v,"formalizedBy").equals(text(u,"name"))&&v.hasNonNull("formalizedAt"),"Falta autor de aprobación");for(JsonNode t:v.path("tests"))if(text(t,"type").equals("DEVANADOS")&&!t.path("notPerformed").asBoolean())need(List.of("APROBADO","OBSERVADO","RECHAZADO").contains(text(t,"manualResult")),"Falta resultado manual de devanados");need(!significant(v,work)||!text(v,"historicalComment").isBlank(),"Falta comentario por variación significativa");need(old.payload().path("tests").equals(v.path("tests")),"Formalizar no modifica las pruebas");need(text(v,"result").equals(global(v,equip)),"Resultado global incorrecto");}
   else if((state.equals("OBSERVADO")&&before.equals("REGISTRADO"))||state.equals("ANULADO")){need(review(u),"Solo ingeniería observa o anula");required(v,"reason",500);need(old.payload().path("tests").equals(v.path("tests")),"Cambiar el estado no modifica las pruebas");}
   else throw new IllegalArgumentException("Transición de estado inválida");
  }
  if(!text(v,"replacesId").isBlank()){
   JsonNode original=reference(work,"protocols",text(v,"replacesId"));need(text(original,"state").equals("FORMALIZADO")&&text(original,"equipmentId").equals(text(v,"equipmentId")),"Reemplazo inválido");unique(work,kind,id,n->text(n,"replacesId").equals(text(v,"replacesId"))&&!text(n,"state").equals("ANULADO"),"El original ya tiene reemplazo");
  }
  need(v.path("changes").isArray()&&!v.path("changes").isEmpty(),"Falta trazabilidad");JsonNode last=v.path("changes").get(v.path("changes").size()-1);need(text(last,"userId").equals(uid)&&text(last,"userName").equals(text(u,"name"))&&text(last,"to").equals(state),"Autor de cambio inválido");need(text(last,"from").equals(before)&&last.path("at").isNumber()&&last.path("at").asLong()<=System.currentTimeMillis()+60000,"Cambio sin fecha o estado de origen válidos");
  if(old!=null){JsonNode changes=old.payload().path("changes");need(v.path("changes").size()==changes.size()+1,"No borres el historial de cambios");for(int i=0;i<changes.size();i++)need(changes.get(i).equals(v.path("changes").get(i)),"No modifiques el historial anterior");}
 }
}
