plugins {
    application
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

application.mainClass.set("com.qiuchris.Bot")
group = "com.qiuchris"
version = "1.0"

val jdaVersion = "5.0.0-beta.16"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.dv8tion:JDA:$jdaVersion")
    implementation("org.jsoup:jsoup:1.16.2")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.isIncremental = true

    // Set this to the version of java you want to use,
    // the minimum required for JDA is 1.8
    sourceCompatibility = "1.8"
}