package cl.induztek.protocolos;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CaseRulesTest {
 final ObjectMapper mapper=new ObjectMapper();JsonNode fixture;List<CaseRecord> catalog=new ArrayList<>(),work=new ArrayList<>();
 @BeforeEach void load() throws Exception {fixture=mapper.readTree(getClass().getResourceAsStream("/case-demo.json"));for(String k:List.of("clients","installations","equipment","instruments","users"))for(JsonNode n:fixture.path(k))catalog.add(new CaseRecord(k,n.path("id").asText(),n.path("revision").asInt(),n));for(String k:List.of("visits","protocols"))for(JsonNode n:fixture.path(k))work.add(new CaseRecord(k,n.path("id").asText(),1,n));}
 JsonNode user(String id){return CaseRules.reference(catalog,"users",id);} JsonNode equip(){return CaseRules.reference(catalog,"equipment","eq1");}
 ObjectNode isolation(String one,String ten){ObjectNode t=fixture.path("protocols").get(2).path("tests").get(0).deepCopy();for(JsonNode r:t.path("values")){String k=r.path("key").asText();((ObjectNode)r).put("text",k.endsWith("_1")?one:k.endsWith("_10")?ten:"5");}return t;}
 @Test void estructuraDeDatosAndroidCompatible(){for(var r:catalog)assertDoesNotThrow(()->CaseRules.structure(r.kind(),r.payload()));for(var r:work)assertDoesNotThrow(()->CaseRules.structure(r.kind(),r.payload()));}
 @Test void rutModuloOnce(){assertTrue(CaseRules.rut("17.284.519-3"));assertFalse(CaseRules.rut("17.284.519-4"));assertFalse(CaseRules.rut("00.000.000-0"));}
 @Test void decimalYNoFinito(){assertEquals(610.5,CaseRules.number("610,5"));assertThrows(IllegalArgumentException.class,()->CaseRules.number("Infinity"));assertThrows(IllegalArgumentException.class,()->CaseRules.number("NaN"));}
 @Test void aislamientoUmbrales(){assertEquals("RECHAZADO",CaseRules.result(isolation("99","1000"),equip()));assertEquals("APROBADO",CaseRules.result(isolation("100","200"),equip()));assertEquals("OBSERVADO",CaseRules.result(isolation("600","600"),equip()));assertEquals("RECHAZADO",CaseRules.result(isolation("600","599"),equip()));}
 @Test void gigaOhms(){var t=isolation("0.6","1.2");for(JsonNode r:t.path("values"))if(!r.path("key").asText().endsWith("_kv"))((ObjectNode)r).put("unit","GΩ");assertEquals("APROBADO",CaseRules.result(t,equip()));}
 @Test void peorResultado(){assertEquals("RECHAZADO",CaseRules.worst(List.of("APROBADO","RECHAZADO")));}
 @Test void tagDuplicado(){var v=equip().deepCopy();((ObjectNode)v).put("id","otro");assertThrows(IllegalArgumentException.class,()->CaseRules.catalog("equipment","otro",v,user("t1"),catalog));}
 @Test void clienteSoloLectura(){assertThrows(IllegalArgumentException.class,()->CaseRules.catalog("equipment","eq1",equip(),user("c1"),catalog));}
 @Test void instrumentoSoloJefe(){JsonNode i=CaseRules.reference(catalog,"instruments","i1");assertThrows(IllegalArgumentException.class,()->CaseRules.catalog("instruments","i1",i,user("t1"),catalog));assertDoesNotThrow(()->CaseRules.catalog("instruments","i1",i,user("j1"),catalog));}
 @Test void historicoDetectaVariacion(){assertTrue(CaseRules.significant(fixture.path("protocols").get(2),work));}
 @Test void formalizadoInmutable(){var old=work.stream().filter(r->r.id().equals("p1")).findFirst().orElseThrow();var p=old.payload().deepCopy();((ObjectNode)p).put("state","ANULADO");assertThrows(IllegalArgumentException.class,()->CaseRules.work("protocols","p1",p,user("e1"),old,catalog,work));}
 @Test void receptorRutInvalido(){var old=work.stream().filter(r->r.id().equals("v3")).findFirst().orElseThrow();var v=old.payload().deepCopy();((ObjectNode)v.path("receipt")).put("rut","14.226.791-9");assertThrows(IllegalArgumentException.class,()->CaseRules.work("visits","v3",v,user("t1"),old,catalog,work));}
 @Test void revisionSecuencial(){JsonNode p=fixture.path("protocols").get(2).deepCopy();var old=new CaseRecord("protocols","p3",1,p);assertThrows(IllegalArgumentException.class,()->CaseRules.identity("p3",p,old));((ObjectNode)p).put("revision",2);assertDoesNotThrow(()->CaseRules.identity("p3",p,old));}
}
