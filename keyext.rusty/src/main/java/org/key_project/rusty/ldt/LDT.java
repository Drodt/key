/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.ldt;

import java.util.Map;
import java.util.TreeMap;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.expr.BinaryExpression;
import org.key_project.rusty.ast.expr.LiteralExpression;
import org.key_project.rusty.logic.RustyDLTheory;

import org.jspecify.annotations.Nullable;

public abstract class LDT extends AbstractLDT {
    /// the main sort associated with the LDT
    private final @Nullable Sort sort;

    // -------------------------------------------------------------------------
    // constructors
    // -------------------------------------------------------------------------

    protected LDT(Name name, Services services) {
        super(name);
        var sort = services.getNamespaces().sorts().lookup(name);
        if (sort != null) {
            this.sort = sort;
        } else {
            throw new RuntimeException("LDT " + name + " not found.\n"
                + "It seems that there are definitions missing from the .key files.");
        }
    }

    protected LDT(Name name) {
        super(name);
        this.sort = null;
    }

    protected LDT(Name name, Sort sort) {
        super(name);
        this.sort = sort;
    }

    public static Map<Name, LDT> getNewLDTInstances(Services s) {
        // TreeMap ensures the map is sorted according to the natural order of its keys.
        Map<Name, LDT> ret = new TreeMap<>();

        ret.put(RustyDLTheory.NAME, new RustyDLTheory());
        ret.put(IntLDT.NAME, new IntLDT(s));
        ret.put(BoolLDT.NAME, new BoolLDT(s));
        ret.put(CharLDT.NAME, new CharLDT(s));
        ret.put(StrLDT.NAME, new StrLDT(s));
        ret.put(NeverLDT.NAME, new NeverLDT(s));

        return ret;
    }

    // -------------------------------------------------------------------------
    // protected methods
    // -------------------------------------------------------------------------

    public abstract @Nullable Term translateLiteral(LiteralExpression lit, Services services);

    public abstract @Nullable Function getFunctionFor(BinaryExpression.Operator op,
            Services services);

    public abstract boolean isResponsible(BinaryExpression.Operator op, Term[] subs,
            Services services);

    public abstract boolean isResponsible(BinaryExpression.Operator op, Term sub,
            Services services);

    public abstract boolean isResponsible(BinaryExpression.Operator op, Term left, Term right,
            Services services);

    /// Returns the sort associated with the LDT.
    public final Sort targetSort() {
        return sort;
    }

    /// get the function in this LDT for an operation identified by generic operationName. If the
    /// LDT
    /// does not support this named function, it should return null.
    /// This is used to resolve overloaded symbols.
    /// For example: "+" may map to "add" for integers, and to "addFloat" for floats.
    ///
    /// @param operationName non-null operationName for a generic function
    /// @param services services to use
    /// @return reference to the respective LDT-specific function for the operation, null if not
    /// available
    public @Nullable Function getFunctionFor(String operationName, Services services) {
        // by default an LDT does not support overloaded symbols
        return null;
    }
}
