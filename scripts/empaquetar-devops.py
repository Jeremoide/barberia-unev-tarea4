#!/usr/bin/env python3
"""Empaqueta únicamente archivos permitidos; excluye configuración privada."""
from pathlib import Path
import argparse, hashlib, zipfile
root=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--platform',required=True,choices=['linux','windows']);a=p.parse_args()
out=root/'dist';out.mkdir(exist_ok=True)
jar=root/'target/barberia-v3-3.0.0.jar'
items=[(jar,'barberia.jar')]+[(root/name,name) for name in ['docs/MANUAL_USUARIO.md','docs/MATRIZ_REQUISITOS.md','database/diagrama_er.mmd','database/er_agenda.svg','database/er_operaciones.svg','LEEME_DEMO.txt','README.md','config.example.properties','database/01_schema.sql','database/02_demo.sql','database/03_v3.sql','database/04_demo_v3.sql','scripts/respaldo.py','scripts/preparar-demo.py','scripts/abrir-demo.py']]
if any(not f.is_file() for f,_ in items):raise SystemExit('Faltan archivos de distribución.')
zip_path=out/f'Barberia-3.0-{a.platform}-x64.zip'
with zipfile.ZipFile(zip_path,'w',zipfile.ZIP_DEFLATED) as z:
 for src,name in items:z.write(src,'Barberia-3.0/'+name)
sha=hashlib.sha256(zip_path.read_bytes()).hexdigest()
zip_path.with_suffix('.zip.sha256').write_text(sha+'  '+zip_path.name+'\n')
print(zip_path)
