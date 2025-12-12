/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.expr;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.ProgramPrefixUtil;
import org.key_project.rusty.ast.abstraction.TupleType;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.rusty.logic.PosInProgram;
import org.key_project.rusty.logic.PossibleProgramPrefix;
import org.key_project.rusty.logic.op.IProgramVariable;
import org.key_project.util.collection.ImmutableArray;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.jspecify.annotations.NonNull;

public class PanicFrame implements Expr, PossibleProgramPrefix {
    private final IProgramVariable panicVar;
    private final BlockExpression body;

    private final PosInProgram firstActiveChildPos;
    private final int prefixLength;

    public PanicFrame(
            IProgramVariable panicVar,
            BlockExpression body) {
        this.panicVar = panicVar;
        this.body = body;

        firstActiveChildPos = body.getChildCount() == 0 ? PosInProgram.TOP
                : PosInProgram.TOP.down(1).down(0);
        var info = ProgramPrefixUtil.computeEssentials(this);
        prefixLength = info.length();
    }

    @Override
    public Type type(Services services) {
        return TupleType.UNIT;
    }

    @Override
    public boolean isPrefix() {
        return body.getChildCount() != 0;
    }

    @Override
    public boolean hasNextPrefixElement(@UnknownInitialization PanicFrame this) {
        return body.getChildCount() != 0 && body.getChild(0) instanceof PossibleProgramPrefix;
    }

    @Override
    public @NonNull PossibleProgramPrefix getNextPrefixElement(
            @UnknownInitialization PanicFrame this) {
        assert body != null;
        if (hasNextPrefixElement())
            return (PossibleProgramPrefix) body.getChild(0);
        throw new IndexOutOfBoundsException("No next prefix element " + this);
    }

    @Override
    public @NonNull PossibleProgramPrefix getLastPrefixElement() {
        return hasNextPrefixElement() ? getNextPrefixElement().getLastPrefixElement() : this;
    }

    @Override
    public @NonNull ImmutableArray<PossibleProgramPrefix> getPrefixElements() {
        return BlockExpression.computePrefixElements(body);
    }

    @Override
    public @NonNull PosInProgram getFirstActiveChildPos() {
        return firstActiveChildPos;
    }

    @Override
    public int getPrefixLength() {
        return prefixLength;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnPanicFrame(this);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0)
            return panicVar;
        if (n == 1)
            return body;
        throw new IndexOutOfBoundsException("PanicFrame has 2 children: Got " + n);
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    public IProgramVariable getPanicVar() {
        return panicVar;
    }

    public BlockExpression getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "panic_frame!(" + panicVar + ", " + body + ")";
    }
}
