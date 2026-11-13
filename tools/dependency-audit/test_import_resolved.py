#!/usr/bin/env python3
"""Collaudo di import_resolved.py su fixture realistiche.

Genera un lockfile e un report `:app:dependencies` sintetici a partire dalle
coordinate reali di pom.xml, poi verifica parsing, correzioni `-> x.y.z`,
esclusione delle righe non-dipendenza e codici di uscita.

Uso: python3 tools/dependency-audit/test_import_resolved.py
Exit code: 0 se tutti i controlli passano, 1 altrimenti.
"""
import json
import pathlib
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET

HERE = pathlib.Path(__file__).resolve().parent
SCRIPT = HERE / "import_resolved.py"
POM = HERE / "pom.xml"
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def _run_script(name):
    proc = subprocess.run(
        [sys.executable, str(HERE / name)],
        capture_output=True,
        text=True,
        cwd=ROOT,
    )
    return proc.returncode, proc.stdout + proc.stderr

REPORT_FIXTURE = """------------------------------------------------------------
Project :app
------------------------------------------------------------

releaseRuntimeClasspath - Runtime classpath of 'release'.
+--- androidx.core:core-ktx:1.13.1
|    +--- androidx.annotation:annotation:1.7.1
|    \\--- org.jetbrains.kotlin:kotlin-stdlib:2.0.21 -> 2.1.0
+--- androidx.compose.foundation:foundation-layout:1.7.6 (c)
+--- project :core:data
\\--- com.example:broken:1.0 FAILED
"""


def pom_dependencies():
    root = ET.parse(POM).getroot()
    deps = []
    for dep in root.findall(".//m:dependencies/m:dependency", NS):
        group = dep.find("m:groupId", NS).text.strip()
        artifact = dep.find("m:artifactId", NS).text.strip()
        version_el = dep.find("m:version", NS)
        version = version_el.text.strip() if version_el is not None and version_el.text else None
        deps.append((group, artifact, version))
    return deps


def run(path, out):
    proc = subprocess.run(
        [sys.executable, str(SCRIPT), str(path), "--out", str(out)],
        capture_output=True, text=True,
    )
    return proc.returncode, proc.stdout + proc.stderr


def test_lockfile_detects_drift_and_transitives(tmp):
    deps = pom_dependencies()
    assert deps, "pom.xml senza coordinate: il collaudo non avrebbe senso"
    lines = ["# This is a Gradle generated file for dependency locking.", "empty=annotationProcessor"]
    for index, (group, artifact, version) in enumerate(deps):
        resolved = "9.9.9" if index == 0 else version
        lines.append(f"{group}:{artifact}:{resolved}=releaseRuntimeClasspath")
    lines.append("com.example:fake-transitive:1.0.0=releaseRuntimeClasspath")
    lockfile = tmp / "gradle.lockfile"
    lockfile.write_text("\n".join(lines) + "\n")

    declared = {f"{group}:{artifact}" for group, artifact, _ in deps}
    assert "com.example:fake-transitive" not in declared, (
        "la coordinata fittizia deve essere assente dal pom, altrimenti non è una transitiva"
    )

    code, out = run(lockfile, tmp / "lock.json")
    assert code == 1, f"atteso 1 con una versione in drift, ottenuto {code}: {out}"
    assert f"coordinate risolte: {len(deps) + 1}" in out, f"conteggio errato: {out}"
    assert "DRIFT:" in out, f"drift non segnalato: {out}"
    assert "presenti solo come transitive: 1" in out, f"transitiva non riconosciuta: {out}"
    data = json.loads((tmp / "lock.json").read_text())
    assert data["drift"][0]["risolta"] == "9.9.9", "versione risolta non riportata nel report"


def test_report_tree_handles_gradle_annotations(tmp):
    report = tmp / "deps.txt"
    report.write_text(REPORT_FIXTURE)

    code, out = run(report, tmp / "report.json")
    assert code in (0, 1), f"exit inatteso {code}: {out}"
    assert "coordinate risolte: 3" in out, f"l'albero non ha prodotto 3 coordinate: {out}"
    assert "FAILED" not in out and "project :" not in out, "righe non-dipendenza incluse"

    data = json.loads((tmp / "report.json").read_text())
    versions = {item["coordinate"]: item["version"] for item in data["coordinates"]}
    assert versions["org.jetbrains.kotlin:kotlin-stdlib"] == "2.1.0", "correzione -> non applicata"
    assert "androidx.compose.foundation:foundation-layout" not in versions, "vincolo (c) incluso"
    assert data["source_kind"] == "dependency-report", "tipo di sorgente errato"


def test_unreadable_input_exits_two(tmp):
    empty = tmp / "empty.txt"
    empty.write_text("nessuna coordinata qui\n")
    code, out = run(empty, tmp / "empty.json")
    assert code == 2, f"atteso 2 su file senza coordinate, ottenuto {code}: {out}"
    assert "ERRORE" in out, "nessun messaggio d'errore esplicito"


def test_missing_file_exits_two(tmp):
    code, out = run(tmp / "non-esiste.txt", tmp / "missing.json")
    assert code == 2, f"atteso 2 su file assente, ottenuto {code}: {out}"


