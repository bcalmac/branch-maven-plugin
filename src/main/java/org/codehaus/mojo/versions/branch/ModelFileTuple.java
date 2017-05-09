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
import org.apache.maven.model.Model;

import java.io.File;
import java.util.Collection;

import static java.util.stream.Collectors.toList;

public class ModelFileTuple {

    private final Model model;
    private final File file;

    public ModelFileTuple(Model model, File file) {
        this.model = model;
        this.file = file;
    }

    public Model getModel() {
        return model;
    }

    public File getFile() {
        return file;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelFileTuple that = (ModelFileTuple) o;
        return Objects.equal(model, that.model) &&
                Objects.equal(file, that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(model, file);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("model", model)
                .add("file", file)
                .toString();
    }

    public static Collection<Model> models(Collection<ModelFileTuple> tuples) {
        return tuples.stream().map(ModelFileTuple::getModel).collect(toList());
    }
}
