plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")
    implementation("com.theokanning.openai-gpt3-java:service:0.18.2")
    implementation("software.amazon.awssdk:bedrock:2.29.10")
    implementation("software.amazon.awssdk:bedrockruntime:2.29.10")
    implementation("software.amazon.awssdk:sdk-core:2.29.10")
    implementation("software.amazon.awssdk:apache-client:2.29.10")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}