plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.projectlombok:lombok:1.18.28")
    implementation("org.projectlombok:lombok:1.18.28")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.13.0")
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.reflections:reflections:0.9.10")
    implementation("org.javassist:javassist:3.20.0-GA")
}

tasks.test {
    useJUnitPlatform()
}