plugins {
    application
}

group = "io.github.inhalelol.jnx"
version = "1.5.0"

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "io.github.inhalelol.jnx.JNX"
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // https://mvnrepository.com/artifact/com.fazecast/jSerialComm
    implementation("com.fazecast:jSerialComm:2.11.0")
}

application {
    mainClass.set("io.github.inhalelol.jnx.JNX")
}

tasks.test {
    useJUnitPlatform()
}