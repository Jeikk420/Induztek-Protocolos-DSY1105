import argparse, base64, copy, datetime, json, urllib.request, urllib.error, uuid
from pathlib import Path
from zoneinfo import ZoneInfo
parser=argparse.ArgumentParser();parser.add_argument('--equipos',default='http://localhost:8081');parser.add_argument('--protocolos',default='http://localhost:8082');parser.add_argument('--output',default='evidencias/backend_integracion_real.json');args=parser.parse_args()
open_url=urllib.request.build_opener(urllib.request.ProxyHandler({})).open
report=[]
def call(base,path,user='t1',method='GET',body=None):
 auth=base64.b64encode((user+':Induztek2026!').encode()).decode()
 req=urllib.request.Request(base+path,data=None if body is None else json.dumps(body,ensure_ascii=False).encode(),method=method,headers={'Authorization':'Basic '+auth,'Content-Type':'application/json'})
 try:
  with open_url(req,timeout=15) as response:return response.status,json.load(response)
 except urllib.error.HTTPError as e:
  try:data=json.load(e)
  except Exception:data={}
  return e.code,data

def check(name,base,path,user='t1',method='GET',body=None,expected=200):
 status,result=call(base,path,user,method,body);report.append(dict(prueba=name,status=status,esperado=expected,ok=status==expected));assert status==expected,(name,status,result);return result
check('Servicio de equipos disponible',args.equipos,'/actuator/health')
check('Servicio de protocolos disponible',args.protocolos,'/actuator/health')
catalog=check('Técnico consulta los catálogos',args.equipos,'/api/v2/records')
scoped=check('Cliente solo consulta su empresa',args.equipos,'/api/v2/records','c1')
assert [r['id'] for r in scoped if r['kind']=='clients']==['cl1']
assert all(r['kind']!='instruments' for r in scoped)
check('Cuenta inexistente no ingresa',args.equipos,'/api/v2/records','inexistente',expected=401)
check('Contraseña incorrecta no ingresa',args.equipos,'/api/v2/records','t1:incorrecta',expected=401)
check('Protocolos exige cuenta activa',args.protocolos,'/api/v2/records','inexistente',expected=401)
by_id={(r['kind'],r['id']):r['payload'] for r in catalog}
client=by_id['clients','cl1']
check('Cliente no puede escribir',args.equipos,'/api/v2/records/clients/cl1','c1','PUT',client,403)
check('Técnico no administra calibraciones',args.equipos,'/api/v2/records/instruments/i1','t1','PUT',by_id['instruments','i1'],403)
uid=str(uuid.uuid4());zone=ZoneInfo('America/Santiago');now=datetime.datetime.now(zone);ms=int(now.timestamp()*1000)
visit=dict(id=uid,clientId='cl1',installationId='in1',date=now.date().isoformat(),ot='OT-PRUEBA-'+uid[:8],engineerId='e1',technicianIds=['t1'],kind='Solo pruebas',revision=1,actaJson='')
invalid_visit=copy.deepcopy(visit);invalid_visit['clientId']='cl2'
check('Visita rechaza instalación de otro cliente',args.protocolos,'/api/v2/records/visits/'+uid,method='PUT',body=invalid_visit,expected=400)
check('Abrir visita',args.protocolos,'/api/v2/records/visits/'+uid,method='PUT',body=visit)
proto=copy.deepcopy(json.loads((Path(__file__).parents[1]/'backend/ms-protocolos/src/main/resources/case-demo.json').read_text())['protocols'][2]);pid=str(uuid.uuid4())
proto.update(id=pid,number='PT-'+pid.upper(),visitId=uid,testedAt=ms,state='BORRADOR',revision=1,result='OBSERVADO',formalizedBy='',historicalComment='',replacesId='',reason='')
proto.pop('formalizedAt',None);proto['changes']=[dict(userId='t1',userName=by_id['users','t1']['name'],at=ms,**{'from':'NUEVO'},to='BORRADOR',reason='')]
check('Guardar borrador',args.protocolos,'/api/v2/records/protocols/'+pid,method='PUT',body=proto)
private=check('Cliente consulta sin recibir borradores',args.protocolos,'/api/v2/records','c1')
assert not any(r['id'] in [pid,uid] for r in private)
check('Cliente no modifica protocolos',args.protocolos,'/api/v2/records/protocols/'+pid,'c1','PUT',proto,403)
check('Reintento idéntico sin duplicados',args.protocolos,'/api/v2/records/protocols/'+pid,method='PUT',body=proto)
conflict=copy.deepcopy(proto);conflict['notes']='Cambio con revisión antigua'
check('Conflicto conserva el servidor',args.protocolos,'/api/v2/records/protocols/'+pid,method='PUT',body=conflict,expected=409)
def next_state(user,state,reason=''):
 global proto
 old=proto['state'];proto['revision']+=1;proto['state']=state;proto['reason']=reason;proto['changes'].append(dict(userId=user,userName=by_id['users',user]['name'],at=ms,**{'from':old},to=state,reason=reason))
