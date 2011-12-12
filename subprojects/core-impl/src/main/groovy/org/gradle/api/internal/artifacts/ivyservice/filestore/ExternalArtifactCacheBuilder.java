/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.api.internal.artifacts.ivyservice.filestore;

import org.gradle.api.internal.artifacts.ivyservice.ArtifactCacheMetaData;
import org.gradle.api.internal.artifacts.ivyservice.artifactcache.ArtifactFileStore;

import java.io.File;

public class ExternalArtifactCacheBuilder {
    private final CompositeExternalArtifactCache composite = new CompositeExternalArtifactCache();
    private final File rootCachesDirectory;

    public ExternalArtifactCacheBuilder(ArtifactCacheMetaData artifactCacheMetaData) {
        this.rootCachesDirectory = artifactCacheMetaData.getCacheDir().getParentFile();
    }

    public void addCurrent(ArtifactFileStore artifactFileStore) {
        composite.addExternalArtifactCache(artifactFileStore.asExternalArtifactCache());
    }

    public void addMilestone6() {
        addExternalCache(new File(rootCachesDirectory, "artifacts-4"), "[organisation]/[module](/[branch])/*/[type]s/[artifact]-[revision](-[classifier])(.[ext])");
        addExternalCache(new File(rootCachesDirectory, "artifacts-4"), "[organisation]/[module](/[branch])/*/pom.originals/[artifact]-[revision](-[classifier])(.[ext])");
    }

    private void addExternalCache(File baseDir, String pattern) {
        if (baseDir.exists()) {
            composite.addExternalArtifactCache(new PatternBasedExternalArtifactCache(baseDir, pattern));
        }
    }

    public  ExternalArtifactCache getExternalArtifactCache() {
        return composite;
    }
}
