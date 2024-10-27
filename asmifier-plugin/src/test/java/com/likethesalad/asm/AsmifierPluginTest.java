package com.likethesalad.asm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AsmifierPluginTest {
  @TempDir File projectDir;

  @BeforeEach
  void setUp() throws IOException {
    createBuildFile();
    createFile(
        "settings.gradle.kts",
        """
                import org.gradle.api.initialization.resolve.RepositoriesMode

                dependencyResolutionManagement {
                    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                    repositories {
                        mavenCentral()
                    }
                }
                """);
  }

  @Test
  void verifyFileOutput() throws IOException {
    createAsmifierSourceFile(
        "com/test/MyClass.java",
        """
                package com.test;

                public class MyClass {
                    public void someMethod() {
                        System.out.println("Hello World!");
                    }
                }
                """);
    createAsmifierSourceFile(
        "com/test/MyOtherClass.java",
        """
                package com.test;

                public class MyOtherClass {
                    public void someOtherMethod() {
                        System.out.println("Hello Other World!");
                    }
                }
                """);

    BuildResult result = asmifierRunner().build();

    assertThat(getAsmifierOutcome(result)).isEqualTo(TaskOutcome.SUCCESS);
    Map<String, File> generatedFiles = getGeneratedFiles();
    assertThat(generatedFiles.keySet())
        .containsExactlyInAnyOrder(
            "asm/com/test/MyClassDump.java", "asm/com/test/MyOtherClassDump.java");
  }

  @Test
  void verifyIncrementalCompilation() throws IOException {
    File myClassFile =
        createAsmifierSourceFile(
            "com/test/MyClass.java",
            """
                    package com.test;

                    public class MyClass {
                        public void someMethod() {
                            System.out.println("Hello World!");
                        }
                    }
                    """);
    createAsmifierSourceFile(
        "com/test/MySecondClass.java",
        """
                    package com.test;

                    public class MySecondClass {
                        public void someSecondMethod() {
                            System.out.println("Hello World!");
                        }
                    }
                    """);
    createAsmifierSourceFile(
        "com/test/MyThirdClass.java",
        """
                        package com.test;

                        public class MyThirdClass {
                            public void someThirdMethod() {
                                System.out.println("Hello World!");
                            }
                        }
                        """);

    BuildResult result = asmifierRunner().build();

    assertThat(getAsmifierOutcome(result)).isEqualTo(TaskOutcome.SUCCESS);
    Map<String, File> generatedFiles = getGeneratedFiles();
    assertThat(generatedFiles.keySet())
        .containsExactlyInAnyOrder(
            "asm/com/test/MyClassDump.java",
            "asm/com/test/MySecondClassDump.java",
            "asm/com/test/MyThirdClassDump.java");
    BasicFileAttributes secondClassAttrs =
        getFileAttrs(generatedFiles.get("asm/com/test/MySecondClassDump.java"));
    BasicFileAttributes thirdClassAttrs =
        getFileAttrs(generatedFiles.get("asm/com/test/MyThirdClassDump.java"));
    long secondClassCreationTime = secondClassAttrs.creationTime().toMillis();
    long secondClassModifiedTime = secondClassAttrs.lastModifiedTime().toMillis();
    long thirdClassCreationTime = thirdClassAttrs.creationTime().toMillis();
    long thirdClassModifiedTime = thirdClassAttrs.lastModifiedTime().toMillis();

    // Removing input
    if (!myClassFile.delete()) {
      fail("Could not delete the input file");
    }
    // Modifying input
    createAsmifierSourceFile(
        "com/test/MyThirdClass.java",
        """
                            package com.test;

                            public class MyThirdClass {
                                public void someThirdMethod() {
                                    System.out.println("Hello Changed World!");
                                }
                            }
                            """);
    // Adding input
    createAsmifierSourceFile(
        "com/test/MyFourthClass.java",
        """
                            package com.test;

                            public class MyFourthClass {
                                public void someFourthMethod() {
                                    System.out.println("Hello World!");
                                }
                            }
                            """);

    // Rerun
    BuildResult secondResult = asmifierRunner().build();

    assertThat(getAsmifierOutcome(secondResult)).isEqualTo(TaskOutcome.SUCCESS);
    Map<String, File> generatedFilesSecondRun = getGeneratedFiles();
    assertThat(generatedFilesSecondRun.keySet())
        .containsExactlyInAnyOrder(
            "asm/com/test/MySecondClassDump.java",
            "asm/com/test/MyThirdClassDump.java",
            "asm/com/test/MyFourthClassDump.java");

    // Assert untouched file to not have been regenerated
    BasicFileAttributes secondClassSecondRunAttrs =
        getFileAttrs(generatedFilesSecondRun.get("asm/com/test/MySecondClassDump.java"));
    assertThat(secondClassSecondRunAttrs.creationTime().toMillis())
        .isEqualTo(secondClassCreationTime);
    assertThat(secondClassSecondRunAttrs.lastModifiedTime().toMillis())
        .isEqualTo(secondClassModifiedTime);

    // Assert modified file has changed
    BasicFileAttributes thirdClassSecondRunAttrs =
        getFileAttrs(generatedFilesSecondRun.get("asm/com/test/MyThirdClassDump.java"));
    assertThat(thirdClassSecondRunAttrs.creationTime().toMillis())
        .isEqualTo(thirdClassCreationTime);
    assertThat(thirdClassSecondRunAttrs.lastModifiedTime().toMillis())
        .isGreaterThan(thirdClassModifiedTime);
  }

  @Test
  void verifyTaskIsSkippedWhenNoInputsAreAvailable() {
    BuildResult result = asmifierRunner().build();

    assertThat(getAsmifierOutcome(result)).isEqualTo(TaskOutcome.NO_SOURCE);
  }

  private @NotNull Map<String, File> getGeneratedFiles() throws IOException {
    return getDirFiles("build/generated/sources/asmifierDump");
  }

  private Map<String, File> getDirFiles(String projectDirPath) throws IOException {
    Map<String, File> files = new HashMap<>();
    File dir = getProjectFile(projectDirPath);
    if (!dir.exists()) {
      return files;
    }
    Path dirPath = dir.toPath();
    Files.walkFileTree(
        dirPath,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            files.put(dirPath.relativize(file).toString(), file.toFile());
            return FileVisitResult.CONTINUE;
          }
        });

    return files;
  }

  private GradleRunner asmifierRunner() {
    return GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments("asmifier");
  }

  private void createBuildFile() throws IOException {
    createFile(
        "build.gradle.kts",
        """
                plugins {
                    id("java")
                    id("com.likethesalad.asmifier")
                }

                dependencies {
                    asmifier("org.ow2.asm:asm-util:9.7.1")
                }
                """);
  }

  private File createAsmifierSourceFile(String javaRelativePath, String contents)
      throws IOException {
    return createFile("src/asmifier/java/" + javaRelativePath, contents);
  }

  private File createFile(String projectRelativePath, String contents) throws IOException {
    File file = getProjectFile(projectRelativePath);
    if (file.getParentFile() != projectDir && !file.getParentFile().exists()) {
      file.getParentFile().mkdirs();
    }
    Files.writeString(file.toPath(), contents);
    return file;
  }

  private File getProjectFile(String projectRelativePath) {
    return new File(projectDir, projectRelativePath);
  }

  private static TaskOutcome getAsmifierOutcome(BuildResult result) {
    return result.task(":asmifier").getOutcome();
  }

  private static BasicFileAttributes getFileAttrs(File file) throws IOException {
    return Files.readAttributes(file.toPath(), BasicFileAttributes.class);
  }
}