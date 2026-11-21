// InvSee Summary für Minecraft 26.1.x und 26.2 (unverschleiert, Java 25) – Port der 1.21.11-Fassung von Noah (MIT).
plugins {
    id("net.fabricmc.fabric-loom") version "1.19.4"
}

val mc = providers.gradleProperty("mc").get()
val fabricApi = mapOf("26.2" to "0.161.0+26.2", "26.1.2" to "0.155.3+26.1.2", "26.1.1" to "0.145.4+26.1.1", "26.1" to "0.145.1+26.1")

version = "1.0.0+$mc"
group = "de.invseesummary"
base { archivesName.set("invsee-summary") }

dependencies {
    minecraft("com.mojang:minecraft:$mc")
    implementation("net.fabricmc:fabric-loader:0.20.1")
    implementation("net.fabricmc.fabric-api:fabric-api:${fabricApi[mc]}")
}

// Unterschiede zwischen den Versionen stecken in src/compat/<Version>/Compat.java
sourceSets.main { java.srcDir("src/compat/" + if (mc.startsWith("26.2")) "26.2" else "26.1") }

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mc", mc)
    filesMatching("fabric.mod.json") { expand("version" to project.version, "mc" to mc) }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
tasks.withType<JavaCompile>().configureEach { options.release.set(25); options.encoding = "UTF-8" }
