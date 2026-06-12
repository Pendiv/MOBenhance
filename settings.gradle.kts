plugins {
    // Lets Gradle auto-download the Java 25 toolchain when it isn't installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "EnhancedMobs"

// attributelib(兄弟プロジェクト)を composite build で参照する。
// 依存表記 "DIV:attributelib" は自動的にローカルビルドへ置換される。
includeBuild("../attributelib")
