/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.ty;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.RustyProgramElement;
import org.key_project.rusty.ast.SourceData;
import org.key_project.rusty.ast.abstraction.SchemaType;
import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.rusty.rule.MatchConditions;
import org.key_project.rusty.rule.inst.SVInstantiations;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// A schema variable standing for a type in SchemaRust
public record SchemaRustType(SchemaType type) implements RustType {
    @Override
    public void visit(Visitor v) {
        v.performActionOnSchemaRustType(this);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException(getClass() + " has no children");
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String toString() {
        return type.toString();
    }

    @Override
    public @Nullable MatchConditions match(SourceData source, @Nullable MatchConditions mc) {
        final Services services = source.getServices();
        final RustyProgramElement src = source.getSource();
        // TODO: move this somewhere more general
        if (src == null)
            return null;
        SVInstantiations instantiations = Objects.requireNonNull(mc).getInstantiations();

        final Object instant = instantiations.getInstantiation(type.sv());
        if (instant == null) {
            instantiations = instantiations.add(type.sv(), src, services);
            mc = mc.setInstantiations(instantiations);
            // TODO: is this true?
            assert mc != null;
        } else if (!instant.equals(src)) {
            return null;
        }
        source.next();
        return mc;
    }
}
