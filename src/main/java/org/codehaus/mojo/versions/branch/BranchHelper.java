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

import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toSet;

/**
 * Analyzes a list of POMs and identifies the properties for SNAPSHOT dependencies that need to be re-written.
 */
public class BranchHelper {

    private static final Pattern REFERENCE_PATTERN = Pattern.compile("\\$\\{(.*)}");

    // Does the artifact exist in the repository?
    private final Predicate<GroupArtifactVersion> artifactResolver;

    // property -> (groupId, artifactId) dependency
    private final SetMultimap<String, GroupArtifact> dependencies = HashMultimap.create();
    // property -> referenced property
    private final Map<String, String> propertyRefs = Maps.newHashMap();
    // property -> (name, value)
    private final SetMultimap<String, PropertyInstance> properties = HashMultimap.create();

    public static Set<PropertyInstance> propertyChanges(Collection<Model> models, Predicate<GroupArtifactVersion> artifactResolver) {
        BranchHelper instance = new BranchHelper(artifactResolver);
        models.forEach(instance::collectProperties);
        models.forEach(instance::collectDependencies);
        return instance.getChanges();
    }

    private BranchHelper(Predicate<GroupArtifactVersion> artifactResolver) {
        this.artifactResolver = artifactResolver;
    }

    private void collectProperties(Model model) {
        Properties projectProperties = model.getProperties();
        if (projectProperties != null) {
            collectProperties(model.getArtifactId(), null, projectProperties);
        }
        for (Profile profile : model.getProfiles()) {
            Properties profileProperties = profile.getProperties();
            if (profileProperties != null) {
                collectProperties(model.getArtifactId(), profile.getId(), profileProperties);
            }
        }
    }

    private void collectProperties(String module, String profile, Properties modelProperties) {
        for (Map.Entry<Object, Object> entry : modelProperties.entrySet()) {
            String name = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (!Strings.isNullOrEmpty(value)) {
                if (ArtifactUtils.isSnapshot(value)) {
                    properties.put(name, new PropertyInstance(module, profile, name, value));
                } else {
                    parseReference(value).ifPresent(ref -> propertyRefs.put(name, ref));
                }
            }
        }
    }

    private Optional<String> parseReference(String value) {
        if (!Strings.isNullOrEmpty(value)) {
            Matcher matcher = REFERENCE_PATTERN.matcher(value);
            if (matcher.matches()) {
                return Optional.of(matcher.group(1));
            }
        }
        return Optional.empty();
    }

    private void collectDependencies(Model model) {
        model.getDependencies().forEach(this::collectDependency);
        if (model.getDependencyManagement() != null) {
            model.getDependencyManagement().getDependencies().forEach(this::collectDependency);
        }
        for (Profile profile : model.getProfiles()) {
            profile.getDependencies().forEach(this::collectDependency);
            if (profile.getDependencyManagement() != null) {
                profile.getDependencyManagement().getDependencies().forEach(this::collectDependency);
            }
        }
    }

    private void collectDependency(Dependency dependency) {
        parseReference(dependency.getVersion())
                .flatMap(this::resolveReference)
                .ifPresent(x -> dependencies.put(x, new GroupArtifact(dependency)));
    }

    private Optional<String> resolveReference(String reference) {
        String property = reference;
        while (!properties.containsKey(property)) {
            property = propertyRefs.get(property);
            if (property == null) {
                return Optional.empty();
            }
        }
        return Optional.of(property);
    }

    private Set<PropertyInstance> getChanges() {
        return properties.values().parallelStream()
                .filter(this::validateBranchedArtifacts)
                .collect(toSet());
    }

    private boolean validateBranchedArtifacts(PropertyInstance property) {
        Set<GroupArtifact> artifacts = dependencies.get(property.getName());
        return !artifacts.isEmpty() && artifacts.stream().allMatch(artifact ->
                artifactResolver.test(new GroupArtifactVersion(artifact, property.getValue()))
        );
    }

    static Optional<String> find(String string, Pattern pattern) {
        if (!Strings.isNullOrEmpty(string)) {
            Matcher matcher = pattern.matcher(string);
            if (matcher.find()) {
                return Optional.of(matcher.group());
            }
        }
        return Optional.empty();
    }
}
