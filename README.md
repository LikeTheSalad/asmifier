# Gradle ASMifier

What it is
---

Convenience tool that converts `.java` source files (and non-Java ones that still compile into JVM bytecode, such as
Kotlin's (`.kt`) ones, for example) to [ASM](https://asm.ow2.io/) instructions by wrapping
the [ASMifier](https://asm.ow2.io/javadoc/org/objectweb/asm/util/ASMifier.html) tool around a Gradle incremental task
that can convert multiple source files at once.

This tool, as well as [the one it's built upon](https://asm.ow2.io/javadoc/org/objectweb/asm/util/ASMifier.html), is
meant to be used as a development tool for anyone who'd like to check how does source code translate into bytecode
instructions using [ASM](https://asm.ow2.io/).

What it is not
---

This is not a tool to generate production code. Its source target files are in a separate location from the
production source files (similarly to the test sources, which are in a separate dir that isn't packaged into the
production app).

How to use
---

### Add the plugin to your project

You can find the latest version of the plugin [here](https://plugins.gradle.org/plugin/com.likethesalad.asmifier).

```kotlin
// build.gradle.kts
plugins {
    id("java") // or java-library
    id("com.likethesalad.asmifier") version "[latest]"
}
```

### Add the ASMifier dependency

[ASMifier](https://asm.ow2.io/javadoc/org/objectweb/asm/util/ASMifier.html) is part of ASM's util library, which you can
find [here](https://central.sonatype.com/artifact/org.ow2.asm/asm-util). This plugin needs to use that library to
perform the conversion, so you must add it as part of your project's `asmifier` dependencies as shown below.

```kts
// build.gradle.kts
plugins { /* ... */ }

dependencies {
    asmifier("org.ow2.asm:asm-util:[latest]")
}
```

You can find the latest ASM util lib version [here](https://central.sonatype.com/artifact/org.ow2.asm/asm-util).

> [!NOTE]
> The `asmifier` dependency type is added by this plugin to ensure that its dependencies are separated from those
> of your app (similarly to configurations such as `testImplementation` are only used for a specific purpose and not
> to get packaged with your production code).

### Add sources to transform

The sources that will be transformed by this plugin must be placed in a src dir named `asmifier`, as
shown below.

```text
app/
├─ build.gradle.kts
├─ src/
│  ├─ main/
│  │  ├─ java/
│  ├─ asmifier/
│  │  ├─ java/ <-- Here is where the asmifier .java (or other JVM-supported) target files must be placed
```

### Run the Gradle task

To transform the source files you must run the Gradle task named `asmifier`, like so:

```shell
./gradlew asmifier
```

### Finding the ASMified sources

Once the `asmifier` task has finished, you can find its results in your app's `build` dir
under `build/generated/sources/asmifierDump`, as shown below.

```text
app/
├─ build/
│  ├─ generated/
│  │  ├─ sources/
│  │  │  ├─ asmifierDump/ <-- Here you will find the transformed files.
```

Try it out
---

You can clone this repo and try this plugin with [this sample app](sample-app). You can test it by running the gradlew
command within the `sample-app` dir. You must make sure to cd into `sample-app` before running the gradlew command.
