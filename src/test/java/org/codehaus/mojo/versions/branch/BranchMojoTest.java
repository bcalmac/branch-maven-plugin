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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BranchMojoTest {

    @Test
    public void sanitizeBranch() {
        assertEquals("ab-12_C3",BranchMojo.sanitizeBranch("ab!@-/12^#_\\C*&3\"'"));
    }

    @Test
    public void truncateBranch() {
        assertEquals("ABC-123", BranchMojo.truncateBranch("ABC-123-456-foo"));
        assertEquals("ABC-123", BranchMojo.truncateBranch("890-ABC-123-456-foo"));
        assertEquals("ABC-123", BranchMojo.truncateBranch("abc-ABC-123-456-foo"));
        assertEquals("abc-123-de", BranchMojo.truncateBranch("abc-123-def-567"));
        assertEquals("abc", BranchMojo.truncateBranch("abc"));
    }

    @Test
    public void trimPath() {
        assertEquals("foo", BranchMojo.trimPath("foo"));
        assertEquals("foo", BranchMojo.trimPath("/foo"));
        assertEquals("foo", BranchMojo.trimPath("a/foo"));
        assertEquals("foo", BranchMojo.trimPath("a/b/foo"));
    }
}