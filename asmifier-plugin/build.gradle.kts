import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("java-gradle-plugin")
    alias(libs.plugins.errorprone)
    alias(libs.plugins.spotless)
}

dependencies {
    implementation(libs.asm.util)
    errorprone(libs.errorprone)
    errorprone(libs.nullaway)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.junit)
    testImplementation(libs.assertj)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

gradlePlugin {
    plugins {
        create("asmifierPlugin") {
            id = "com.likethesalad.asmifier"
            implementationClass = "com.likethesalad.asm.AsmifierPlugin"
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("asm_version", libs.versions.asm.get())
}
tasks.withType<JavaCompile> {
    if (name.contains("test", true)) {
        options.errorprone.isEnabled.set(false)
    } else {
        options.release = 11 // Ensuring deliverable jvm compatibility
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
