plugins {
    application
}

group = "io.github.inhalelol.jnx"
version = "1.4"

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
}

application {
    mainClass.set("io.github.inhalelol.jnx.JNX")
}

tasks.test {
    useJUnitPlatform()
}