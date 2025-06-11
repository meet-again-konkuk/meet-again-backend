dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation(project(":domain:ma-domain-core"))
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
} 