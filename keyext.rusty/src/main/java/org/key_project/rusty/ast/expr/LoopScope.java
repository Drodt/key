/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.expr;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.ProgramPrefixUtil;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.rusty.logic.PosInProgram;
import org.key_project.rusty.logic.PossibleProgramPrefix;
import org.key_project.rusty.logic.op.IProgramVariable;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.jspecify.annotations.Nullable;

public class LoopScope implements LoopExpression, PossibleProgramPrefix {
    private final IProgramVariable index;
    private final IProgramVariable returnVar;
    private final BlockExpression block;
    /// Only null for schema Rust
    private final @Nullable FunctionFrame functionFrame;
    private final int prefixLength;

    public LoopScope(IProgramVariable index, IProgramVariable returnVar, BlockExpression block) {
        this.index = index;
        this.returnVar = returnVar;
        this.block = block;
        ProgramPrefixUtil.ProgramPrefixInfo info = ProgramPrefixUtil.computeEssentials(this);
        prefixLength = info.length();
        functionFrame = info.innermostFunctionFrame();
    }

    public LoopScope(ExtList list) {
        index = Objects.requireNonNull(list.removeFirstOccurrence(IProgramVariable.class));
        returnVar = Objects.requireNonNull(list.removeFirstOccurrence(IProgramVariable.class));
        block = Objects.requireNonNull(list.removeFirstOccurrence(BlockExpression.class));
        ProgramPrefixUtil.ProgramPrefixInfo info = ProgramPrefixUtil.computeEssentials(this);
        prefixLength = info.length();
        functionFrame = info.innermostFunctionFrame();
    }

    @Override
    public Type type(Services services) {
        return block.type(services);
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnLoopScope(this);
    }

    public IProgramVariable getIndex() {
        return index;
    }

    public IProgramVariable getReturnVar() {
        return returnVar;
    }

    public BlockExpression getBlock() {
        return block;
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n == 0) {
            return index;
        }
        if (n == 1) {
            return returnVar;
        }
        if (n == 2) {
            return block;
        }
        throw new IndexOutOfBoundsException(n);
    }

    @Override
    public int getChildCount() {
        return 3;
    }

    @Override
    public boolean isPrefix(@UnknownInitialization LoopScope this) {
        assert block != null;
        return block.isPrefix();
    }

    @Override
    public boolean hasNextPrefixElement(@UnknownInitialization LoopScope this) {
        assert block != null;
        return block.getChildCount() != 0 && block.getChild(0) instanceof PossibleProgramPrefix;
    }

    @Override
    public PossibleProgramPrefix getNextPrefixElement(@UnknownInitialization LoopScope this) {
        assert block != null;
        if (hasNextPrefixElement()) {
            return (PossibleProgramPrefix) block.getChild(0);
        } else {
            throw new IndexOutOfBoundsException("No next prefix element " + this);
        }
    }

    @Override
    public PossibleProgramPrefix getLastPrefixElement() {
        return hasNextPrefixElement() ? getNextPrefixElement().getLastPrefixElement() : this;
    }

    @Override
    public ImmutableArray<PossibleProgramPrefix> getPrefixElements() {
        return BlockExpression.computePrefixElements(block);
    }

    @Override
    public PosInProgram getFirstActiveChildPos() {
        return block.getChildCount() == 0 ? PosInProgram.TOP : PosInProgram.TOP.down(2).down(0);
    }

    @Override
    public int getPrefixLength() {
        return prefixLength;
    }

    @Override
    public String toString() {
        return "loop_scope!(" + index + ", " + returnVar + ", " + block + ")";
    }
}
