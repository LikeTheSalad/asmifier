package com.likethesalad.asm;

import com.likethesalad.asm.tasks.AsmfierTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.jetbrains.annotations.NotNull;

public final class AsmfierPlugin implements Plugin<Project> {
  private static final String ASMFIER_TASK_NAME = "asmfier";
  private static final String ASMFIER_OUTPUT_DIR_NAME = ASMFIER_TASK_NAME + "Dump";

  @Override
  public void apply(Project project) {
    JavaPluginExtension javaExtension =
        project.getExtensions().getByType(JavaPluginExtension.class);
    SourceSet asmfierSourceSet = javaExtension.getSourceSets().create(ASMFIER_TASK_NAME);
    TaskProvider<JavaCompile> compileJava =
        project.getTasks().named(asmfierSourceSet.getCompileJavaTaskName(), JavaCompile.class);

    Configuration asmfierClasspath = getAsmfierClasspath(project, asmfierSourceSet);

    TaskProvider<AsmfierTask> asmfierTaskTaskProvider =
        project.getTasks().register(ASMFIER_TASK_NAME, AsmfierTask.class);
    asmfierTaskTaskProvider.configure(
        asmfierTask -> {
          asmfierTask
              .getOutputDir()
              .set(
                  project
                      .getLayout()
                      .getBuildDirectory()
                      .dir("generated/sources/" + ASMFIER_OUTPUT_DIR_NAME));
          asmfierTask
              .getTargetClasses()
              .set(compileJava.flatMap(JavaCompile::getDestinationDirectory));
          asmfierTask.getClasspath().from(asmfierClasspath);
        });

    configureDumpSourceSet(
        project,
        asmfierSourceSet,
        javaExtension,
        asmfierTaskTaskProvider.flatMap(AsmfierTask::getOutputDir));
  }

  private static void configureDumpSourceSet(
      Project project,
      SourceSet asmfierSourceSet,
      JavaPluginExtension javaExtension,
      Provider<Directory> asmfierOutputDir) {
    SourceSet dumpAsmfier =
        javaExtension
            .getSourceSets()
            .create(
                ASMFIER_OUTPUT_DIR_NAME, sourceSet -> sourceSet.getJava().srcDir(asmfierOutputDir));

    ConfigurationContainer configurations = project.getConfigurations();
    configurations
        .getByName(dumpAsmfier.getImplementationConfigurationName())
        .extendsFrom(
            configurations.getByName(asmfierSourceSet.getImplementationConfigurationName()));
  }

  private static @NotNull Configuration getAsmfierClasspath(
      Project project, SourceSet asmfierSourceSet) {
    ConfigurationContainer configurations = project.getConfigurations();
    Configuration asmfier =
        configurations.create(
            ASMFIER_TASK_NAME,
            configuration -> {
              configuration.setCanBeConsumed(false);
              configuration.setCanBeResolved(false);
            });
    configurations
        .getByName(asmfierSourceSet.getImplementationConfigurationName())
        .extendsFrom(asmfier);
    return configurations.getByName(asmfierSourceSet.getRuntimeClasspathConfigurationName());
  }
}
