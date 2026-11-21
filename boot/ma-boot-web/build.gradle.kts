
import org.asciidoctor.gradle.jvm.AsciidoctorTask
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	id("org.asciidoctor.jvm.convert") version "3.3.2"
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")

	implementation(project(":domain:ma-domain-core"))
	runtimeOnly("com.github.holeman79:spring-yaml-importer:1.0.0")
	runtimeOnly(project(":infrastructure:support:ma-sms-sender"))
	runtimeOnly(project(":infrastructure:support:ma-crypto-core"))
	runtimeOnly(project(":infrastructure:support:ma-jwt-core"))
	runtimeOnly(project(":infrastructure:storage:ma-db-core"))
	runtimeOnly(project(":infrastructure:storage:ma-redis-core"))
	runtimeOnly(project(":infrastructure:support:ma-file-storage"))
	runtimeOnly(project(":infrastructure:support:ma-id-obfuscator"))
	runtimeOnly(project(":infrastructure:support:ma-payment-core"))
	testImplementation(project(":infrastructure:support:ma-jwt-core"))
	testImplementation(project(":infrastructure:support:ma-id-obfuscator"))
	testImplementation(testFixtures(project(":infrastructure:storage:ma-db-core")))
	testImplementation(testFixtures(project(":domain:ma-domain-core")))

	val asciidoctorExt: Configuration by configurations.creating
	asciidoctorExt("org.springframework.restdocs:spring-restdocs-asciidoctor")
}

configure<org.springframework.boot.gradle.dsl.SpringBootExtension> {
	buildInfo()
}


tasks {
	val snippetsDir = file("build/generated-snippets")

	withType<Test> {
		useJUnitPlatform()
		outputs.dir(snippetsDir)
	}

	withType<AsciidoctorTask> {
		asciidoctor.get().doFirst {
			delete {
				file("build/docs/asciidoc")
				file("src/main/resources/static/docs")
			}
		}
		configurations("asciidoctorExt")
		inputs.dir(snippetsDir)
		dependsOn(test)
		forkOptions {
			jvmArgs(
				"--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
				"--add-opens", "java.base/java.io=ALL-UNNAMED"
			)
		}
	}

	withType<BootJar> {
		delete {
			file("build/docs/asciidoc")
			file("src/main/resources/static/docs")
		}
		dependsOn(":domain:ma-domain-core:test")
		dependsOn(asciidoctor)
		from("${asciidoctor.get().outputDir}/html5") {
			into("static/docs")
		}

		archiveBaseName.set("api")
		archiveVersion.set("")
	}

	val copyDocument by registering(Copy::class) {
		dependsOn(asciidoctor)

		from(file("build/docs/asciidoc/"))
		into(file("src/main/resources/static/docs"))
	}

	build {
		dependsOn(copyDocument)
	}
}

tasks.jar {
	enabled = false
}
