/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic;

import org.key_project.rusty.logic.op.BoundVariable;

import org.jspecify.annotations.Nullable;

public class LogicVariableTable {
    public static final LogicVariableTable EMPTY = new EmptyLogicVariableTable();

    private final BoundVariable localVar;

    private final @Nullable LogicVariableTable parent;

    public LogicVariableTable(LogicVariableTable parent, BoundVariable localVar) {
        this.parent = parent;
        this.localVar = localVar;
    }

    public boolean contains(BoundVariable var) {
        return localVar.equals(var) || parent.contains(var);
    }

    public boolean containsLocally(BoundVariable var) {
        return localVar.equals(var);
    }

    public LogicVariableTable extend(BoundVariable var) {
        return new LogicVariableTable(this, var);
    }

    public LogicVariableTable parent() {
        return parent;
    }

    public @Nullable BoundVariable getBoundVariable(int idx) {
        if (idx == 1) {
            return localVar;
        }
        return parent.getBoundVariable(idx - 1);
    }

    @Override
    public String toString() {
        return localVar + "\nparent:" + parent;
    }

    private static class EmptyLogicVariableTable extends LogicVariableTable {
        public EmptyLogicVariableTable() { super(null, null); }

        @Override
        public boolean contains(BoundVariable var) {
            return false;
        }

        @Override
        public boolean containsLocally(BoundVariable var) {
            return false;
        }

        @Override
        public @Nullable BoundVariable getBoundVariable(int idx) {
            return null;
        }

        @Override
        public String toString() {
            return "empty";
        }
    }
}
