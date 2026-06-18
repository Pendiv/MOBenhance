plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.69-stable")
    // カスタム属性・ダメージパイプライン基盤(別プラグインとして同サーバーに配置する)。
    // composite build によりローカルの ../attributelib が使われる。
    compileOnly("DIV:attributelib:1.0-SNAPSHOT")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    withType<JavaCompile>().configureEach {
        // Source files are UTF-8; don't let the Windows default (Shift-JIS) corrupt them.
        options.encoding = "UTF-8"
    }

    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("26.1.2")
        jvmArgs("-Xms2G", "-Xmx2G")
        // attributelib もテストサーバーへ自動配置する(composite build で先にビルドされる)。
        // バージョンが変わっても拾えるよう attributelib-*.jar をパターンで配置する
        // (旧: 1.0-SNAPSHOT 固定参照で jar 名変更時にロード失敗していた)。
        dependsOn(gradle.includedBuild("attributelib").task(":jar"))
        pluginJars.from(fileTree("../attributelib/build/libs") { include("attributelib-*.jar") })
    }

    processResources {
        // expand はこの charset でファイルを読む。未指定だとプラットフォーム既定(Windows では
        // Shift-JIS)になり、UTF-8 の日本語コメントが壊れて plugin.yml が読めなくなる
        filteringCharset = "UTF-8"
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
