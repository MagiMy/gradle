/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.buildinit.plugins.internal;

import org.gradle.buildinit.plugins.internal.modifiers.BuildInitDsl;
import org.gradle.buildinit.plugins.internal.modifiers.BuildInitTestFramework;
import org.gradle.internal.file.PathToFileResolver;

import java.util.Collections;
import java.util.Set;

public class BasicTemplateBasedProjectInitDescriptor implements ProjectInitDescriptor {
    private final PathToFileResolver fileResolver;

    public BasicTemplateBasedProjectInitDescriptor(PathToFileResolver fileResolver) {
        this.fileResolver = fileResolver;
    }

    @Override
    public void generate(InitSettings settings) {
        new BuildScriptBuilder(settings.getDsl(), fileResolver, "build")
            .fileComment("This is a general purpose Gradle build.\n"
                + "Learn how to create Gradle builds at https://guides.gradle.org/creating-new-gradle-builds/")
            .create()
            .generate();
    }

    @Override
    public boolean supports(BuildInitDsl dsl) {
        return true;
    }

    @Override
    public BuildInitTestFramework getDefaultTestFramework() {
        return BuildInitTestFramework.NONE;
    }

    @Override
    public Set<BuildInitTestFramework> getTestFrameworks() {
        return Collections.singleton(BuildInitTestFramework.NONE);
    }
}
