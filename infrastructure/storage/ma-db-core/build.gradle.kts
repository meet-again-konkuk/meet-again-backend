// Flyway Gradle 플러그인(운영 분리 마이그레이션 스텝)이 실행 시 사용할 DB 드라이버/모듈을
// 플러그인 classpath(buildscript)에 올린다. MariaDB 지원은 flyway-mysql 필요.
buildscript {
    dependencies {
        classpath("org.mariadb.jdbc:mariadb-java-client:3.3.3")
        classpath("org.flywaydb:flyway-mysql:10.10.0")
    }
}

plugins {
    kotlin("plugin.jpa") version "1.9.22"
    `java-test-fixtures`
    // 운영 배포용 분리 마이그레이션 스텝(`./gradlew ...:flywayMigrate`). 버전은 런타임 flyway-core(10.10.0)와 일치.
    id("org.flywaydb.flyway") version "10.10.0"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:0.57.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.57.0")

    // Flyway (스키마 마이그레이션) — MariaDB 지원은 flyway-mysql 필요
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    implementation(project(":domain:ma-domain-core"))

    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.exposed:exposed-spring-boot-starter:0.57.0")
    testImplementation("org.jetbrains.exposed:exposed-java-time:0.57.0")
    testRuntimeOnly("com.h2database:h2")

    testFixturesApi("org.springframework.boot:spring-boot-starter-test")
    testFixturesApi("org.jetbrains.exposed:exposed-spring-boot-starter:0.57.0")
    testFixturesApi("org.jetbrains.exposed:exposed-java-time:0.57.0")
    testFixturesApi("com.h2database:h2")
    testFixturesApi(project(":domain:ma-domain-core"))
}

// 운영(prod): 앱 부팅과 분리해 배포 파이프라인에서 실행한다.
//   DB_URL/DB_USERNAME/DB_PASSWORD 환경변수 주입 후 `./gradlew :infrastructure:storage:ma-db-core:flywayMigrate`
// 로컬(local): 부팅 시 자동 마이그레이션(application-local.yml 의 spring.flyway 설정)이 담당하므로 이 태스크는 불필요.
flyway {
    url = System.getenv("DB_URL")
    user = System.getenv("DB_USERNAME")
    password = System.getenv("DB_PASSWORD")
    locations = arrayOf("filesystem:src/main/resources/db/migration")
    baselineOnMigrate = true
    baselineVersion = "1"
}
