/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.ty;

import java.util.Objects;
import java.util.stream.Collectors;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.abstraction.TupleType;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.util.collection.ImmutableList;

public class TupleRustType implements RustType {
    private final ImmutableList<RustType> types;
    private final Type type;

    public static TupleRustType UNIT = new TupleRustType();

    public TupleRustType(ImmutableList<RustType> types, Services services) {
        this.types = types;
        this.type =
            TupleType.getInstance(types.stream().map(RustType::type).collect(Collectors.toList()),
                services);
    }

    private TupleRustType() {
        types = ImmutableList.nil();
        type = TupleType.UNIT;
    }

    @Override
    public Type type() {
        return type;
    }

    public ImmutableList<RustType> getTypes() {
        return types;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnTupleRustType(this);
    }

    @Override
    public SyntaxElement getChild(int n) {
        return Objects.requireNonNull(types.get(n));
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
