#!/usr/bin/env python3
"""Porta nel manifest di audit le versioni REALMENTE risolte da Gradle.

Legge uno dei due file prodotti da Gradle in locale:

  app/gradle.lockfile   (dependency locking: group:artifact:version=configurazioni)
  deps.txt              (report `./gradlew :app:dependencies --configuration ...`)

e lo confronta con le coordinate dichiarate in tools/dependency-audit/pom.xml.
Serve a chiudere la lacuna transitiva: lo scanner di AppJet non risolve il grafo
Gradle, quindi le versioni vanno lette dal file risolto e riportate nel manifest.

Uso:
    python3 tools/dependency-audit/import_resolved.py deps.txt
    python3 tools/dependency-audit/import_resolved.py app/gradle.lockfile --out report.json

Exit code:
    0  manifest allineato: nessuna versione diversa e nessuna coordinata runtime assente
    1  disallineamenti da correggere (versioni diverse o coordinate runtime assenti dal grafo)
    2  file illeggibile o nessuna coordinata riconosciuta (esito vuoto: non e' una copertura)
"""
import argparse
import json
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[2]
POM_PATH = ROOT / "tools" / "dependency-audit" / "pom.xml"
NS = {"m": "http://maven.apache.org/POM/4.0.0"}

# Riga di lockfile: group:artifact:version=config1,config2
LOCK_RE = re.compile(r"^([A-Za-z0-9_.\-]+):([A-Za-z0-9_.\-]+):([^=\s]+)\s*=")
# Nodo dell'albero del report: prefisso +--- / \--- / |    seguito dalla coordinata
TREE_RE = re.compile(
    r"(?:\+---|\\---|\|)\s+(?P<coord>[A-Za-z0-9_.\-]+:[A-Za-z0-9_.\-]+:[A-Za-z0-9_.\-+]+)"
    r"(?P<rest>.*)$"
)
RESOLVED_ARROW_RE = re.compile(r"->\s*([A-Za-z0-9_.\-+]+)")
SKIP_MARKERS = ("(c)", "FAILED", "project :", "constraint")
# Dipendenze dichiarate che per natura non entrano nel grafo runtime dell'app:
# processori di annotazioni (ksp/kapt) e librerie usate solo dai test.
TEST_GROUP_PREFIXES = ("androidx.test",)
# Gruppi che sono toolchain di build: i plugin Gradle e i loro artefatti di supporto.
BUILD_ONLY_GROUPS = ("com.android.tools.build", "com.google.devtools.ksp")
BUILD_ONLY_HINTS = (
    "compiler", "junit", "mockk", "robolectric", "turbine", "espresso",
    "testing", "gradle-plugin", "kotlin-serialization",
)


def is_build_or_test_only(coordinate):
    """True se la coordinata non è attesa nel classpath runtime dell'app."""
    group, _, artifact = coordinate.partition(":")
    if group.lower().startswith(TEST_GROUP_PREFIXES):
        return True
    if group.lower().startswith(BUILD_ONLY_GROUPS):
        return True
    return any(hint in artifact.lower() for hint in BUILD_ONLY_HINTS)


def parse_lockfile(text):
    """group:artifact -> versione, da un file di dependency locking."""
    resolved = {}
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#") or line.startswith("empty="):
            continue
        match = LOCK_RE.match(line)
        if match:
            resolved[f"{match.group(1)}:{match.group(2)}"] = match.group(3).strip()
    return resolved


def parse_dependency_report(text):
    """group:artifact -> versione, dall'albero di `gradle :app:dependencies`.

    Gestisce le annotazioni del report: `(*)` gia' mostrato, `(c)` vincolo,
    `FAILED` non risolto e `-> x.y.z` quando Gradle ha corretto la versione.
    """
    resolved = {}
    for line in text.splitlines():
        if any(marker in line for marker in SKIP_MARKERS):
            continue
        match = TREE_RE.search(line)
        if not match:
            continue
        group, artifact, version = match.group("coord").split(":")
        arrow = RESOLVED_ARROW_RE.search(match.group("rest"))
        if arrow:
            version = arrow.group(1)
        resolved[f"{group}:{artifact}"] = version
    return resolved


