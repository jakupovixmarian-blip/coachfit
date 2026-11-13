#!/usr/bin/env python3
"""Verifica che il manifest di audit copra tutte le dipendenze dichiarate nel catalogo Gradle.

Confronta le coordinate groupId:artifactId presenti in gradle/libs.versions.toml con quelle
elencate in tools/dependency-audit/pom.xml. Non risolve le transitive (per quello serve Gradle):
serve solo a garantire che nessuna dipendenza DICHIARATA resti fuori dal controllo advisory.

Uso: python3 tools/dependency-audit/coverage_check.py
Exit code: 0 se la copertura e' completa, 1 se mancano coordinate.
"""
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[2]
TOML_PATH = ROOT / "gradle" / "libs.versions.toml"
POM_PATH = ROOT / "tools" / "dependency-audit" / "pom.xml"
NS = {"m": "http://maven.apache.org/POM/4.0.0"}

MODULE_RE = re.compile(r'module\s*=\s*"([^"]+)"')
STRING_RE = re.compile(r'=\s*"([A-Za-z0-9_.\-]+:[A-Za-z0-9_.\-]+(?::[^"]*)?)"')


def catalog_coordinates(text):
    """Coordinate groupId:artifactId dichiarate nel catalogo delle versioni."""
    section = text.split("[libraries]", 1)[1] if "[libraries]" in text else ""
    section = section.split("\n[", 1)[0]
    found = {}
    for line in section.splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key = line.split("=", 1)[0].strip()
        match = MODULE_RE.search(line) or STRING_RE.search(line)
        if not match:
            continue
        parts = match.group(1).split(":")
        found[key] = ":".join(parts[:2])
    return found


def pom_coordinates(path):
    """Coordinate groupId:artifactId elencate nel manifest."""
    root = ET.parse(path).getroot()
    coords = set()
    for dep in root.findall(".//m:dependencies/m:dependency", NS):
        group = dep.find("m:groupId", NS)
        artifact = dep.find("m:artifactId", NS)
        if group is not None and artifact is not None:
            coords.add(f"{group.text.strip()}:{artifact.text.strip()}")
    return coords


def main():
    catalog = catalog_coordinates(TOML_PATH.read_text())
    covered = pom_coordinates(POM_PATH)
    missing = sorted({coord for coord in catalog.values() if coord not in covered})

    print(f"voci nel catalogo [libraries]: {len(catalog)}")
    print(f"coordinate uniche nel manifest: {len(covered)}")
    print(f"dipendenze dichiarate NON coperte: {len(missing)}")
    for coord in missing:
        keys = sorted(k for k, v in catalog.items() if v == coord)
        print(f"  MANCA: {coord}  (catalogo: {', '.join(keys)})")
    if missing:
        print("\nAggiungi queste coordinate a tools/dependency-audit/pom.xml e rilancia il controllo.")
        return 1
    print("\nCopertura completa delle dipendenze dichiarate (le transitive restano fuori: serve Gradle).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
