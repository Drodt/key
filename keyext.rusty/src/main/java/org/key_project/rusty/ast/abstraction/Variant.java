/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.rusty.Services;
import org.key_project.util.collection.ImmutableArray;

public record Variant(Name name, ImmutableArray<Field> fields) {
    public Variant instantiate(Map<GenericTyParam, GenericTyArg> instMap, Services services) {
        var fields = new Field[this.fields.size()];
        for (int i = 0; i < this.fields.size(); i++) {
            fields[i] = this.fields.get(i).instantiate(instMap, services);
        }
        return new Variant(name, new ImmutableArray<>(fields));
    }
}
