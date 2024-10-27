import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("java-gradle-plugin")
    id("net.ltgt.errorprone") version "4.1.0"
    id("com.diffplug.spotless") version "6.25.0"
}

dependencies {
    implementation("org.ow2.asm:asm-util:9.7.1")
    errorprone("com.google.errorprone:error_prone_core:2.35.1")
    errorprone("com.uber.nullaway:nullaway:0.12.0")
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

gradlePlugin {
    plugins {
        create("asmifierPlugin") {
            id = "com.likethesalad.asmifier"
            implementationClass = "com.likethesalad.asm.AsmifierPlugin"
        }
    }
}

tasks.withType(Test::class.java) {
    useJUnitPlatform()
}
tasks.withType(JavaCompile::class.java) {
    if (name.contains("test", true)) {
        options.errorprone.isEnabled.set(false)
    } else {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "com.likethesalad.asm")
        }
    }
}
spotless {
    java {
        googleJavaFormat()
    }
}
tasks.named("classes").configure {
    dependsOn("spotlessApply")
}
