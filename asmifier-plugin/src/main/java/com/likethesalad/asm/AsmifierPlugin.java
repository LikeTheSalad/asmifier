package com.likethesalad.asm;

import com.likethesalad.asm.tasks.AsmifierTask;
import java.util.List;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

public final class AsmifierPlugin implements Plugin<Project> {
  private static final String ASMIFIER_TASK_NAME = "asmifier";
  private static final String ASMIFIER_OUTPUT_DIR_NAME = ASMIFIER_TASK_NAME + "Dump";

  @Override
  public void apply(Project project) {
    JavaPluginExtension javaExtension =
        project.getExtensions().getByType(JavaPluginExtension.class);
    SourceSet asmifierSourceSet = javaExtension.getSourceSets().create(ASMIFIER_TASK_NAME);

    Configuration asmifierClasspath = getAsmifierClasspath(project, asmifierSourceSet);

    TaskProvider<Sync> asmifierTargetCollector =
        project.getTasks().register("asmifierCollector", Sync.class);
    asmifierTargetCollector.configure(
        sync -> {
          sync.from(
              asmifierSourceSet.getOutput(),
              copySpec -> copySpec.setIncludes(List.of("**/*.class")));
          sync.into(project.getLayout().getBuildDirectory().dir("intermediates/" + sync.getName()));
        });

    TaskProvider<AsmifierTask> asmifierTaskTaskProvider =
        project.getTasks().register(ASMIFIER_TASK_NAME, AsmifierTask.class);
    asmifierTaskTaskProvider.configure(
        asmifierTask -> {
          asmifierTask
              .getOutputDir()
              .set(
                  project
                      .getLayout()
                      .getBuildDirectory()
                      .dir("generated/sources/" + ASMIFIER_OUTPUT_DIR_NAME));
          asmifierTask.getTargetClasses().from(asmifierTargetCollector);
          asmifierTask.getClasspath().from(asmifierClasspath);
        });

    configureDumpSourceSet(
        project,
        asmifierSourceSet,
        javaExtension,
        asmifierTaskTaskProvider.flatMap(AsmifierTask::getOutputDir));
  }

  private static void configureDumpSourceSet(
      Project project,
      SourceSet asmifierSourceSet,
      JavaPluginExtension javaExtension,
      Provider<Directory> asmifierOutputDir) {
    SourceSet dumpAsmifier =
        javaExtension
            .getSourceSets()
            .create(
                ASMIFIER_OUTPUT_DIR_NAME,
                sourceSet -> sourceSet.getJava().srcDir(asmifierOutputDir));

    ConfigurationContainer configurations = project.getConfigurations();
    configurations
        .getByName(dumpAsmifier.getImplementationConfigurationName())
        .extendsFrom(
            configurations.getByName(asmifierSourceSet.getImplementationConfigurationName()));
  }

  private static @NotNull Configuration getAsmifierClasspath(
      Project project, SourceSet asmifierSourceSet) {
    ConfigurationContainer configurations = project.getConfigurations();
    Configuration asmifier =
        configurations.create(
            ASMIFIER_TASK_NAME,
            configuration -> {
              configuration.setCanBeConsumed(false);
              configuration.setCanBeResolved(false);
            });
    configurations
        .getByName(asmifierSourceSet.getImplementationConfigurationName())
        .extendsFrom(asmifier);
    return configurations.getByName(asmifierSourceSet.getRuntimeClasspathConfigurationName());
  }
}
