plugins {
	id("org.asciidoctor.jvm.convert") version "3.3.2"
}

val snippetsDir by extra { file("build/generated-snippets") }

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	testImplementation("org.springframework.boot:spring-boot-starter-test")

	// RestDocs 의존성 추가
	testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")

	implementation(project(":domain:ma-domain-core"))
	runtimeOnly(project(":config:ma-config-yaml-importer"))
	runtimeOnly(project(":support:ma-sms-sender"))
	runtimeOnly(project(":storage:ma-db-core"))
	runtimeOnly(project(":storage:ma-redis-core"))
}

tasks {
	test {
		useJUnitPlatform()
		outputs.dir(snippetsDir)
		testLogging {
			events("passed", "skipped", "failed")
		}
	}

	asciidoctor {
		inputs.dir(snippetsDir)
		dependsOn(test)
	}
}

// 빌드 시 asciidoctor 태스크가 실행되도록 설정
tasks.bootJar {
	dependsOn(tasks.asciidoctor)
	from("${tasks.asciidoctor.get().outputDir}") {
		into("static/docs")
	}
}
