dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	testImplementation("org.springframework.boot:spring-boot-starter-test")

	implementation(project(":domain:ma-domain-core"))
	runtimeOnly(project(":config:ma-config-yaml-importer"))
	runtimeOnly(project(":support:ma-sms-sender"))
	runtimeOnly(project(":storage:ma-redis-core"))
}
