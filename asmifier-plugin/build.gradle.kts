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

val javaVersion = JavaVersion.VERSION_11
java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
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
    systemProperty("asm_version", libs.versions.asm.get())
}
tasks.withType(JavaCompile::class.java) {
    if (name.contains("test", true)) {
        options.errorprone.isEnabled.set(false)
        val testJavaVersion = JavaVersion.VERSION_15.toString()
        sourceCompatibility = testJavaVersion
        targetCompatibility = testJavaVersion
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
