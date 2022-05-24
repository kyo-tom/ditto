/*
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
package com.kyotom.ditto.parser.tree;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

public class FileFormat
        extends Statement
{
    private final Identifier storedAs;

    public FileFormat(Identifier storedAs)
    {
        this(Optional.empty(), storedAs);
    }

    public FileFormat(NodeLocation location, Identifier storedAs)
    {
        this(Optional.of(location), storedAs);
    }

    private FileFormat(Optional<NodeLocation> location, Identifier storedAs)
    {
        super(location);
        this.storedAs = requireNonNull(storedAs, "name is null");
    }

    public Identifier getStoredAs() {
        return storedAs;
    }

    @Override
    public <R, C> R accept(AstVisitor<R, C> visitor, C context)
    {
        return visitor.visitFileFormat(this, context);
    }

    @Override
    public List<Node> getChildren()
    {
        return ImmutableList.<Node>builder()
                .build();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(storedAs);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        FileFormat o = (FileFormat) obj;
        return Objects.equals(storedAs, o.storedAs);
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("storedAs", storedAs)
                .toString();
    }

    public boolean isKuduTable() {
        return "KUDU".equalsIgnoreCase(storedAs.getValue());
    }
}
