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

import com.google.common.collect.ImmutableList;
import org.apache.maven.model.Model;
import org.codehaus.mojo.versions.api.PomHelper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchHelperTest {

    private static final Pattern LEADING_NUMBER_PATTERN = Pattern.compile("^\\d+");

    @Test
    public void simple() throws Exception {
        Collection<Model> models = ImmutableList.of(
                model("root.xml"),
                model("module.xml")
        );

        Set<PropertyInstance> changes = BranchHelper.propertyChanges(models, BranchHelperTest::moduloThreeResolver);

        // Assert that changes only consist of properties with index 3k+1.
        assertThat(changes).containsExactlyInAnyOrder(
                new PropertyInstance("branch", null, "lib1", "1.0-SNAPSHOT"),
                new PropertyInstance("branch", null, "lib4", "4.0-SNAPSHOT"),
                new PropertyInstance("branch", null, "lib7", "7.0-SNAPSHOT"),
                new PropertyInstance("branch", "root-profile", "lib10", "10.0-SNAPSHOT"),
                new PropertyInstance("branch", "root-profile", "lib13", "13.0-SNAPSHOT"),
                new PropertyInstance("branch", "root-profile", "lib16", "16.0-SNAPSHOT"),
                new PropertyInstance("branch-module", null, "lib19", "19.0-SNAPSHOT"),
                new PropertyInstance("branch-module", "module-profile", "lib22", "22.0-SNAPSHOT")
        );
    }

    private static Model model(String resource) throws URISyntaxException, IOException {
        return PomHelper.getRawModel(new File(BranchHelperTest.class.getResource(resource).toURI()));
    }

    /**
     * 3k  :    non-SNAPSHOT            -> false
     * 3k+1:    branched SNAPSHOT       -> true
     * 3k+2:    non-branched SNAPSHOT   -> false
     * else:                            -> false
     */
    private static boolean moduloThreeResolver(GroupArtifactVersion artifact) {
        return firstLeadingNumber(artifact).map(x -> x % 3 == 1).orElse(false);
    }

    private static Optional<Integer> firstLeadingNumber(GroupArtifactVersion artifact) {
        return BranchHelper.find(artifact.getVersion(), LEADING_NUMBER_PATTERN).map(Integer::valueOf);
    }

}