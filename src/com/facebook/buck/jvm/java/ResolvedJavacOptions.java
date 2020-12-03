/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.buck.jvm.java;

import com.facebook.buck.core.filesystems.AbsPath;
import com.facebook.buck.core.sourcepath.PathSourcePath;
import com.facebook.buck.core.sourcepath.resolver.SourcePathResolverAdapter;
import com.facebook.buck.core.util.immutables.BuckStyleValue;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolved JavacOptions used in {@link JavacPipelineState} */
@BuckStyleValue
public abstract class ResolvedJavacOptions {

  abstract Optional<String> getBootclasspath();

  abstract Optional<List<AbsPath>> getBootclasspathList();

  abstract JavacLanguageLevelOptions getLanguageLevelOptions();

  abstract boolean isDebug();

  abstract boolean isVerbose();

  abstract JavacPluginParams getJavaAnnotationProcessorParams();

  abstract JavacPluginParams getStandardJavacPluginParams();

  abstract List<String> getExtraArguments();

  abstract ImmutableList<JavacPluginJsr199Fields> getAnnotationProcessors();

  abstract ImmutableList<JavacPluginJsr199Fields> getJavaPlugins();

  abstract boolean isJavaAnnotationProcessorParamsPresent();

  /** Creates {@link ResolvedJavacOptions} */
  public static ResolvedJavacOptions of(
      JavacOptions javacOptions, SourcePathResolverAdapter resolver, AbsPath ruleCellRoot) {

    JavacLanguageLevelOptions languageLevelOptions = javacOptions.getLanguageLevelOptions();
    ImmutableList<PathSourcePath> bootclasspath =
        javacOptions.getSourceToBootclasspath().get(languageLevelOptions.getSourceLevel());
    Optional<List<AbsPath>> bootclasspathList = Optional.empty();
    if (bootclasspath != null) {
      bootclasspathList =
          Optional.of(
              bootclasspath.stream().map(resolver::getAbsolutePath).collect(Collectors.toList()));
    }

    JavacPluginParams javaAnnotationProcessorParams =
        javacOptions.getJavaAnnotationProcessorParams();
    JavacPluginParams standardJavacPluginParams = javacOptions.getStandardJavacPluginParams();

    return of(
        javacOptions.getBootclasspath(),
        bootclasspathList,
        languageLevelOptions,
        javacOptions.isDebug(),
        javacOptions.isVerbose(),
        javaAnnotationProcessorParams,
        standardJavacPluginParams,
        javacOptions.getExtraArguments(),
        extractJavacPluginJsr199Fields(javaAnnotationProcessorParams, ruleCellRoot),
        extractJavacPluginJsr199Fields(standardJavacPluginParams, ruleCellRoot),
        !javaAnnotationProcessorParams.isEmpty());
  }

  /** Creates {@link ResolvedJavacOptions} */
  public static ResolvedJavacOptions of(
      Optional<String> bootclasspath,
      Optional<List<AbsPath>> bootclasspathList,
      JavacLanguageLevelOptions languageLevelOptions,
      boolean debug,
      boolean verbose,
      JavacPluginParams javaAnnotationProcessorParams,
      JavacPluginParams standardJavacPluginParams,
      List<String> extraArguments,
      ImmutableList<JavacPluginJsr199Fields> annotationProcessors,
      ImmutableList<JavacPluginJsr199Fields> javaPlugins,
      boolean javaAnnotationProcessorParamsPresent) {
    return ImmutableResolvedJavacOptions.ofImpl(
        bootclasspath,
        bootclasspathList,
        languageLevelOptions,
        debug,
        verbose,
        javaAnnotationProcessorParams,
        standardJavacPluginParams,
        extraArguments,
        annotationProcessors,
        javaPlugins,
        javaAnnotationProcessorParamsPresent);
  }

  private static ImmutableList<JavacPluginJsr199Fields> extractJavacPluginJsr199Fields(
      JavacPluginParams javacPluginParams, AbsPath ruleCellRoot) {
    return javacPluginParams.getPluginProperties().stream()
        .map(p -> p.getJavacPluginJsr199Fields(ruleCellRoot))
        .collect(ImmutableList.toImmutableList());
  }

  /** Validates classpath */
  public void validateClasspath(Function<String, Boolean> classpathChecker) throws IOException {
    Optional<String> bootclasspath = getBootclasspath();
    if (!bootclasspath.isPresent()) {
      return;
    }
    String bootClasspath = bootclasspath.get();

    try {
      if (!classpathChecker.apply(bootClasspath)) {
        throw new IOException(
            String.format("Bootstrap classpath %s contains no valid entries", bootClasspath));
      }
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }
}
