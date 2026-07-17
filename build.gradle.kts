plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.3.4"
	id("io.spring.dependency-management") version "1.1.6"
}

allprojects {
	repositories {
		mavenCentral()
	}
}

subprojects {
	apply(plugin = "org.springframework.boot")
	apply(plugin = "io.spring.dependency-management")
	apply(plugin = "kotlin")
	apply(plugin = "kotlin-spring")
	apply(plugin = "kotlin-kapt")

	// AWS SDK apache5-client(2.46.x)는 httpclient5 5.4+/httpcore5 5.4+를 요구 —
	// Spring Boot BOM이 5.3.1/5.2.5로 내리면 S3Client 생성이 NoClassDefFoundError로 실패한다
	ext["httpclient5.version"] = "5.6.1"
	ext["httpcore5.version"] = "5.4.3"

	if (project.name != "ma-config-logging") {
		dependencies {
			implementation(project(":config:ma-config-logging"))
		}
	}

	dependencies {

		// KoTest
		testImplementation("io.kotest:kotest-runner-junit5-jvm:5.5.5")
		testImplementation("io.kotest:kotest-assertions-core-jvm:5.5.5")
		testImplementation("io.kotest:kotest-extensions-jvm:5.5.5")
		testImplementation("io.kotest:kotest-property-jvm:5.5.5")
		testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.2")

		// Mockk
		testImplementation("com.ninja-squad:springmockk:4.0.0")
	}

	group = "com.s2w"
	version = "0.0.1-SNAPSHOT"
	tasks.register("prepareKotlinBuildScriptModel"){}

	java {
		toolchain {
			languageVersion.set(JavaLanguageVersion.of(21))
		}
	}

	tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
		kotlinOptions {
			freeCompilerArgs = freeCompilerArgs + "-Xjsr305=strict"
		}
	}

	tasks.withType<Test> {
		useJUnitPlatform()
	}
}

tasks.named("bootJar") {
	enabled = false
}

tasks.named("jar") {
	enabled = false
}
