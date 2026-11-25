#!/usr/bin/env python3
"""Abre la distribución desde su propia carpeta sin rutas del autor."""
import argparse, shutil, subprocess
from pathlib import Path
root=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--check',action='store_true');a=p.parse_args()
if not shutil.which('java'):raise SystemExit('Instale Java 21 o superior y compruebe java -version.')
config=root/'config.demo.properties'
if not config.exists():raise SystemExit('Ejecute primero python scripts/preparar-demo.py.')
jar=root/'barberia.jar'
if not jar.exists():jar=root/'target/barberia-v2-2.0.0.jar'
if not jar.exists():raise SystemExit('Falta el JAR: descargue el ZIP de Releases o ejecute mvn package.')
cmd=['java','-Duser.timezone=America/Santo_Domingo','-Dbarberia.config='+str(config)]
cmd+=['-cp',str(jar),'edu.unev.barberia.tools.CheckConnection'] if a.check else ['-jar',str(jar)]
raise SystemExit(subprocess.call(cmd,cwd=root))
