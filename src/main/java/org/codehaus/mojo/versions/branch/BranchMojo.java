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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.versions.AbstractVersionsUpdaterMojo;
import org.codehaus.mojo.versions.api.ArtifactVersions;
import org.codehaus.mojo.versions.api.PomHelper;
import org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.codehaus.mojo.versions.api.PomHelper.getRawModel;

/**
 * Sets the current project's version to include a given SCM branch and then propagate to child modules and
 * dependencies as necessary. For now it only supports dependencies with the version specified as a property.
 */
@Mojo(name = "branch", requiresDirectInvocation = true, aggregator = true)
public class BranchMojo extends AbstractVersionsUpdaterMojo {

    private static final String SNAPSHOT = "-SNAPSHOT";
    private static final Pattern JIRA_ID_PATTERN = Pattern.compile("[A-Z]+-\\d+");

    /**
     * The current branch, determined externally.
     *
     * @since 1.0
     */
    @Parameter(property = "branch")
    private String branch;

    /**
     * By default foo-1.0-SNAPSHOT becomes foo-1.0-BRANCH-SNAPSHOT. When prepend is enabled, foo-1.0-SNAPSHOT becomes
     * BRANCH-foo-1.0-SNAPSHOT. This can be useful when you have dependency ranges and you want the branched versions
     * to be outside of those ranges.
     */
    @Parameter(property = "prepend", defaultValue = "false")
    private boolean prepend;

    /**
     * The branch after removing leading path elements.
     */
    private String trimmedBranch;

    private Set<PropertyInstance> propertyChanges;

