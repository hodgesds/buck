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

package com.facebook.buck.features.python;

import com.facebook.buck.core.rulekey.AddsToRuleKey;
import com.facebook.buck.core.rules.BuildRule;
import com.facebook.buck.core.sourcepath.SourcePath;
import java.util.function.Consumer;

/**
 * Interface representing the modules, resources, etc. that dependencies contribute to Python
 * binaries, used to model how these components should be handled by {@link BuildRule}s (e.g. how
 * they're hashed into rule keys).
 */
public interface PythonComponents extends AddsToRuleKey {

  /** Run {@code consumer} on all {@link SourcePath}s contained in this object. */
  void forEachInput(Consumer<SourcePath> consumer);
}