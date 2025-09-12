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

	allOpen {
		annotation("jakarta.persistence.Entity")
		annotation("jakarta.persistence.MappedSuperclass")
		annotation("jakarta.persistence.Embeddable")
	}

	dependencies {
//		implementation(project(":config:ma-config-logging"))
		if (project.path != ":config:ma-config-logging") {
			implementation(project(":config:ma-config-logging"))
		}

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
