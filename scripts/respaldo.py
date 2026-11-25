#!/usr/bin/env python3
"""Respaldo/restauración de la demo 3.0 sin exponer contraseñas en argumentos."""
import argparse, shutil, subprocess
from pathlib import Path
root=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('action',choices=['crear','restaurar']);p.add_argument('file',type=Path);p.add_argument('--engine',choices=['podman','docker']);p.add_argument('--name',default='barberia-3-demo');p.add_argument('--confirmar-restauracion',action='store_true');a=p.parse_args()
engine=a.engine or next((x for x in ['docker','podman'] if shutil.which(x)),None)
if not engine:raise SystemExit('No hay motor de contenedores.')
check=subprocess.run([engine,'inspect','--format','{{index .Config.Labels "edu.unev.demo"}}',a.name],capture_output=True,text=True)
if check.returncode or check.stdout.strip()!='devops':raise SystemExit('El destino no es una base demo autorizada; no se ejecutó ninguna operación.')
if a.action=='crear':
 a.file.parent.mkdir(parents=True,exist_ok=True)
 with a.file.open('xb') as f:
  a.file.chmod(0o600)
  r=subprocess.run([engine,'exec',a.name,'sh','-c','MYSQL_PWD="$MARIADB_ROOT_PASSWORD" exec mariadb-dump -uroot --single-transaction --routines --triggers "$MARIADB_DATABASE"'],stdout=f)
 if r.returncode:raise SystemExit('Falló el respaldo; no lo utilice.')
 print('Respaldo privado creado. Contiene datos personales; no lo suba a GitHub.')
else:
 if not a.confirmar_restauracion:raise SystemExit('La restauración reemplaza datos. Use una demo vacía y --confirmar-restauracion después de respaldarla.')
 with a.file.open('rb') as f:r=subprocess.run([engine,'exec','-i',a.name,'sh','-c','MYSQL_PWD="$MARIADB_ROOT_PASSWORD" exec mariadb -uroot "$MARIADB_DATABASE"'],stdin=f)
 if r.returncode:raise SystemExit('La restauración no terminó correctamente.')
 print('Restauración terminada. Verifique conteos y acceso antes de continuar.')
