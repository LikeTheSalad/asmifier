plugins {
    id("com.likethesalad.artifact-publisher")
}

artifactPublisher {
    displayName = "ASMifier gradle plugin"
    url = "https://github.com/LikeTheSalad/asmifier"
    vcsUrl = "https://github.com/LikeTheSalad/asmifier.git"
    issueTrackerUrl = "https://github.com/LikeTheSalad/asmifier/issues"
    tags.addAll("asm", "java", "bytecode", "asmifier")
}