plugins {
    id("java-gradle-plugin")
}

dependencies {
    implementation("org.ow2.asm:asm-util:9.7.1")
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

tasks.withType(Test::class.java) {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("asmfierPlugin") {
            id = "com.likethesalad.asmfier"
            implementationClass = "com.likethesalad.asm.AsmfierPlugin"
        }
    }
}