/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.rusty.Services;

public record Field(Name name, Type type) {
    public Field instantiate(Map<GenericTyParam, GenericTyArg> instMap, Services services) {
        var arg = instMap.get(type);
        if (arg == null) {
            return this;
        }
        return new Field(name, type.instantiate(instMap, services));
    }
}
