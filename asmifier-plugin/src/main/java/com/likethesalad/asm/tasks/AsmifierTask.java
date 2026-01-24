package com.likethesalad.asm.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileType;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;
import org.gradle.work.ChangeType;
import org.gradle.work.FileChange;
import org.gradle.work.InputChanges;

public abstract class AsmifierTask extends DefaultTask {

  @PathSensitive(PathSensitivity.RELATIVE)
  @SkipWhenEmpty
  @InputFiles
  public abstract ConfigurableFileCollection getTargetClasses();

  @InputFiles
  public abstract ConfigurableFileCollection getClasspath();

  @OutputDirectory
  public abstract DirectoryProperty getOutputDir();

  @SuppressWarnings("JavaxInjectOnAbstractMethod")
  @Inject
  public abstract ExecOperations getExecOperations();

  @TaskAction
  public void execute(InputChanges inputChanges) {
    Project project = getProject();
    FileCollection classpath = project.files(getClasspath(), getTargetClasses());

    if (inputChanges.isIncremental()) {
      Iterable<FileChange> fileChanges = inputChanges.getFileChanges(getTargetClasses());
      for (FileChange fileChange : fileChanges) {
        if (fileChange.getFileType().equals(FileType.DIRECTORY)) {
          continue;
        }

        if (fileChange.getChangeType().equals(ChangeType.REMOVED)) {
          File targetFile = getTargetFile(fileChange.getNormalizedPath());
          if (!targetFile.delete()) {
            getLogger().warn("Could not delete: {}", targetFile);
          }
        } else if (fileChange.getChangeType().equals(ChangeType.ADDED)
            || fileChange.getChangeType().equals(ChangeType.MODIFIED)) {
          String relativeSourcePath = fileChange.getNormalizedPath();
          File outputFile = getOutputFile(relativeSourcePath);
          asmifyToFile(outputFile, classpath, relativeSourcePath);
        }
      }
    } else {
      getTargetClasses()
          .getAsFileTree()
          .visit(
              fileVisitDetails -> {
                if (!fileVisitDetails.isDirectory()) {
                  String relativeSourcePath = fileVisitDetails.getRelativePath().getPathString();
                  File outputFile = getOutputFile(relativeSourcePath);
                  asmifyToFile(outputFile, classpath, relativeSourcePath);
                }
              });
    }
  }

  private void asmifyToFile(File outputFile, FileCollection classpath, String relativeSourcePath) {
    try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
      asmify(classpath, relativeSourcePath, outputStream);
    } catch (Exception e) {
      throw new RuntimeException(
          "Exception during asmifier run where the source is: "
              + relativeSourcePath
              + " and the output is: "
              + outputFile.getPath(),
          e);
    }
  }

  private File getOutputFile(String sourceRelativePath) {
    File outputFile = getTargetFile(sourceRelativePath);
    try {
      outputFile.getParentFile().mkdirs();
      outputFile.createNewFile();
    } catch (IOException e) {
      throw new RuntimeException("Exception while getting output file: " + outputFile.getPath(), e);
    }
    return outputFile;
  }

  private File getTargetFile(String sourceRelativePath) {
    String newPath = sourceRelativePath.replaceFirst("\\.class", "Dump.java");
    return getOutputDir().file("asm/" + newPath).get().getAsFile();
  }

  private void asmify(FileCollection classpath, String relativePath, OutputStream outputStream) {
    String className = relativePath.replaceFirst("\\.class", "").replaceAll("/", ".");
    getExecOperations()
        .javaexec(
            javaExecSpec -> {
              javaExecSpec.setClasspath(classpath);
              javaExecSpec.getMainClass().set("org.objectweb.asm.util.ASMifier");
              javaExecSpec.args("-nodebug");
              javaExecSpec.args(className);
              javaExecSpec.setStandardOutput(outputStream);
            });
  }
}
