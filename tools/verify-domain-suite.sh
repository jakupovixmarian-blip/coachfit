#!/usr/bin/env bash
# Esegue la suite di test dei motori di dominio di FitCoach senza Gradle.
#
# Perché esiste: i motori (calcoli, nutrizione, tool calling, generatore schede,
# dieta, progressione, timer) sono Kotlin puro e possono essere verificati senza
# Android SDK. Questo script compila quei moduli con il compilatore Kotlin e li
# esegue con JUnit. Le annotazioni Room sono stub generate qui sotto: servono solo
# a far compilare entità e DAO, non a parlare con un database.
#
# Non sostituisce `./gradlew assembleDebug testDebugUnitTest`: non compila le
# schermate Compose, non esegue Hilt e non prova la migrazione Room.
#
# Uso:
#   bash tools/verify-domain-suite.sh
#
# Variabili opzionali:
#   KOTLIN_LIB_DIR   cartella con kotlin-compiler-embeddable, kotlin-stdlib,
#                    kotlinx-coroutines-core-jvm, annotations e trove4j
#   WORK_DIR         cartella di lavoro per classi compilate e stub
#                    (default: build/domain-verify)

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KOTLIN_LIB_DIR="${KOTLIN_LIB_DIR:-/tmp/kt/lib}"
WORK_DIR="${WORK_DIR:-$REPO_ROOT/build/domain-verify}"
SRC="$REPO_ROOT/app/src/main/java/dev/fitcoach/app"
TESTS="$REPO_ROOT/app/src/test/java/dev/fitcoach/app"

die() { echo "ERRORE: $*" >&2; exit 1; }

[ -d "$KOTLIN_LIB_DIR" ] || die "cartella dei jar Kotlin non trovata: $KOTLIN_LIB_DIR (imposta KOTLIN_LIB_DIR)"

pick() {
  local pattern="$1" found
  found="$(find "$KOTLIN_LIB_DIR" "$HOME/.m2/repository" -name "$pattern" 2>/dev/null | head -1 || true)"
  [ -n "$found" ] || die "jar non trovato: $pattern"
  printf '%s' "$found"
}

KC="$(pick 'kotlin-compiler-embeddable-2.0.21.jar')"
STDLIB="$(pick 'kotlin-stdlib-2.0.21.jar')"
CORO="$(pick 'kotlinx-coroutines-core-jvm-1.9.0.jar')"
TROVE="$(pick 'trove4j-1.0.20200330.jar')"
ANN="$(pick 'annotations-24.1.0.jar')"
JVMCP="$KC:$STDLIB:$CORO:$TROVE:$ANN"
CP="$(find "$KOTLIN_LIB_DIR" -name '*.jar' ! -name '*coroutines-core-jvm-1.6*' | tr '\n' ':')"

mkdir -p "$WORK_DIR"
rm -rf "$WORK_DIR/out" "$WORK_DIR/tout" "$WORK_DIR/stubs"
mkdir -p "$WORK_DIR/stubs"

# Stub delle annotazioni Room: solo superficie di compilazione.
cat > "$WORK_DIR/stubs/Room.kt" <<'KOTLIN'
package androidx.room

@Target(AnnotationTarget.CLASS)
annotation class Entity(
    val tableName: String = "",
    val indices: Array<Index> = [],
    val foreignKeys: Array<ForeignKey> = [],
    val primaryKeys: Array<String> = []
)

@Target(AnnotationTarget.CLASS)
annotation class Index(vararg val value: String, val unique: Boolean = false, val name: String = "")

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
annotation class PrimaryKey(val autoGenerate: Boolean = false)

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
annotation class ColumnInfo(val name: String = "")

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS)
annotation class Embedded(val prefix: String = "")

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
annotation class Relation(
    val parentColumn: String = "",
    val entityColumn: String = "",
    val entity: kotlin.reflect.KClass<*> = Any::class
)

@Target(AnnotationTarget.CLASS)
annotation class Junction(
    val value: kotlin.reflect.KClass<*> = Any::class,
    val parentColumn: String = "",
    val entityColumn: String = ""
)

