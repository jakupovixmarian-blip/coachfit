#!/usr/bin/env python3
"""Rigenera il manifest di audit (pom.xml) dal lockfile Gradle risolto.

Perché esiste: lo scanner segnala il pom come "manifest a versioni esplicite", cioè
controlla solo ciò che è scritto e non risolve le transitive. Scrivendo nel pom tutte
le coordinate di app/gradle.lockfile con la versione realmente risolta, il manifest
descrive per intero il grafo che finisce nell'APK.

Cosa mette nel pom:
  - tutte le coordinate del lockfile (runtime + transitive), versione risolta;
  - le coordinate del pom attuale che NON sono nel lockfile: sono processori di
    annotazioni e librerie di test, conservate con scope/type originali;
  - il <type> originale quando già presente (aar per AndroidX, pom per i BOM).

Uso:
  python3 tools/dependency-audit/sync_pom_from_lockfile.py           # riscrive pom.xml
  python3 tools/dependency-audit/sync_pom_from_lockfile.py --check   # exit 1 se disallineato
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
LOCKFILE = ROOT / "app" / "gradle.lockfile"
CATALOG = ROOT / "gradle" / "libs.versions.toml"
POM = HERE / "pom.xml"

# Mappa id plugin Gradle -> artefatto Maven che lo implementa. Le versioni NON sono
# scritte qui: si leggono dal version catalog (version.ref), cosi' il manifest segue
# il catalogo invece di duplicarlo. Senza queste voci il toolchain di build non viene
# controllato da nessuno scan, perche' non compare nel lockfile dell'app.
PLUGIN_ARTIFACTS = {
    "com.android.application": "com.android.tools.build:gradle",
    "org.jetbrains.kotlin.android": "org.jetbrains.kotlin:kotlin-gradle-plugin",
    "org.jetbrains.kotlin.plugin.compose": "org.jetbrains.kotlin:compose-compiler-gradle-plugin",
    "org.jetbrains.kotlin.plugin.serialization": "org.jetbrains.kotlin:kotlin-serialization",
    "com.google.devtools.ksp": "com.google.devtools.ksp:symbol-processing-gradle-plugin",
    "com.google.dagger.hilt.android": "com.google.dagger:hilt-android-gradle-plugin",
}

ANDROID_PREFIXES = (
    "androidx.",
    "com.google.android.",
    "com.google.firebase",
    "com.google.mlkit",
    "com.android.",
)

HEADER = """<?xml version="1.0" encoding="UTF-8"?>
<!--
  Manifest di SOLO AUDIT per il controllo advisory delle dipendenze.

  Questo file NON fa parte del build: il progetto e' un'app Android costruita con
  Gradle + version catalog (gradle/libs.versions.toml).

  GENERATO da tools/dependency-audit/sync_pom_from_lockfile.py a partire da
  app/gradle.lockfile: contiene le {resolved} coordinate risolte (runtime e transitive)
  piu' {build_only} coordinate usate solo in compilazione/test e {plugins} plugin di build
  letti da gradle/libs.versions.toml. Non modificarlo a mano: rigeneralo dopo aver
  aggiornato il lockfile o il catalogo.

  Il lockfile deve restare ristretto alle classpath dell'app (release/debug
  Compile+Runtime). Con lockAllConfigurations() compaiono ~51 segnalazioni di build
  tooling che non finisce nell'APK.
-->
"""

DEFAULT_TYPE_POM = "pom"


def read_lockfile(path: Path) -> dict[str, str]:
    """group:artifact -> version, dalla forma group:artifact:version=configurazioni."""
    resolved: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        left = line.split("=", 1)[0].strip()
        parts = left.split(":")
        if len(parts) != 3:
            continue
        group, artifact, version = parts
        resolved[f"{group}:{artifact}"] = version
    return resolved


def read_catalog_plugins(path: Path = CATALOG) -> dict[str, str]:
    """coordinata Maven del plugin -> versione dichiarata nel version catalog."""
    if not path.is_file():
        return {}
    versions: dict[str, str] = {}
    plugin_versions: list[tuple[str, str | None]] = []
    section = ""
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("["):
            section = line.strip("[]")
            continue
        if "=" not in line:
            continue
        name, _, value = line.partition("=")
        if section == "versions":
            versions[name.strip()] = value.strip().strip('"')
        elif section == "plugins":
            id_match = re.search(r'id\s*=\s*"([^"]+)"', value)
            ref_match = re.search(r'version\.ref\s*=\s*"([^"]+)"', value)
            literal_match = re.search(r'version\s*=\s*"([^"]+)"', value)
            if not id_match:
                continue
            version = None
            if ref_match:
                version = versions.get(ref_match.group(1))
            elif literal_match:
                version = literal_match.group(1)
            plugin_versions.append((id_match.group(1), version))

    plugins: dict[str, str] = {}
    for plugin_id, version in plugin_versions:
        coordinate = PLUGIN_ARTIFACTS.get(plugin_id)
        if coordinate and version:
            plugins[coordinate] = version
    return plugins


DEP_RE = re.compile(r"<dependency>(.*?)</dependency>", re.S)


def read_pom(path: Path) -> dict[str, dict[str, str]]:
    """group:artifact -> {version, scope, type} delle voci gia' presenti."""
    entries: dict[str, dict[str, str]] = {}
    for block in DEP_RE.findall(path.read_text(encoding="utf-8")):
        group = re.search(r"<groupId>(.*?)</groupId>", block)
        artifact = re.search(r"<artifactId>(.*?)</artifactId>", block)
        version = re.search(r"<version>(.*?)</version>", block)
        if not (group and artifact and version):
            continue
        key = f"{group.group(1).strip()}:{artifact.group(1).strip()}"
        scope = re.search(r"<scope>(.*?)</scope>", block)
        type_ = re.search(r"<type>(.*?)</type>", block)
        entries[key] = {
            "version": version.group(1).strip(),
            "scope": scope.group(1).strip() if scope else "",
            "type": type_.group(1).strip() if type_ else "",
        }
    return entries


