#!/usr/bin/env python3
"""Inicializa solo la instancia MariaDB de desarrollo de esta copia, sin sudo."""
from pathlib import Path
import subprocess,secrets,base64,os
root=Path(__file__).resolve().parents[1];os.chdir(root)
subprocess.run(['bash','scripts/iniciar-db-local.sh'],check=True)
socket=str(root/'.local-db/server.sock')
def sql(text,base=None):
 command=['mariadb','--socket='+socket]
 if base:command.append(base)
 return subprocess.run(command,input=text,text=True,check=True,capture_output=True).stdout
sql('CREATE DATABASE IF NOT EXISTS barberia_v2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;')
sql((root/'database/01_schema.sql').read_text(),'barberia_v2')
config=root/'config.properties'
if not config.exists():
 if sql("SELECT COUNT(*) FROM mysql.user WHERE User='barberia_v2_app' AND Host='127.0.0.1';").strip().splitlines()[-1]!='0':
  raise SystemExit('El usuario ya existe. Configure su clave existente sin restablecerla.')
 password=secrets.token_hex(24)
 sql("CREATE USER 'barberia_v2_app'@'127.0.0.1' IDENTIFIED BY '"+password+"'; GRANT SELECT,INSERT,UPDATE,DELETE ON barberia_v2.* TO 'barberia_v2_app'@'127.0.0.1';")
 fd=os.open(config,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
 with os.fdopen(fd,'w') as f:f.write('db.url=jdbc:mariadb://127.0.0.1:3308/barberia_v2?sslMode=disable\ndb.user=barberia_v2_app\ndb.password='+password+'\nsecurity.key='+base64.b64encode(secrets.token_bytes(32)).decode()+'\n')
if sql('SELECT COUNT(*) FROM usuarios;','barberia_v2').strip().splitlines()[-1]=='0':
 sql((root/'database/02_demo.sql').read_text(),'barberia_v2')
print('Base local preparada. La configuración privada no se mostró. Use las cuentas de prueba del PDF y vincule su Authenticator.')
