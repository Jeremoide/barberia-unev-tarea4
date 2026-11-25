#!/usr/bin/env python3
"""Prepara una base de demostración independiente con Docker o Podman."""
import argparse, base64, json, os, secrets, shutil, subprocess, time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
a=argparse.ArgumentParser();a.add_argument('--profile',default='demo');a.add_argument('--engine',choices=['docker','podman']);a.add_argument('--port',type=int,default=3313);a.add_argument('--name',default='barberia-3-demo');o=a.parse_args()
engine=o.engine or next((e for e in ['docker','podman'] if shutil.which(e)),None)
if not engine: raise SystemExit('Instale e inicie Docker Desktop/Engine o Podman. Después repita este comando.')
def run(args):
 r=subprocess.run([engine,*args],capture_output=True,text=True)
 if r.returncode: raise SystemExit('El motor de contenedores no completó la operación. Revise que esté iniciado y que haya conexión a Internet. '+r.stderr[-500:])
 return r.stdout
if not 1024<=o.port<=65535: raise SystemExit('Puerto inválido.')
if not o.name.replace('-','').replace('_','').isalnum(): raise SystemExit('Nombre de contenedor inválido.')
if not o.profile.isalnum():raise SystemExit('Perfil inválido.')
state=ROOT/('.demo' if o.profile=='demo' else '.demo-'+o.profile);state.mkdir(exist_ok=True);os.chmod(state,0o700)
env=state/'database.env';config=ROOT/('config.'+o.profile+'.properties')
if not env.exists():
 if config.exists(): raise SystemExit('Existe configuración sin su estado asociado. Use otra carpeta; no se sobrescribió.')
 password=secrets.token_hex(24)
 with env.open('x') as f:f.write('MARIADB_DATABASE=barberia_demo\nMARIADB_USER=barberia_demo\nMARIADB_PASSWORD='+password+'\nMARIADB_ROOT_PASSWORD='+secrets.token_hex(24)+'\n')
 os.chmod(env,0o600)
 with config.open('x') as f:f.write('db.url=jdbc:mariadb://127.0.0.1:'+str(o.port)+'/barberia_demo?sslMode=disable\ndb.user=barberia_demo\ndb.password='+password+'\nsecurity.key='+base64.b64encode(secrets.token_bytes(32)).decode()+'\n')
 os.chmod(config,0o600)
else:
 if not config.exists(): raise SystemExit('Falta config.demo.properties. No se modificaron las credenciales existentes.')
 if f'127.0.0.1:{o.port}/' not in config.read_text(): raise SystemExit('El puerto no coincide con la configuración existente. Repita usando el puerto original.')
init=state/'init';init.mkdir(exist_ok=True)
for name in ['01_schema.sql','02_demo.sql','03_v3.sql','04_demo_v3.sql']:shutil.copyfile(ROOT/'database'/name,init/name)
r=subprocess.run([engine,'inspect',o.name],capture_output=True,text=True)
if r.returncode==0:
 info=json.loads(r.stdout)[0]
 if info.get('Config',{}).get('Labels',{}).get('edu.unev.demo')!='devops':raise SystemExit('El nombre ya lo usa otro contenedor. Elija --name diferente.')
 run(['start',o.name])
else:
 run(['run','-d','--name',o.name,'--label','edu.unev.demo=devops','--env-file',str(env),'-p',f'127.0.0.1:{o.port}:3306','-v',o.name+'-datos:/var/lib/mysql','-v',str(init)+':/docker-entrypoint-initdb.d:ro,Z','docker.io/library/mariadb:11.4'])
for _ in range(90):
 r=subprocess.run([engine,'exec',o.name,'healthcheck.sh','--connect','--innodb_initialized'],capture_output=True,text=True)
 if r.returncode==0:break
 time.sleep(2)
else:raise SystemExit('La base no terminó de iniciar. Revise el contenedor; no se borraron sus datos.')
print('Base de demostración lista. Configuración privada: '+config.name+'. Para la demo ejecute python scripts/abrir-demo.py')
print('Usuarios: admin.demo, recepcion.demo y barbero.demo. Consulte sus claves de práctica en LEEME_DEMO.txt; cada cuenta vincula su propio Authenticator.')
