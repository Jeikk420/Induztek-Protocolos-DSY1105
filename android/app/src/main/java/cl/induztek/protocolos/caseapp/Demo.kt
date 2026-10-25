package cl.induztek.protocolos.caseapp

import java.time.LocalDate
import java.time.ZoneId

object Demo {
 const val PASSWORD = "Induztek2026!"
 val users = listOf(
  User("t1","Matías Godoy Herrera","17.284.519-3",Roles.TECH,"mgodoy@induztek.ejemplo"),
  User("t2","Javiera Tapia Muñoz","19.412.087-7",Roles.TECH,"jtapia@induztek.ejemplo"),
  User("t3","Carlos Rondón Pérez","26.104.583-4",Roles.TECH,"crondon@induztek.ejemplo"),
  User("t4","Diego Valenzuela Ríos","20.137.648-3",Roles.TECH,"dvalenzuela@induztek.ejemplo"),
  User("e1","Andrea Contreras Silva","15.906.334-8",Roles.ENGINEER,"acontreras@induztek.ejemplo"),
  User("e2","Sebastián Morales Gutiérrez","16.553.270-8",Roles.ENGINEER,"smorales@induztek.ejemplo"),
  User("j1","Ricardo Espinoza Lagos","13.871.905-7",Roles.CHIEF,"respinoza@induztek.ejemplo"),
  User("c1","Marcela Fuentes Rojas","14.226.791-8",Roles.CLIENT,"mfuentes@envpacifico.ejemplo","cl1","Jefa de Mantenimiento"),
  User("c2","Tomás Leiva Araya","18.390.452-3",Roles.CLIENT,"tleiva@losaromos.ejemplo","cl4","Supervisor Eléctrico"),
  User("c3","Patricio Navarro Soto","12.645.308-6",Roles.CLIENT,"pnavarro@cerroalto.ejemplo","cl3","Jefe de Mantenimiento Eléctrico")
 )
 fun data(): Data {
  val clients=listOf(
   Client("cl1","Envasadora del Pacífico S.A.","76.482.915-8","Agroindustria","Marcela Fuentes Rojas","+56 9 0000 0101","mfuentes@envpacifico.ejemplo"),
   Client("cl2","Sanitaria Valle Central S.A.","96.731.540-0","Servicios sanitarios","Jorge Arancibia Leiva","+56 9 0000 0102","jarancibia@svcentral.ejemplo"),
   Client("cl3","Minera Cerro Alto SpA","77.105.362-9","Minería","Patricio Navarro Soto","+56 9 0000 0103","pnavarro@cerroalto.ejemplo"),
   Client("cl4","Clínica Los Aromos Ltda.","76.903.218-5","Salud","Carolina Méndez Pizarro","+56 9 0000 0104","cmendez@losaromos.ejemplo"),
   Client("cl5","Cementos Lomas Blancas S.A.","99.524.170-6","Construcción","Luis Bravo Castillo","+56 9 0000 0105","lbravo@lomasblancas.ejemplo"),
   Client("cl6","Data Center Andes Norte SpA","78.214.635-1","Tecnología","Felipe Órdenes Vera","+56 9 0000 0106","fordenes@andesnorte.ejemplo")
  )
  val installs=listOf(
   Installation("in1","cl1","Planta Lampa","Sector industrial","Lampa",access="Inducción obligatoria de 30 min"),
   Installation("in2","cl2","Planta Elevadora Sur","Sector rural poniente","San Bernardo",access="Sala eléctrica subterránea, sin señal"),
   Installation("in3","cl3","Faena Til Til","Acceso por Ruta 5 Norte","Til Til",access="Sin señal en toda la faena"),
   Installation("in4","cl4","Edificio Clínico","Zona urbana","La Florida",access="Cortes domingos 06:00–10:00"),
   Installation("in5","cl5","Planta Molienda","Sector precordillera","Puente Alto",access="Uso obligatorio de respirador"),
   Installation("in6","cl6","Site Huechuraba","Parque empresarial","Huechuraba",access="Acompañamiento permanente del cliente")
  )
  val equipment=listOf(
   Equipment("eq1","in1","TR-01","TRANSFORMADOR","Rhona","Demo 1500","RH-18733","2012","1500","12","0.4"),
   Equipment("eq2","in1","TR-02","TRANSFORMADOR","Rhona","Demo 1000","RH-21904","2017","1000","12","0.4"),
   Equipment("eq3","in2","TR-A","TRANSFORMADOR","Tusan","Demo 2500","TS-5521","2009","2500","23","0.4"),
   Equipment("eq4","in4","CEL-MT-03","CELDA","Ormazabal","Demo MT","OR-77812","2018",highVoltage="12",current="630",contactMax="60"),
   Equipment("eq5","in3","INT-52-1","INTERRUPTOR","Schneider","Demo MT","SE-449021","2015",highVoltage="23",current="1250",contactMax="45"),
   Equipment("eq6","in5","REL-50/51-A","RELE","SEL","751","SEL-751-30219","2020",highVoltage="0.12",current="5"),
   Equipment("eq7","in6","CAB-MT-02","CABLE","Nexans","Demo 3×240","NX-2201-B","2019",highVoltage="15"),
   Equipment("eq8","in3","TR-SS-01","TRANSFORMADOR","ABB","Demo 5000","ABB-1180442","2011","5000","23","6.6")
  )
  val instruments=listOf(
   Instrument("i1","Megóhmetro 5 kV","AISLACION","Megger","MIT525","MIT-0045821","2026-03-15","2027-03-15"),
   Instrument("i2","Megóhmetro 10 kV","AISLACION","Megger","MIT1025","MIT-0071233","2025-10-10","2026-10-10"),
   Instrument("i3","Medidor TTR","TTR","Megger","TTR300","TTR-30044","2026-06-02","2027-06-02"),
   Instrument("i4","Micro-ohmímetro vencido","CONTACTOS","Megger","DLRO10X","DLR-10882","2025-08-20","2026-08-20"),
   Instrument("i5","Inyección secundaria","RELE","Omicron","CMC 356","CMC-HB411","2026-01-05","2027-01-05"),
   Instrument("i6","Medidor de devanados demo","DEVANADOS","Demo","D-01","DEMO-DEV-01","2026-01-01","2027-12-31"),
   Instrument("i7","Micro-ohmímetro demo vigente","CONTACTOS","Demo","C-01","DEMO-CON-01","2026-01-01","2027-12-31")
  )
  fun time(date:String) = LocalDate.parse(date).atTime(10,0).atZone(ZoneId.of("America/Santiago")).toInstant().toEpochMilli()
  val visits=mutableListOf<Visit>(); val protocols=mutableListOf<Protocol>()
  for((index,day) in listOf("2024-09-14","2025-09-13","2026-09-26").withIndex()) {
   val v=Visit("v${index+1}","cl1","in1",day,if(index==2) "OT-2026-1187" else "OT-HIST-${index+1}","e1",listOf(if(index==1) "t2" else "t1"),closedAt=time(day)+3600000,receipt=Receipt("Marcela Fuentes Rojas","14.226.791-8","Jefa de Mantenimiento",time(day)+3600000))
   visits.add(v)
   val one=listOf("12500","9800","6100")[index]; val ten=listOf("31250","21560","10980")[index]
   val tests=Rules.newTests("TRANSFORMADOR").map { t -> if(t.type=="AISLACION") t.copy(instrumentId="i1",values=t.values.map { it.copy(text=if(it.key.endsWith("_1")) one else if(it.key.endsWith("_10")) ten else "5") }) else t.copy(notPerformed=true,reason="No realizada en el historial sintético de ejemplo.") }
   val state=if(index<2) States.FINAL else States.REGISTERED; val u=users.first { it.id==v.technicianIds.first() }
   protocols.add(Protocol("p${index+1}",listOf("PT-2024-0312","PT-2025-0288","PT-2026-0401")[index],v.id,"eq1",u.id,u.name,time(day),listOf("18","21","19")[index],listOf("55","48","60")[index],state,if(index<2) "APROBADO" else "OBSERVADO",tests,notes="Historial ficticio del Anexo 9.",formalizedBy=if(index<2) users[4].name else "",formalizedAt=if(index<2) time(day)+7200000 else null,changes=listOf(Change(u.id,u.name,time(day),"NUEVO",state,"Carga del historial ficticio"))))
  }
  
  return Data(clients,installs,equipment,users,instruments,visits.map { it.copy(actaJson=com.google.gson.Gson().toJson(protocols.filter { p -> p.visitId==it.id })) },protocols)
 }
}