def load_resolved(path):
    """Sceglie il parser in base al contenuto: lockfile oppure report ad albero."""
    text = path.read_text(encoding="utf-8", errors="replace")
    from_lock = parse_lockfile(text)
    if from_lock:
        return from_lock, "gradle.lockfile"
    from_report = parse_dependency_report(text)
    if from_report:
        return from_report, "dependency-report"
    return {}, "unknown"


def pom_declared(path):
    """group:artifact -> versione dichiarata nel manifest."""
    root = ET.parse(path).getroot()
    declared = {}
    for dep in root.findall(".//m:dependencies/m:dependency", NS):
        group = dep.find("m:groupId", NS)
        artifact = dep.find("m:artifactId", NS)
        version = dep.find("m:version", NS)
        if group is None or artifact is None:
            continue
        key = f"{group.text.strip()}:{artifact.text.strip()}"
        declared[key] = version.text.strip() if version is not None and version.text else None
    return declared


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("resolved_file", type=pathlib.Path,
                        help="gradle.lockfile oppure deps.txt")
    parser.add_argument("--out", type=pathlib.Path, default=None,
                        help="report JSON di output (default: resolved.json accanto allo script)")
    args = parser.parse_args()

    if not args.resolved_file.is_file():
        print(f"ERRORE: file non trovato: {args.resolved_file}")
        return 2

    resolved, source_kind = load_resolved(args.resolved_file)
    if not resolved:
        print(f"ERRORE: nessuna coordinata riconosciuta in {args.resolved_file}.")
        print("Attesi: righe 'group:artifact:version=config' (lockfile) oppure nodi")
        print("'+--- group:artifact:version' (report :app:dependencies).")
        return 2

    declared = pom_declared(POM_PATH)
    drift = []
    for key, version in sorted(resolved.items()):
        if key in declared and declared[key] and declared[key] != version:
            drift.append({"coordinate": key, "dichiarata": declared[key], "risolta": version})
    not_resolved = sorted(key for key in declared if key not in resolved)
    build_only = [key for key in not_resolved if is_build_or_test_only(key)]
    unexpected = [key for key in not_resolved if not is_build_or_test_only(key)]
    transitive_only = sorted(key for key in resolved if key not in declared)

    report = {
        "source": str(args.resolved_file),
        "source_kind": source_kind,
        "resolved_count": len(resolved),
        "declared_count": len(declared),
        "transitive_count": len(transitive_only),
        "drift": drift,
        "declared_not_resolved": not_resolved,
        "declared_build_or_test_only": build_only,
        "declared_unexpected_absent": unexpected,
        "coordinates": [{"coordinate": k, "version": v} for k, v in sorted(resolved.items())],
    }

    out = args.out or (pathlib.Path(__file__).resolve().parent / "resolved.json")
    out.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    print(f"sorgente: {args.resolved_file} ({source_kind})")
    print(f"coordinate risolte: {len(resolved)}")
    print(f"coordinate dichiarate nel manifest: {len(declared)}")
    print(f"presenti solo come transitive: {len(transitive_only)}")
    print(f"versioni diverse dal manifest: {len(drift)}")
    for item in drift:
        print(f"  DRIFT: {item['coordinate']}  manifest={item['dichiarata']}  risolta={item['risolta']}")
    print(f"dichiarate assenti dal grafo runtime: {len(not_resolved)}")
    if build_only:
        print(f"  di cui solo compilazione/test, atteso: {len(build_only)}")
        for key in build_only:
            print(f"    BUILD/TEST: {key}")
    if unexpected:
        print(f"  INATTESE, dovrebbero essere nel runtime: {len(unexpected)}")
        for key in unexpected:
            print(f"    INATTESA: {key}")
    print(f"\nreport scritto in {out}")

    if drift or unexpected:
        print("\nAggiorna tools/dependency-audit/pom.xml con le versioni risolte e rilancia lo scan.")
        return 1
    print("\nManifest allineato alle versioni risolte: lo scan ora copre anche le transitive elencate.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
