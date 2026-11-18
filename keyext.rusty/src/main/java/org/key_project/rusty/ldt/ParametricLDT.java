/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.ldt;

import java.util.Map;
import java.util.TreeMap;

import org.key_project.logic.Name;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.sort.ParametricSortDecl;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// An "LDT" or "language data type" class corresponds to a standard rule file shipped with KeY.
/// Usually, this rule file declares a sort (such as "int") and a number of operators. The LDT class
/// provides a programming interface to access these entities, and it assists the type converter in
/// handling them.
///
/// This class is for parametric types, e.g., `Set`.
@NullMarked
public abstract class ParametricLDT extends AbstractLDT {
    /// the main parametric sort associated with the LDT
    private final @Nullable ParametricSortDecl sort;

    protected ParametricLDT(Name name, Services services) {
        super(name);
        sort = services.getNamespaces().parametricSorts().lookup(name);
        if (sort == null) {
            throw new RuntimeException("LDT " + name + " not found.\n"
                + "It seems that there are definitions missing from the .key files.");
        }
    }

    public static Map<Name, ParametricLDT> getNewLDTInstances(Services s) {
        // TreeMap ensures the map is sorted according to the natural order of its keys.
        Map<Name, ParametricLDT> ret = new TreeMap<>();

        ret.put(ArrayLDT.NAME, new ArrayLDT(s));
        ret.put(FieldLDT.NAME, new FieldLDT(s));
        for (int i = 1; i <= 12; ++i) {
            var t = new TupleLDT(i, s);
            ret.put(t.name(), t);
        }
        ret.put(SRefLDT.NAME, new SRefLDT(s));
        ret.put(MRefLDT.NAME, new MRefLDT(s));
        ret.put(GhostLDT.NAME, new GhostLDT(s));
        ret.put(SeqLDT.NAME, new SeqLDT(s));

        return ret;
    }

    @Override
    public final String toString() {
        return "Parametric LDT " + name() + " (" + targetSort() + ")";
    }

    /// Returns the sort associated with the LDT.
    public final ParametricSortDecl targetSort() {
        return sort;
    }
}
