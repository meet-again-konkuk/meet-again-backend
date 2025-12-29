plugins {
    kotlin("plugin.jpa") version "1.9.22"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:0.57.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.57.0")

    implementation(project(":domain:ma-domain-core"))

    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.exposed:exposed-spring-boot-starter:0.57.0")
    testImplementation("org.jetbrains.exposed:exposed-java-time:0.57.0")
}
