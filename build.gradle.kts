plugins {
	id("groovy")
}

group = "com.konkuk"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
}

subprojects {
	apply {
		plugin("groovy")
	}

	group = "com.konkuk"
	version = "0.0.1-SNAPSHOT"

	dependencies {
		implementation("org.codehaus.groovy:groovy-all:3.0.16")
	}

	tasks.register("prepareKotlinBuildScriptModel"){}
}
