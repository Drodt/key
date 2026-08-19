/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic.op.sv;

import org.key_project.logic.Name;
import org.key_project.logic.TerminalSyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.logic.RustyDLTheory;

import org.jspecify.annotations.NonNull;

public class SkolemTermSV extends OperatorSV implements TerminalSyntaxElement {
    /// whether the constants created for this schema variable are definitional symbols
    private final boolean definitional;

    /// Creates a new schema variable that is used as placeholder for skolem terms.
    ///
    /// @param name the Name of the SchemaVariable
    /// @param sort the Sort of the SchemaVariable and the matched type allowed to match a list of
    /// program constructs
    /// @param definitional whether the created constants are definitional symbols, declared as
    /// `\skolemTerm[definitional]`
    SkolemTermSV(Name name, Sort sort, boolean definitional) {
        super(name, sort, true, false);
        assert sort != RustyDLTheory.UPDATE;
        this.definitional = definitional;
    }

    @Override
    public @NonNull String toString() {
        return toString(sort() + " skolem term");
    }

    @Override
    public boolean isSkolemTerm() {
        return true;
    }

    /// @return whether the constants created for this schema variable are definitional symbols
    public boolean isDefinitional() {
        return definitional;
    }
}
