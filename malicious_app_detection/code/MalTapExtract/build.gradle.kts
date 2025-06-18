plugins {
    id("java")
}

group = "com.tapjacking.maltapextract"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("de.fraunhofer.sit.sse.flowdroid:soot-infoflow:2.14.1")
    implementation("de.fraunhofer.sit.sse.flowdroid:soot-infoflow-summaries:2.14.1")
    implementation("de.fraunhofer.sit.sse.flowdroid:soot-infoflow-android:2.14.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.18.1")
    implementation("commons-codec:commons-codec:1.15")
    implementation("org.xerial:sqlite-jdbc:3.47.0.0")
    implementation("org.apache.commons:commons-lang3:3.17.0")
    // APKTool
    implementation(files("libs/apktool_2.10.0.jar"))
    implementation("com.beust:jcommander:1.82")


}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.tapjacking.maltapextract.Main"
        )
    }

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}