next_state('t1','REGISTRADO')
check('Otro técnico no registra un protocolo ajeno',args.protocolos,'/api/v2/records/protocols/'+pid,'t2','PUT',proto,400)
bad=copy.deepcopy(proto);bad['tests']='formato incorrecto'
check('Rechaza estructura de mediciones inválida',args.protocolos,'/api/v2/records/protocols/'+pid,method='PUT',body=bad,expected=400)
bad=copy.deepcopy(proto);bad['equipmentId']='equipo-inexistente'
check('No permite cambiar identidad del equipo',args.protocolos,'/api/v2/records/protocols/'+pid,method='PUT',body=bad,expected=400)
bad=copy.deepcopy(proto);bad['humidity']='101'
check('Humedad fuera de rango bloqueada',args.protocolos,'/api/v2/records/protocols/'+pid,method='PUT',body=bad,expected=400)
check('Registrar pruebas completas',args.protocolos,'/api/v2/records/protocols/'+pid,method='PUT',body=proto)
early=copy.deepcopy(proto);early.update(revision=proto['revision']+1,state='FORMALIZADO',formalizedBy=by_id['users','e1']['name'],formalizedAt=ms)
early['changes'].append(dict(userId='e1',userName=by_id['users','e1']['name'],at=ms,**{'from':'REGISTRADO'},to='FORMALIZADO',reason=''))
check('No formaliza antes del cierre de visita',args.protocolos,'/api/v2/records/protocols/'+pid,'e1','PUT',early,400)
visit.update(revision=2,closedAt=ms,receipt=dict(name='Marcela Fuentes Rojas',rut='14.226.791-8',cargo='Jefa de Mantenimiento',receivedAt=ms),actaJson=json.dumps([proto],ensure_ascii=False,separators=(',',':')))
bad_receipt=copy.deepcopy(visit);bad_receipt['receipt']['rut']='14.226.791-9'
check('Receptor con RUT inválido no cierra visita',args.protocolos,'/api/v2/records/visits/'+uid,method='PUT',body=bad_receipt,expected=400)
check('Cerrar visita y conservar acta',args.protocolos,'/api/v2/records/visits/'+uid,method='PUT',body=visit)
next_state('e1','REGISTRADO');proto['historicalComment']='Variación significativa revisada con historial; requiere seguimiento.'
check('Guardar revisión de ingeniería',args.protocolos,'/api/v2/records/protocols/'+pid,'e1','PUT',proto)
next_state('e1','FORMALIZADO');proto['formalizedBy']=by_id['users','e1']['name'];proto['formalizedAt']=ms
check('Formalizar protocolo después del cierre',args.protocolos,'/api/v2/records/protocols/'+pid,'e1','PUT',proto)
next_state('e1','ANULADO','Intento de modificar original')
check('Protocolo formalizado inmutable',args.protocolos,'/api/v2/records/protocols/'+pid,'e1','PUT',proto,400)
client_work=check('Cliente consulta sus actas y protocolos',args.protocolos,'/api/v2/records','c1')
assert any(r['id']==pid and r['payload']['state']=='FORMALIZADO' for r in client_work)
assert all(r['payload'].get('clientId')=='cl1' for r in client_work if r['kind']=='visits')
assert all(r['payload']['state'] not in ['BORRADOR','ANULADO'] for r in client_work if r['kind']=='protocols')
other_client=check('Otro cliente no ve el protocolo',args.protocolos,'/api/v2/records','c2')
assert not any(r['id'] in [uid,pid] for r in other_client)
closed=next(r['payload'] for r in client_work if r['id']==uid)
assert json.loads(closed['actaJson'])[0]['state']=='REGISTRADO'
output=Path(args.output);output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(dict(fecha=now.isoformat(),pruebas=report,resultado='APROBADO'),ensure_ascii=False,indent=2));print(f'{len(report)} comprobaciones HTTP aprobadas.')
