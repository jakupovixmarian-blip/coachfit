#!/usr/bin/env python3
"""Verifica che i finding della toolchain di build non tocchino il grafo dell'app.

Perché esiste: il repository contiene due lockfile con scopi diversi.

  app/gradle.lockfile          -> ciò che finisce nell'APK (release/debug Compile+Runtime)
  buildscript-gradle.lockfile  -> il classpath del buildscript: AGP, Kotlin, KSP, Hilt e le
                                  loro transitive (netty, guava, protobuf, bouncycastle...)

Lo scanner di AppJet tratta il repository come un unico scope, quindi somma i due grafi:
324 pacchetti, 49 segnalazioni (24 high/critical). Senza questa verifica quel numero sembra
riguardare l'app. Non è così: qui si dimostra, in modo ripetibile, che nessuno dei pacchetti
segnalati compare nel grafo dell'app.

Uso: python3 tools/dependency-audit/scope_check.py
Exit code: 0 se l'app è pulita, 1 se un pacchetto segnalato è entrato nel grafo dell'app.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
APP_LOCK = ROOT / "app" / "gradle.lockfile"
BUILDSCRIPT_LOCK = ROOT / "buildscript-gradle.lockfile"

# Pacchetti con almeno una segnalazione nel buildscript, secondo OSV-Scanner (rev. 50).
# Elenco esplicito: se un domani uno di questi entra nel grafo dell'app, il controllo
# fallisce invece di aggiornarsi da solo e nascondere la regressione.
# Aggiornato dopo la chiusura di CVE-2026-53914: kotlin-gradle-plugin, protobuf-java e
# commons-io non hanno più segnalazioni; jose4j (transitiva di AGP) è entrato nell'elenco.
FLAGGED = (
    "io.netty:netty-codec",
    "io.netty:netty-codec-http",
    "io.netty:netty-codec-http2",
    "io.netty:netty-common",
    "io.netty:netty-handler",
    "io.netty:netty-handler-proxy",
    "org.bouncycastle:bcpkix-jdk18on",
    "org.bouncycastle:bcprov-jdk18on",
    "com.google.guava:guava",
    "org.apache.commons:commons-compress",
    "org.bitbucket.b_c:jose4j",
    "org.jdom:jdom2",
)

LOCK_RE = re.compile(r"^([A-Za-z0-9_.\-]+:[A-Za-z0-9_.\-]+):([^=\s]+)\s*=")


def read_lock(path: pathlib.Path) -> dict[str, str]:
    """coordinata group:artifact -> versione risolta."""
    if not path.is_file():
        print(f"ERRORE: lockfile assente: {path}", file=sys.stderr)
        raise SystemExit(2)
    out: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or line.startswith("empty="):
            continue
        match = LOCK_RE.match(line)
        if match:
            out[match.group(1)] = match.group(2)
    return out


def main() -> int:
    app = read_lock(APP_LOCK)
    toolchain = read_lock(BUILDSCRIPT_LOCK)

    leaked = {key: app[key] for key in FLAGGED if key in app}
    toolchain_hits = {key: toolchain[key] for key in FLAGGED if key in toolchain}
    shared = sorted(set(app) & set(toolchain))

    print(f"grafo app (APK):        {len(app)} coordinate")
    print(f"grafo buildscript:      {len(toolchain)} coordinate")
    print(f"coordinate condivise:   {len(shared)}" + (f" -> {', '.join(shared)}" if shared else ""))
    print(f"pacchetti segnalati nel buildscript: {len(toolchain_hits)}/{len(FLAGGED)}")
    for key, version in sorted(toolchain_hits.items()):
        print(f"  toolchain: {key} {version}")

    if leaked:
        print("\nFALLITO: pacchetti con vulnerabilità note presenti nel grafo dell'app:", file=sys.stderr)
        for key, version in sorted(leaked.items()):
            print(f"  {key}:{version}", file=sys.stderr)
        print("Aggiornali o escludili prima di considerare l'app pulita.", file=sys.stderr)
        return 1

    print("\nOK: nessuno dei pacchetti segnalati è nel grafo dell'app.")
    print("Le segnalazioni riguardano solo il classpath del buildscript (non finisce nell'APK).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
