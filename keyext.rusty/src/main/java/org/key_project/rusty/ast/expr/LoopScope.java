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

public class LoopScope implements LoopExpression, PossibleProgramPrefix {
    private final IProgramVariable index;
    private final BlockExpression block;
    private final FunctionFrame functionFrame;
    private final int prefixLength;

    public LoopScope(IProgramVariable index, BlockExpression block) {
        this.index = index;
        this.block = block;
        ProgramPrefixUtil.ProgramPrefixInfo info = ProgramPrefixUtil.computeEssentials(this);
        prefixLength = info.length();
        functionFrame = Objects.requireNonNull(info.innermostFunctionFrame());
    }

    public LoopScope(ExtList list) {
        index = Objects.requireNonNull(list.get(IProgramVariable.class));
        block = Objects.requireNonNull(list.get(BlockExpression.class));
        ProgramPrefixUtil.ProgramPrefixInfo info = ProgramPrefixUtil.computeEssentials(this);
        prefixLength = info.length();
        functionFrame = Objects.requireNonNull(info.innermostFunctionFrame());
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

    public BlockExpression getBlock() {
        return block;
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n == 0) {
            return index;
        }
        if (n == 1) {
            return block;
        }
        throw new IndexOutOfBoundsException(n);
    }

    @Override
    public int getChildCount() {
        return 2;
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
        return block.getChildCount() == 0 ? PosInProgram.TOP : PosInProgram.TOP.down(1).down(0);
    }

    @Override
    public int getPrefixLength() {
        return prefixLength;
    }

    @Override
    public String toString() {
        return "loop_scope!(" + index + ", " + block + ")";
    }
}