def is_android(group: str, artifact: str) -> bool:
    if group.startswith(ANDROID_PREFIXES):
        return True
    return group == "com.google.dagger" and artifact == "hilt-android"


def render_dependency(key: str, version: str, scope: str = "", type_: str = "") -> str:
    group, artifact = key.split(":", 1)
    parts = [
        f"        <dependency>",
        f"            <groupId>{group}</groupId>",
        f"            <artifactId>{artifact}</artifactId>",
        f"            <version>{version}</version>",
    ]
    if type_:
        parts.append(f"            <type>{type_}</type>")
    if scope:
        parts.append(f"            <scope>{scope}</scope>")
    parts.append("        </dependency>")
    return "\n".join(parts)


def build_pom(
    resolved: dict[str, str],
    previous: dict[str, dict[str, str]],
    plugins: dict[str, str],
) -> str:
    runtime: list[str] = []
    for key in sorted(resolved):
        group, artifact = key.split(":", 1)
        known = previous.get(key, {})
        type_ = known.get("type") or ("aar" if is_android(group, artifact) else "")
        runtime.append(render_dependency(key, resolved[key], type_=type_))

    build_only: list[str] = []
    for key in sorted(previous):
        if key in resolved or key in plugins:
            continue
        info = previous[key]
        type_ = info["type"] or ("aar" if is_android(*key.split(":", 1)) else "")
        build_only.append(render_dependency(key, info["version"], scope=info["scope"], type_=type_))

    plugin_block = [
        render_dependency(key, version, scope="provided")
        for key, version in sorted(plugins.items())
    ]

    blocks = [
        HEADER.format(resolved=len(resolved), build_only=len(build_only), plugins=len(plugin_block)),
        '<project xmlns="http://maven.apache.org/POM/4.0.0"\n'
        '         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n'
        '         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 '
        'http://maven.apache.org/xsd/maven-4.0.0.xsd">\n'
        "    <modelVersion>4.0.0</modelVersion>\n"
        "    <groupId>dev.fitcoach</groupId>\n"
        "    <artifactId>dependency-audit</artifactId>\n"
        "    <version>1.0.0</version>\n"
        "    <packaging>pom</packaging>\n"
        "    <name>FitCoach dependency audit manifest</name>\n"
        "\n"
        "    <properties>\n"
        "        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n"
        "    </properties>\n"
        "\n"
        "    <!-- Gli artefatti AndroidX/Compose sono pubblicati su Google Maven, non su Central. -->\n"
        "    <repositories>\n"
        "        <repository>\n"
        "            <id>google</id>\n"
        "            <url>https://dl.google.com/dl/android/maven2/</url>\n"
        "        </repository>\n"
        "        <repository>\n"
        "            <id>central</id>\n"
        "            <url>https://repo1.maven.org/maven2/</url>\n"
        "        </repository>\n"
        "    </repositories>\n",
        "    <dependencies>",
        f"        <!-- Runtime e transitive risolte da app/gradle.lockfile ({len(runtime)}) -->",
        *runtime,
        "",
        f"        <!-- Solo compilazione/test: non entrano nel classpath runtime ({len(build_only)}) -->",
        *build_only,
        "",
        f"        <!-- Plugin di build dal version catalog: non finiscono nell'APK, ma le versioni vanno controllate ({len(plugin_block)}) -->",
        *plugin_block,
        "    </dependencies>",
        "</project>",
    ]
    return "\n".join(blocks) + "\n"


def main(argv: list[str]) -> int:
    check_only = "--check" in argv
    if not LOCKFILE.is_file():
        print(f"ERRORE: lockfile assente: {LOCKFILE}")
        return 2
    resolved = read_lockfile(LOCKFILE)
    if not resolved:
        print("ERRORE: nessuna coordinata nel lockfile")
        return 2
    previous = read_pom(POM)
    plugins = read_catalog_plugins()
    generated = build_pom(resolved, previous, plugins)
    current = POM.read_text(encoding="utf-8")
    changed = generated.strip() != current.strip()

    kept = sum(1 for key in previous if key not in resolved and key not in plugins)
    total = len(resolved) + kept + len(plugins)
    print(f"lockfile: {len(resolved)} coordinate risolte")
    print(f"pom: {len(previous)} voci prima, {total} dopo (runtime + build/test + plugin)")
    print(f"coordinate solo build/test conservate: {kept}")
    print(f"plugin di build letti dal catalogo: {len(plugins)}")

    if check_only:
        if changed:
            print("DISALLINEATO: pom.xml non corrisponde al lockfile, rilancia senza --check")
            return 1
        print("ALLINEATO: pom.xml corrisponde al lockfile")
        return 0

    if not changed:
        print("Nessuna modifica: pom.xml era gia' allineato")
        return 0
    POM.write_text(generated, encoding="utf-8")
    print(f"pom.xml riscritto: {POM}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
