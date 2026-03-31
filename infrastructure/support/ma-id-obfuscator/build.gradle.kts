dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.hashids:hashids:1.0.3")

    implementation(project(":domain:ma-domain-core"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
