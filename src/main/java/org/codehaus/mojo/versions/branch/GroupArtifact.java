/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.mojo.versions.branch;


import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.apache.maven.model.Dependency;

public class GroupArtifact {

    private final String groupId;
    private final String artifactId;


    public GroupArtifact(Dependency dependency) {
        this(dependency.getGroupId(), dependency.getArtifactId());
    }

    private GroupArtifact(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupArtifact that = (GroupArtifact) o;
        return Objects.equal(groupId, that.groupId) &&
                Objects.equal(artifactId, that.artifactId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(groupId, artifactId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("groupId", groupId)
                .add("artifactId", artifactId)
                .toString();
    }
}
