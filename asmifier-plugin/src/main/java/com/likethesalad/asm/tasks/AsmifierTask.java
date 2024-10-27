package com.likethesalad.asm.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileType;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.ChangeType;
import org.gradle.work.FileChange;
import org.gradle.work.InputChanges;

public abstract class AsmifierTask extends DefaultTask {

  @PathSensitive(PathSensitivity.RELATIVE)
  @SkipWhenEmpty
  @InputDirectory
  public abstract DirectoryProperty getTargetClasses();

  @InputFiles
  public abstract ConfigurableFileCollection getClasspath();

  @OutputDirectory
  public abstract DirectoryProperty getOutputDir();

  @TaskAction
  public void execute(InputChanges inputChanges) {
    Project project = getProject();
    FileCollection classpath = project.files(getClasspath(), getTargetClasses().getAsFile());

    if (inputChanges.isIncremental()) {
      Iterable<FileChange> fileChanges = inputChanges.getFileChanges(getTargetClasses());
      for (FileChange fileChange : fileChanges) {
        if (fileChange.getFileType().equals(FileType.DIRECTORY)) {
          continue;
        }

        if (fileChange.getChangeType().equals(ChangeType.REMOVED)) {
          File targetFile =
              getTargetFile(fileChange.getNormalizedPath(), fileChange.getFile().getName());
          if (!targetFile.delete()) {
            getLogger().warn("Could not delete: {}", targetFile);
          }
        } else if (fileChange.getChangeType().equals(ChangeType.ADDED)
            || fileChange.getChangeType().equals(ChangeType.MODIFIED)) {
          String relativeSourcePath = fileChange.getNormalizedPath();
          File outputFile = getOutputFile(relativeSourcePath, fileChange.getFile().getName());
          asmfyToFile(outputFile, project, classpath, relativeSourcePath);
        }
      }
    } else {
      getTargetClasses()
          .getAsFileTree()
          .visit(
              fileVisitDetails -> {
                if (!fileVisitDetails.isDirectory()) {
                  String relativeSourcePath = fileVisitDetails.getRelativePath().getPathString();
                  File outputFile = getOutputFile(relativeSourcePath, fileVisitDetails.getName());
                  asmfyToFile(outputFile, project, classpath, relativeSourcePath);
                }
              });
    }
  }

  private void asmfyToFile(
      File outputFile, Project project, FileCollection classpath, String relativeSourcePath) {
    try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
      asmfy(project, classpath, relativeSourcePath, outputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private File getOutputFile(String sourceRelativePath, String sourceFileName) {
    File outputFile = getTargetFile(sourceRelativePath, sourceFileName);
    try {
      outputFile.getParentFile().mkdirs();
      outputFile.createNewFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return outputFile;
  }

  private File getTargetFile(String sourceRelativePath, String sourceFileName) {
    String classSimpleName = sourceFileName.replaceFirst("\\.class", "");
    return getOutputDir()
        .file(
            "asm/" + sourceRelativePath.replaceFirst(sourceFileName, classSimpleName + "Dump.java"))
        .get()
        .getAsFile();
  }

  private void asmfy(
      Project project, FileCollection classpath, String relativePath, OutputStream outputStream) {
    String className = relativePath.replaceFirst("\\.class", "").replaceAll("/", ".");
    project.javaexec(
        javaExecSpec -> {
          javaExecSpec.setClasspath(classpath);
          javaExecSpec.getMainClass().set("org.objectweb.asm.util.ASMifier");
          javaExecSpec.args("-nodebug");
          javaExecSpec.args(className);
          javaExecSpec.setStandardOutput(outputStream);
        });
  }
}