    /**
     * Called when this mojo is executed.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (getProject().getOriginalModel().getVersion() == null) {
            throw new MojoExecutionException("Project version is inherited from parent; There's not enough information to re-write the POM.");
        }

        if (StringUtils.isBlank(branch)) {
            getLog().info("POM re-write aborted; the 'branch' parameter was not specified.");
            return;
        }

        trimmedBranch = truncateBranch(trimPath(branch));
        getLog().info("Branch parameter " + branch + " was trimmed to " + trimmedBranch + " for use inside the version.");

        String oldVersion = project.getVersion();
        String newVersion = branchedVersion(oldVersion);
        if (newVersion.equals(oldVersion)) {
            getLog().info("POM re-write aborted; the current version already includes the branch: " + oldVersion);
            return;
        }

        try {
            Map<String, ModelFileTuple> reactor = loadModels(getProject(), getLog());
            propertyChanges = BranchHelper.propertyChanges(ModelFileTuple.models(reactor.values()), this::existsOnBranch);
            for (ModelFileTuple tuple : reactor.values()) {
                processFile(tuple);
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void processFile(ModelFileTuple tuple) throws MojoExecutionException, MojoFailureException {
        try {
            StringBuilder input = PomHelper.readXmlFile(tuple.getFile());
            ModifiedPomXMLEventReader newPom = newModifiedPomXER(input);

            updateFile(newPom, tuple.getModel());

            if (newPom.isModified()) {
                writeFile(tuple.getFile(), input);
            }
        } catch (IOException | XMLStreamException e) {
            getLog().error(e);
        }

    }


    private void updateFile(ModifiedPomXMLEventReader pom, Model model) throws MojoExecutionException, MojoFailureException, XMLStreamException {
        updateProject(pom, model);
        updateParent(pom, model);
        updateDependencies(pom, model);
    }

    private void updateProject(ModifiedPomXMLEventReader pom, Model model) throws XMLStreamException, MojoExecutionException {
        String version = model.getVersion();
        if (ArtifactUtils.isSnapshot(version)) {
            final String newVersion = branchedVersion(version);
            boolean success = PomHelper.setProjectVersion(pom, newVersion);
            if (success) {
                getLog().info(">>> Updated " + model.getArtifactId() + " project version from " + version + " to " + newVersion);
            }
        }
    }

    private void updateParent(ModifiedPomXMLEventReader pom, Model model) throws XMLStreamException, MojoExecutionException {
        Parent parent = model.getParent();
        if (parent != null) {
            String version = parent.getVersion();
            if (ArtifactUtils.isSnapshot(version)) {
                final String newVersion = branchedVersion(version);
                boolean success = PomHelper.setProjectParentVersion(pom, newVersion);
                if (success) {
                    getLog().info(">>> Updated " + model.getArtifactId() + " parent's version from " + version + " to " + newVersion);
                }
            }
        }
    }

    private void updateDependencies(ModifiedPomXMLEventReader pom, Model model) throws MojoExecutionException, XMLStreamException {
        for (PropertyInstance property : propertyChanges) {
            if (property.getModule().equals(moduleId(model))) {
                final String currentValue = property.getValue();
                final String newValue = branchedVersion(currentValue);
                boolean success = PomHelper.setPropertyVersion(pom, property.getProfile(), property.getName(), newValue);
                if (success) {
                    getLog().info(">>> Updated  property ${" + property.getName() + "} from " + currentValue + " to " + newValue);
                }
            }
        }
    }

    // Adapted from PomHelper.getReactorModels()
    private static Map<String, ModelFileTuple> loadModels(MavenProject project, Log logger) throws IOException {
        Map<String, ModelFileTuple> result = new LinkedHashMap<>();
        Model model = getRawModel(project);
        result.put(moduleId(model), new ModelFileTuple(model, project.getFile()));
        result.putAll(loadModels("", model, project, logger));
        return result;
    }

    // Adapted from PomHelper.getReactorModels()
    private static Map<String, ModelFileTuple> loadModels(String path, Model model, MavenProject project, Log logger)
            throws IOException {
        if (path.length() > 0 && !path.endsWith("/")) {
            path += '/';
        }
        Map<String, ModelFileTuple> result = new LinkedHashMap<>();
        Map<String, ModelFileTuple> childResults = new LinkedHashMap<>();

        File baseDir = path.length() > 0 ? new File(project.getBasedir(), path) : project.getBasedir();

        Set<String> childModules = PomHelper.getAllChildModules(model, logger);

        PomHelper.removeMissingChildModules(logger, baseDir, childModules);

        for (String childModuleName : childModules) {
            String childModulePath = path + childModuleName;

            File childModuleDir = new File(baseDir, childModuleName);

            File childModuleProjectFile;

            if (childModuleDir.isDirectory()) {
                childModuleProjectFile = new File(childModuleDir, "pom.xml");
            } else {
                // i don't think this should ever happen... but just in case
                // the module references the file-name
                childModuleProjectFile = childModuleDir;
            }

            try {
                // the aim of this goal is to fix problems when the project cannot be parsed by Maven
                // so we have to work with the raw model and not the interpolated parsed model from maven
                Model childModuleModel = getRawModel(childModuleProjectFile);
                result.put(moduleId(childModuleModel), new ModelFileTuple(childModuleModel, childModuleProjectFile));
                childResults.putAll(loadModels(childModulePath, childModuleModel, project, logger));
            } catch (IOException e) {
                logger.debug("Could not parse " + childModuleProjectFile.getPath(), e);
            }
        }
        result.putAll(childResults); // more efficient update order if all children are added after siblings
        return result;
    }

    private String branchedVersion(String currentVersion) {
        if (currentVersion.contains(trimmedBranch)) {
            return currentVersion;
        } else if (prepend) {
            return trimmedBranch + "-" + currentVersion;
        } else {
            return currentVersion.replace(SNAPSHOT, "-" + trimmedBranch + SNAPSHOT);
        }
    }

    private Artifact branchedArtifact(GroupArtifactVersion artifact) {
        return new DefaultArtifact(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                VersionRange.createFromVersion(branchedVersion(artifact.getVersion())),
                "compile", "pom", "", null);

    }

    private boolean existsOnBranch(GroupArtifactVersion gav) {
        Artifact artifact = branchedArtifact(gav);
        try {
            ArtifactVersions versions = this.getHelper().lookupArtifactVersions(artifact, false);
            getLog().debug("Available versions for " + artifact + ": " + Arrays.toString(versions.getVersions(true)));
            if (versions.containsVersion(artifact.getVersion())) {
                getLog().info("Branched artifact resolved successfully: " + artifact);
                return true;
            } else {
                getLog().info("Branched artifact not found: " + artifact + ". The corresponding property will not be updated.");
                return false;
            }
        } catch (MojoExecutionException | ArtifactMetadataRetrievalException e) {
            getLog().info("Artifact resolution failed for " + artifact + ". The POM re-write will be aborted.");
            throw new RuntimeException(e);
        }
    }

    private static String truncateBranch(String branch) {
        return BranchHelper.find(branch, JIRA_ID_PATTERN).orElse(StringUtils.left(branch, 10));
    }

    private static String trimPath(String branch) {
        return branch.replaceFirst(".*/", "");
    }

    private static String moduleId(Model model) {
        return model.getArtifactId();
    }

    @Override
    protected void update(ModifiedPomXMLEventReader pom) throws MojoExecutionException, MojoFailureException, XMLStreamException, ArtifactMetadataRetrievalException {
        throw new UnsupportedOperationException();
    }
}