def test_real_lockfile_is_aligned(tmp):
    """Il lockfile versionato deve risultare allineato al manifest."""
    lockfile = HERE.parents[1] / "app" / "gradle.lockfile"
    if not lockfile.is_file():
        print("SKIP test_real_lockfile_is_aligned: app/gradle.lockfile assente")
        return
    code, out = run(lockfile, tmp / "real.json")
    assert code == 0, f"il lockfile del repo non è allineato (exit {code}): {out}"
    assert "solo compilazione/test, atteso" in out, f"classificazione build/test assente: {out}"
    data = json.loads((tmp / "real.json").read_text())
    assert data["drift"] == [], f"drift inatteso: {data['drift']}"
    assert data["declared_unexpected_absent"] == [], "coordinate runtime assenti dal grafo"
    assert data["resolved_count"] >= 150, f"lockfile troppo piccolo: {data['resolved_count']}"
    assert data["transitive_count"] == 0, (
        "il pom deve contenere tutto il grafo risolto: rigeneralo con sync_pom_from_lockfile.py"
    )
    assert len(data["declared_build_or_test_only"]) == 17, (
        f"attese 17 coordinate ksp/test/plugin, trovate {len(data['declared_build_or_test_only'])}"
    )


def test_pom_matches_lockfile(tmp):
    """Il pom di audit deve corrispondere a quanto genera il lockfile (nessuna deriva)."""
    sync = HERE / "sync_pom_from_lockfile.py"
    lockfile = HERE.parents[1] / "app" / "gradle.lockfile"
    if not (sync.is_file() and lockfile.is_file()):
        print("SKIP test_pom_matches_lockfile: script o lockfile assenti")
        return
    proc = subprocess.run(
        [sys.executable, str(sync), "--check"], capture_output=True, text=True
    )
    assert proc.returncode == 0, f"pom non allineato al lockfile: {proc.stdout}{proc.stderr}"
    assert "ALLINEATO" in proc.stdout, f"esito inatteso: {proc.stdout}"
    text = (HERE / "pom.xml").read_text(encoding="utf-8")
    entries = text.count("<dependency>")
    assert entries >= 150, f"pom con troppe poche voci ({entries}): manca il grafo risolto"
    assert "<scope>test</scope>" in text, "le voci di test non sono state conservate"


def test_catalog_plugins_are_in_manifest(tmp):
    """I plugin di build del catalogo devono essere nel manifest, con la versione del catalogo."""
    sys.path.insert(0, str(HERE))
    import sync_pom_from_lockfile as sync

    plugins = sync.read_catalog_plugins()
    assert len(plugins) == 6, f"attesi 6 plugin nel catalogo, trovati {len(plugins)}: {plugins}"
    assert "com.android.tools.build:gradle" in plugins, "AGP assente dal catalogo dei plugin"
    pom = (HERE / "pom.xml").read_text(encoding="utf-8")
    for coordinate, version in plugins.items():
        group, artifact = coordinate.split(":", 1)
        pattern = (
            f"<groupId>{group}</groupId>\n            <artifactId>{artifact}</artifactId>\n"
            f"            <version>{version}</version>"
        )
        assert pattern in pom, f"plugin assente o con versione diversa nel manifest: {coordinate}:{version}"
    assert "<scope>provided</scope>" in pom, "i plugin devono essere marcati provided, non runtime"


def test_app_graph_has_no_toolchain_findings(tmp):
    """I pacchetti segnalati nel buildscript non devono comparire nel grafo dell'app."""
    sys.path.insert(0, str(HERE))
    import scope_check

    code, out = _run_script("scope_check.py")
    assert code == 0, f"scope_check ha fallito ({code}): {out}"
    assert "OK: nessuno dei pacchetti segnalati" in out, f"esito non riconosciuto: {out}"

    app = scope_check.read_lock(scope_check.APP_LOCK)
    toolchain = scope_check.read_lock(scope_check.BUILDSCRIPT_LOCK)
    assert len(app) == 157, f"attese 157 coordinate nell'app, trovate {len(app)}"
    assert len(toolchain) == 155, f"attese 155 coordinate nel buildscript, trovate {len(toolchain)}"
    assert not [key for key in scope_check.FLAGGED if key in app], "pacchetto segnalato nel grafo app"
    # guava nell'app è solo listenablefuture: non è il guava segnalato.
    assert "com.google.guava:guava" not in app, "guava completo non deve essere nell'app"


def main():
    tests = [
        test_lockfile_detects_drift_and_transitives,
        test_report_tree_handles_gradle_annotations,
        test_unreadable_input_exits_two,
        test_missing_file_exits_two,
        test_real_lockfile_is_aligned,
        test_pom_matches_lockfile,
        test_catalog_plugins_are_in_manifest,
        test_app_graph_has_no_toolchain_findings,
    ]
    failures = 0
    with tempfile.TemporaryDirectory() as raw:
        tmp = pathlib.Path(raw)
        for test in tests:
            try:
                test(tmp)
                print(f"PASS {test.__name__}")
            except AssertionError as exc:
                failures += 1
                print(f"FAIL {test.__name__}: {exc}")
    print(f"\n{len(tests) - failures}/{len(tests)} controlli superati")
    return 0 if failures == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