@Target(AnnotationTarget.CLASS)
annotation class ForeignKey(
    val entity: kotlin.reflect.KClass<*> = Any::class,
    val parentColumns: Array<String> = [],
    val childColumns: Array<String> = [],
    val onDelete: Int = 1,
    val onUpdate: Int = 1
) {
    companion object {
        const val NO_ACTION = 1
        const val RESTRICT = 2
        const val SET_NULL = 3
        const val SET_DEFAULT = 4
        const val CASCADE = 5
    }
}

@Target(AnnotationTarget.CLASS)
annotation class Dao

@Target(AnnotationTarget.FUNCTION)
annotation class Query(val value: String)

@Target(AnnotationTarget.FUNCTION)
annotation class Insert(val onConflict: Int = 1)

@Target(AnnotationTarget.FUNCTION)
annotation class Update(val onConflict: Int = 1)

@Target(AnnotationTarget.FUNCTION)
annotation class Delete

@Target(AnnotationTarget.FUNCTION)
annotation class Transaction

@Target(AnnotationTarget.FUNCTION)
annotation class Ignore

@Target(AnnotationTarget.CLASS)
annotation class TypeConverter

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class TypeConverters(vararg val value: kotlin.reflect.KClass<*>)

object OnConflictStrategy {
    const val REPLACE = 1
    const val ROLLBACK = 2
    const val ABORT = 3
    const val FAIL = 4
    const val IGNORE = 5
}

@Target(AnnotationTarget.FUNCTION)
annotation class Upsert
KOTLIN

# Moduli di dominio: Kotlin puro, nessuna dipendenza da Android o Hilt.
PURE="$(find "$SRC/domain" "$SRC/core" "$SRC/data/catalog" "$SRC/data/ai" -name '*.kt' \
  -exec grep -L -e 'import android' -e 'import androidx' -e 'import dagger' {} \;)"
FULL="$PURE $(find "$SRC/data/local/entity" -name '*.kt') \
$SRC/data/local/mapper/Mappers.kt $SRC/data/local/dao/Daos.kt"

TEST_FILES="$TESTS/EnergyEngineTest.kt $TESTS/NutritionMathTest.kt $TESTS/FoodValidatorTest.kt \
$TESTS/VolumeAnalyzerTest.kt $TESTS/FoodMatcherTest.kt $TESTS/FoodCatalogTest.kt \
$TESTS/CreaImportTest.kt $TESTS/ToolEngineTest.kt $TESTS/AiOrchestratorTest.kt \
$TESTS/PlanGeneratorTest.kt $TESTS/RestTimerTest.kt $TESTS/ResumeSessionTest.kt \
$TESTS/DietGeneratorTest.kt $TESTS/ProgressionApprovalTest.kt"

TEST_CLASSES="dev.fitcoach.app.EnergyEngineTest dev.fitcoach.app.NutritionMathTest \
dev.fitcoach.app.FoodValidatorTest dev.fitcoach.app.VolumeAnalyzerTest \
dev.fitcoach.app.FoodMatcherTest dev.fitcoach.app.FoodCatalogTest \
dev.fitcoach.app.CreaImportTest dev.fitcoach.app.ToolEngineTest \
dev.fitcoach.app.AiOrchestratorTest dev.fitcoach.app.PlanGeneratorTest \
dev.fitcoach.app.RestTimerTest dev.fitcoach.app.ResumeSessionTest \
dev.fitcoach.app.DietGeneratorTest dev.fitcoach.app.ProgressionApprovalTest"

echo "== compilazione moduli di dominio =="
java -cp "$JVMCP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -no-stdlib -no-reflect -classpath "$CP" -d "$WORK_DIR/out" "$WORK_DIR/stubs/Room.kt" $FULL
echo "MAIN_COMPILE_OK"

echo "== compilazione test =="
java -cp "$JVMCP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -no-stdlib -no-reflect -classpath "$CP:$WORK_DIR/out" -d "$WORK_DIR/tout" $TEST_FILES
echo "TEST_COMPILE_OK"

echo "== esecuzione test =="
java -cp "$CP:$WORK_DIR/out:$WORK_DIR/tout" org.junit.runner.JUnitCore $TEST_CLASSES
