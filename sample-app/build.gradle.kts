plugins {
    id("java")
    id("com.likethesalad.asmifier") /* the version isn't needed for this project, but you must add it in yours */
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

dependencies {
    asmifier("org.ow2.asm:asm-util:9.9.1")
}