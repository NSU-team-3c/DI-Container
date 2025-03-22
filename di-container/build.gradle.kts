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
    annotationProcessor("org.projectlombok:lombok:1.18.28");
    testImplementation("org.projectlombok:lombok:1.18.28")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.28");

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.13.0")
    implementation("javax.inject:javax.inject:1")
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")
    implementation("org.jgrapht:jgrapht-core:1.5.0")
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.reflections:reflections:0.9.10")
    implementation("org.javassist:javassist:3.20.0-GA")
}

tasks.test {
    useJUnitPlatform()
}