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
    testImplementation(libs.junit5)
    testImplementation(libs.assertj)
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
