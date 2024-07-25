plugins {
    id("java")
}

group = "com.nechost"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.apache.mina:mina-core:2.2.3")
    implementation("org.apache.logging.log4j:log4j-core:2.14.1")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.14.1")
    implementation("mysql:mysql-connector-java:8.0.23")
    implementation("com.zaxxer:HikariCP:4.0.3")

}

tasks.test {
    useJUnitPlatform()
}