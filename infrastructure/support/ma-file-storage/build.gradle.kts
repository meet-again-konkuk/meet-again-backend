dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation(project(":domain:ma-domain-core"))

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    implementation("net.coobird:thumbnailator:0.4.20")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-batik:3.11.0")

    implementation(platform("software.amazon.awssdk:bom:2.46.20"))
    implementation("software.amazon.awssdk:s3")

    testImplementation(testFixtures(project(":domain:ma-domain-core")))
}
