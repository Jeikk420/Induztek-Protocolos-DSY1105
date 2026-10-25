package cl.induztek.protocolos.caseapp

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.*

class RulesTest {
 private val d=Demo.data()
 private val tr=d.equipment.first { it.id=="eq1" }
 private val day=LocalDate.of(2026,10,8).atTime(10,0).atZone(ZoneId.of("America/Santiago")).toInstant().toEpochMilli()
 private fun insulation(one:String="600",ten:String="1200",unit:String="MΩ")=Rules.newTests("TRANSFORMADOR").first().copy(instrumentId="i1",values=Rules.readings("TRANSFORMADOR","AISLACION").map { it.copy(text=if(it.key.endsWith("_1"))one else if(it.key.endsWith("_10"))ten else "5",unit=if(it.key.endsWith("_kv"))"kV" else unit) })
 private fun ttr(measured:String)=Test("TTR",values=Rules.readings("TRANSFORMADOR","TTR").map { it.copy(text=if(it.key.endsWith("_theory"))"100" else if(it.key=="tap")"1" else measured) })
 private fun relay(time:String,operates:Boolean=true)=Test("RELE",relayOperates=operates,values=Rules.readings("RELE","RELE").map { it.copy(text=when(it.key){"expected"->"100";"time"->time;else->"5"}) })
 @Test fun rutConPuntosYMinuscula(){assertTrue(Rules.rut("17.284.519-3"));assertTrue(Rules.rut("172845193"));assertFalse(Rules.rut("17.284.519-4"));assertFalse(Rules.rut("00.000.000-0"))}
 @Test fun comaDecimalYValoresNoFinitos(){assertEquals(610.5,Rules.number("610,5")!!,0.0);for(v in listOf("NaN","Infinity","1e999","abc",""))assertNull(Rules.number(v))}
 @Test fun correoInvalido(){assertFalse(Rules.email("alumno@"));assertFalse(Rules.email("alumno correo@dominio.cl"));assertTrue(Rules.email("alumno@dominio.cl"))}
 @Test fun tagUnicoPorInstalacion(){assertTrue(Rules.equipment(tr.copy(id="nuevo",tag="tr-01"),d).containsKey("tag"));assertFalse(Rules.equipment(tr.copy(id="nuevo",installationId="in2"),d).containsKey("tag"))}
 @Test fun rutClienteNoDuplicado(){assertTrue(Rules.client(d.clients.first().copy(id="nuevo"),d).containsKey("rut"))}
 @Test fun placaObligatoriaSegunTipo(){assertTrue(Rules.equipment(tr.copy(power=""),d).containsKey("power"));assertFalse(Rules.equipment(d.equipment.first{it.type=="CABLE"},d).containsKey("power"))}
 @Test fun fechaFuturaBloqueada(){assertTrue(Rules.visit(Visit(clientId="cl1",installationId="in1",date=LocalDate.now().plusDays(1).toString()),d).containsKey("date"))}
 @Test fun aislamientoUmbral100(){assertEquals("RECHAZADO",Rules.testResult(insulation("99","500"),tr));assertEquals("APROBADO",Rules.testResult(insulation("100","200"),tr))}
 @Test fun indicePolarizacionLimites(){assertEquals("APROBADO",Rules.testResult(insulation("600","1200"),tr));assertEquals("OBSERVADO",Rules.testResult(insulation("600","600"),tr));assertEquals("RECHAZADO",Rules.testResult(insulation("600","599"),tr))}
 @Test fun gigaOhmsNormalizados(){assertEquals(Rules.testResult(insulation(),tr),Rules.testResult(insulation("0.6","1.2","GΩ"),tr));assertEquals(600.0,Rules.ohms(Reading("r","R","GΩ","0.6"))!!,0.0)}
 @Test fun contactoMaximoPlaca(){val eq=d.equipment.first{it.id=="eq4"};fun test(v:String)=Test("CONTACTOS",values=Rules.readings("CELDA","CONTACTOS").map{it.copy(text=v)})
  assertEquals("APROBADO",Rules.testResult(test("60"),eq));assertEquals("OBSERVADO",Rules.testResult(test("72"),eq));assertEquals("RECHAZADO",Rules.testResult(test("72.1"),eq))}
 @Test fun ttrMedioYUnoPorCiento(){assertEquals("APROBADO",Rules.testResult(ttr("100.5"),tr));assertEquals("OBSERVADO",Rules.testResult(ttr("101"),tr));assertEquals("RECHAZADO",Rules.testResult(ttr("101.01"),tr))}
 @Test fun releCincoDiezYNoOpera(){val eq=d.equipment.first{it.type=="RELE"};assertEquals("APROBADO",Rules.testResult(relay("105"),eq));assertEquals("OBSERVADO",Rules.testResult(relay("110"),eq));assertEquals("RECHAZADO",Rules.testResult(relay("111"),eq));assertEquals("RECHAZADO",Rules.testResult(relay("",false),eq))}
 @Test fun devanadosEsDecisionManual(){assertEquals("OBSERVADO",Rules.testResult(Test("DEVANADOS"),tr));assertEquals("APROBADO",Rules.testResult(Test("DEVANADOS",manualResult="APROBADO"),tr))}
 @Test fun resultadoGlobalPeorPrueba(){assertEquals("RECHAZADO",Rules.worst(listOf("APROBADO","OBSERVADO","RECHAZADO")));assertEquals("OBSERVADO",Rules.worst(listOf("APROBADO","OBSERVADO")))}
 @Test fun instrumentoVencidoBloqueaRegistro(){val eq=d.equipment.first{it.id=="eq4"};val t=Test("CONTACTOS","i4",Rules.readings("CELDA","CONTACTOS").map{it.copy(text="50")});assertTrue(Rules.test(t,eq,d.instruments,day).containsKey("instrumentId"))}
 @Test fun calibracionCubreFecha(){assertTrue(Rules.test(insulation(),tr,d.instruments,day).isEmpty());val old=LocalDate.of(2025,1,1).atStartOfDay(ZoneId.of("America/Santiago")).toInstant().toEpochMilli();assertTrue(Rules.test(insulation(),tr,d.instruments,old).containsKey("instrumentId"))}
 @Test fun noRealizadaExigeMotivo(){assertTrue(Rules.test(Test("TTR",notPerformed=true),tr,d.instruments,day).containsKey("reason"));assertTrue(Rules.test(Test("TTR",notPerformed=true,reason="Sin acceso al equipo"),tr,d.instruments,day).isEmpty())}
 @Test fun lecturaNegativaYUnidadInvalida(){val t=insulation("-1");assertTrue(Rules.test(t,tr,d.instruments,day).containsKey("AT-BT_1"));assertTrue(Rules.test(insulation(unit="Ω"),tr,d.instruments,day).containsKey("values"))}
 @Test fun humedadFueraDeRango(){val p=d.protocols.last().copy(humidity="101");assertTrue(Rules.protocol(p,d,true).containsKey("humidity"))}
 @Test fun todasNoRealizadasNoRegistran(){val p=d.protocols.last().copy(tests=Rules.newTests(tr.type).map{it.copy(notPerformed=true,reason="Sin acceso")});assertTrue(Rules.protocol(p,d,true).containsKey("tests"))}
 @Test fun numeroTapDebeSerEntero(){val t=ttr("100").copy(instrumentId="i3",values=ttr("100").values.map{if(it.key=="tap")it.copy(text="1.5") else it});assertTrue(Rules.test(t,tr,d.instruments,day).containsKey("tap"))}
 @Test fun mantenimientoConservaPrevias(){val t=insulation().copy(afterMaintenance=true);assertTrue(Rules.test(t,tr,d.instruments,day).containsKey("previousValues"));assertTrue(Rules.test(t.copy(previousValues=insulation().values),tr,d.instruments,day).isEmpty())}
 @Test fun variacionMayor25YDivisionPorCero(){assertFalse(Variation("R",100.0,125.0,25.0).significant);assertTrue(Variation("R",100.0,126.0,26.0).significant);val old=d.protocols.first().copy(id="old",testedAt=day-1000,tests=listOf(insulation("0","0")));val p=old.copy(id="new",testedAt=day,tests=listOf(insulation()));val a=Rules.variations(p,d.copy(protocols=listOf(old,p)));assertTrue(a.isNotEmpty());assertTrue(a.all{it.percent==null})}
 @Test fun historicoAnexoMarcaVariacion(){assertTrue(Rules.variations(d.protocols.last(),d).any{it.significant})}
 @Test fun tecnicoSoloEditaPropios(){val t=d.users.first{it.id=="t1"};val p=d.protocols.last().copy(state=States.DRAFT);assertTrue(Rules.canEdit(t,p));assertFalse(Rules.canEdit(t.copy(id="t2"),p));assertFalse(Rules.canEdit(t,p.copy(state=States.FINAL)))}
 @Test fun clienteSoloSuEmpresaYLectura(){val u=d.users.first{it.id=="c1"};val v=Rules.visible(u,d);assertFalse(Rules.canCreate(u));assertFalse(Rules.canReview(u));assertTrue(v.clients.all{it.id=="cl1"});assertTrue(v.equipment.all{it.installationId=="in1"});assertTrue(v.protocols.none{it.state in listOf(States.DRAFT,States.VOID)})}
 @Test fun receptorValidaRutYCargo(){assertTrue(Rules.receipt(Receipt("Marcela","14.226.791-8","Jefa")).isEmpty());assertTrue(Rules.receipt(Receipt("Marcela","14.226.791-9","")).size>=2)}
 @Test fun plazoSaltaFinDeSemana(){val friday=LocalDate.of(2026,10,9).atStartOfDay(ZoneId.of("America/Santiago")).toInstant().toEpochMilli();val p=d.protocols.last().copy(testedAt=friday);assertFalse(Rules.late(p,LocalDate.of(2026,10,13)));assertTrue(Rules.late(p,LocalDate.of(2026,10,14)))}
 @Test fun datosDemoCoherentesYExportables(){assertEquals(10,d.users.size);assertEquals(5,d.equipment.map{it.type}.distinct().size);assertTrue(d.users.all{Rules.rut(it.rut)});assertTrue(d.clients.all{Rules.rut(it.rut)});val json=Gson().toJson(d);assertEquals(d,Gson().fromJson(json,Data::class.java));File("build/test-evidence").mkdirs();File("build/test-evidence/case-demo.json").writeText(json)}
}
