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

package com.facebook.buck.skylark.function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.facebook.buck.core.starlark.compatible.BuckStarlark;
import com.facebook.buck.io.filesystem.ProjectFilesystem;
import com.facebook.buck.io.filesystem.impl.FakeProjectFilesystem;
import com.facebook.buck.io.filesystem.skylark.SkylarkFilesystem;
import com.facebook.buck.parser.api.PackageMetadata;
import com.facebook.buck.skylark.io.impl.NativeGlobber;
import com.facebook.buck.skylark.packages.PackageContext;
import com.facebook.buck.skylark.parser.context.ParseContext;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.cmdline.PackageIdentifier;
import com.google.devtools.build.lib.cmdline.RepositoryName;
import com.google.devtools.build.lib.events.Event;
import com.google.devtools.build.lib.events.EventCollector;
import com.google.devtools.build.lib.events.EventKind;
import com.google.devtools.build.lib.syntax.EvalException;
import com.google.devtools.build.lib.syntax.EvalUtils;
import com.google.devtools.build.lib.syntax.Module;
import com.google.devtools.build.lib.syntax.Mutability;
import com.google.devtools.build.lib.syntax.ParserInput;
import com.google.devtools.build.lib.syntax.SkylarkUtils;
import com.google.devtools.build.lib.syntax.Starlark;
import com.google.devtools.build.lib.syntax.StarlarkFile;
import com.google.devtools.build.lib.syntax.StarlarkThread;
import com.google.devtools.build.lib.vfs.FileSystemUtils;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import java.io.IOException;
import java.util.EnumSet;
import org.junit.Before;
import org.junit.Test;

public class SkylarkPackageModuleTest {
  private Path root;
  private EventCollector eventHandler;

  @Before
  public void setUp() {
    ProjectFilesystem projectFilesystem = FakeProjectFilesystem.createRealTempFilesystem();
    SkylarkFilesystem fileSystem = SkylarkFilesystem.using(projectFilesystem);
    root = fileSystem.getPath(projectFilesystem.getRootPath().toString());
    eventHandler = new EventCollector(EnumSet.allOf(EventKind.class));
  }

  @Test
  public void packageIsParsed() throws Exception {
    assertEquals(
        evaluate("package(visibility=['PUBLIC'])", true).getPackage().getVisibility().get(0),
        "PUBLIC");
  }

  @Test
  public void defaultValueIsReturned() throws Exception {
    PackageMetadata packageMetadata = evaluate("", true).getPackage();
    assertTrue(packageMetadata.getVisibility().isEmpty());
    assertTrue(packageMetadata.getVisibility().isEmpty());
  }

  @Test
  public void nonPackageFunctionsAreNotAllowed() throws Exception {
    evaluate("pkg = package_name()", false);
    assertEquals(eventHandler.iterator().next().getMessage(), "name 'package_name' is not defined");
  }

  private ParseContext evaluate(String expression, boolean expectSuccess)
      throws IOException, InterruptedException {
    Path buildFile = root.getChild("PACKAGE");
    FileSystemUtils.writeContentAsLatin1(buildFile, expression);
    return evaluate(buildFile, expectSuccess);
  }

  private ParseContext evaluate(Path buildFile, boolean expectSuccess)
      throws IOException, InterruptedException {
    try (Mutability mutability = Mutability.create("PACKAGE")) {
      return evaluate(buildFile, mutability, expectSuccess);
    }
  }

  private ParseContext evaluate(Path buildFile, Mutability mutability, boolean expectSuccess)
      throws IOException, InterruptedException {
    byte[] buildFileContent =
        FileSystemUtils.readWithKnownFileSize(buildFile, buildFile.getFileSize());
    StarlarkFile buildFileAst =
        StarlarkFile.parse(ParserInput.create(buildFileContent, buildFile.asFragment()));

    ImmutableMap.Builder<String, Object> module = ImmutableMap.builder();
    module.putAll(Starlark.UNIVERSE);
    Starlark.addMethods(module, SkylarkPackageModule.PACKAGE_MODULE);

    StarlarkThread env =
        StarlarkThread.builder(mutability)
            .setGlobals(Module.createForBuiltins(module.build()))
            .setSemantics(BuckStarlark.BUCK_STARLARK_SEMANTICS)
            .build();
    SkylarkUtils.setPhase(env, SkylarkUtils.Phase.LOADING);
    ParseContext parseContext =
        new ParseContext(
            PackageContext.of(
                NativeGlobber.create(root),
                ImmutableMap.of(),
                PackageIdentifier.create(RepositoryName.DEFAULT, PathFragment.create("my/package")),
                eventHandler,
                ImmutableMap.of()));
    parseContext.setup(env);
    try {
      EvalUtils.exec(buildFileAst, env);
      assertTrue(expectSuccess);
    } catch (EvalException e) {
      eventHandler.handle(Event.error(e.getLocation(), e.getMessage()));
      assertFalse(expectSuccess);
    }
    return parseContext;
  }
}
