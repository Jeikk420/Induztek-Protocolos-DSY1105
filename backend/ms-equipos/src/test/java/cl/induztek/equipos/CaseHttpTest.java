package cl.induztek.equipos;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Base64;
import static org.hamcrest.Matchers.*;
import java.nio.charset.StandardCharsets;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:equip-test;DB_CLOSE_DELAY=-1"}) @AutoConfigureMockMvc
class CaseHttpTest {
 @Autowired MockMvc http;@Autowired CaseStore store;
 String auth(String user){return "Basic "+Base64.getEncoder().encodeToString((user+":"+CaseRules.PASSWORD).getBytes(StandardCharsets.UTF_8));}
 @Test void exigeCuentaActiva()throws Exception{http.perform(get("/api/v2/records")).andExpect(status().isUnauthorized());http.perform(get("/api/v2/records").header("Authorization",auth("inventado"))).andExpect(status().isUnauthorized());}
 @Test void tecnicoConsultaCatalogos()throws Exception{http.perform(get("/api/v2/records").header("Authorization",auth("t1"))).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.kind == 'users')].id").isArray());}
 @Test void clienteFiltrado()throws Exception{http.perform(get("/api/v2/records").header("Authorization",auth("c1"))).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.kind == 'clients')].id").value(contains("cl1"))).andExpect(jsonPath("$[?(@.kind == 'clients')].id").value(hasSize(1))).andExpect(jsonPath("$[?(@.kind == 'instruments')].id").value(empty()));}
 @Test void clienteNoEscribe()throws Exception{var p=store.get("clients","cl1").payload();http.perform(put("/api/v2/records/clients/cl1").header("Authorization",auth("c1")).contentType("application/json").content(p.toString())).andExpect(status().isForbidden());}
 @Test void tecnicoNoAdministraInstrumentos()throws Exception{http.perform(put("/api/v2/records/instruments/i1").header("Authorization",auth("t1")).contentType("application/json").content(store.get("instruments","i1").payload().toString())).andExpect(status().isForbidden());}
 @Test void envioRepetidoEsIdempotente()throws Exception{String body=store.get("clients","cl1").payload().toString();for(int i=0;i<2;i++)http.perform(put("/api/v2/records/clients/cl1").header("Authorization",auth("t1")).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1));}
 @Test void conflictoNoSobrescribe()throws Exception{String body=store.get("clients","cl1").payload().toString().replace("Envasadora del Pacífico S.A.","Otro nombre");http.perform(put("/api/v2/records/clients/cl1").header("Authorization",auth("t1")).contentType("application/json").content(body)).andExpect(status().isConflict());}
}
