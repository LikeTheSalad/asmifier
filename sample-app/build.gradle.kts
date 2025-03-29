plugins {
    id("java")
    id("com.likethesalad.asmifier") /* the version isn't needed for this project, but you must add it in yours */
}

dependencies {
    asmifier("org.ow2.asm:asm-util:9.8")
}