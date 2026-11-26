plugins {
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.54.1.0")
    implementation("com.formdev:flatlaf:3.5.4")
    implementation("com.formdev:flatlaf-extras:3.5.4")
    implementation("com.github.librepdf:openpdf:2.0.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(27)
    }
}

application {
    mainClass = "org.credit.Main"
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 26
}

tasks.named<JavaExec>("run") {
    environment("_JAVA_AWT_WM_NONREPARENTING", "1")

}
tasks.test {
    useJUnitPlatform()
}

