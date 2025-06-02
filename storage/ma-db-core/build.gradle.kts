plugins {
    kotlin("plugin.jpa") version "1.9.22"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:0.57.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.57.0")

    implementation(project(":domain:ma-domain-core"))

    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
