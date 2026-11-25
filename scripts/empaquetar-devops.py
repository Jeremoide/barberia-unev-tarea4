#!/usr/bin/env python3
"""Empaqueta únicamente archivos permitidos; excluye configuración privada."""
from pathlib import Path
import argparse, hashlib, zipfile
root=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--platform',required=True,choices=['linux','windows']);a=p.parse_args()
out=root/'dist';out.mkdir(exist_ok=True)
jar=root/'target/barberia-v2-2.0.0.jar'
items=[(jar,'barberia.jar')]+[(root/name,name) for name in ['LEEME_DEMO.txt','README.md','config.example.properties','database/01_schema.sql','database/02_demo.sql','scripts/preparar-demo.py','scripts/abrir-demo.py']]
if any(not f.is_file() for f,_ in items):raise SystemExit('Faltan archivos de distribución.')
zip_path=out/f'Barberia-DevOps-1.0-{a.platform}-x64.zip'
with zipfile.ZipFile(zip_path,'w',zipfile.ZIP_DEFLATED) as z:
 for src,name in items:z.write(src,'Barberia-DevOps/'+name)
sha=hashlib.sha256(zip_path.read_bytes()).hexdigest()
zip_path.with_suffix('.zip.sha256').write_text(sha+'  '+zip_path.name+'\n')
print(zip_path)
