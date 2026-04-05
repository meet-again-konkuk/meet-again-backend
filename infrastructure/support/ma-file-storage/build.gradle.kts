dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation(project(":domain:ma-domain-core"))

    implementation("net.coobird:thumbnailator:0.4.20")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-batik:3.11.0")
}
