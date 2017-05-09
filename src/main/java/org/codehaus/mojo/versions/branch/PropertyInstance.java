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

public class PropertyInstance {

    private final String module;
    private final String profile; // null means no profile
    private final String name;
    private final String value;

    public PropertyInstance(String module, String name, String value) {
        this(module, null, name, value);
    }

    public PropertyInstance(String module, String profile, String name, String value) {
        this.module = module;
        this.profile = profile;
        this.name = name;
        this.value = value;
    }

    public String getModule() {
        return module;
    }

    public String getProfile() {
        return profile;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyInstance that = (PropertyInstance) o;
        return Objects.equal(module, that.module) &&
                Objects.equal(profile, that.profile) &&
                Objects.equal(name, that.name) &&
                Objects.equal(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(module, profile, name, value);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("module", module)
                .add("profile", profile)
                .add("name", name)
                .add("value", value)
                .toString();
    }
}
