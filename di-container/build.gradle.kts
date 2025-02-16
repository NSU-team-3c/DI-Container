plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.reflections:reflections:0.9.10")
    implementation("org.javassist:javassist:3.20.0-GA")
}

tasks.test {
    useJUnitPlatform()
}