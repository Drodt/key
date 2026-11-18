/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.ldt;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.expr.BinaryExpression;
import org.key_project.rusty.ast.expr.LiteralExpression;
import org.key_project.rusty.logic.op.ParametricFunctionDecl;
import org.key_project.rusty.logic.op.ParametricFunctionInstance;

import org.jspecify.annotations.Nullable;

public final class SeqLDT extends ParametricLDT {
    public static final Name NAME = new Name("Seq");

    private final ParametricFunctionDecl seqGet;
    private final ParametricFunctionDecl seqLen;
    private final ParametricFunctionDecl seqIndexOf;
    private final ParametricFunctionDecl seqGetOutside;
    private final ParametricFunctionDecl seqEmpty;
    private final ParametricFunctionDecl seqSingleton;
    private final ParametricFunctionDecl seqConcat;
    private final ParametricFunctionDecl seqSub;
    private final ParametricFunctionDecl seqReverse;
    private final ParametricFunctionDecl seqUpd;
    private final ParametricFunctionDecl seqDef;
    private final ParametricFunctionDecl seqSwap;
    private final ParametricFunctionDecl seqRemove;

    private final Function seqNPermInv;

    public SeqLDT(Services services) {
        super(NAME, services);

        seqGet = addParametricFunction(services, "seqGet");
        seqLen = addParametricFunction(services, "seqLen");
        seqIndexOf = addParametricFunction(services, "seqIndexOf");
        seqGetOutside = addParametricFunction(services, "seqGetOutside");
        seqEmpty = addParametricFunction(services, "seqEmpty");
        seqSingleton = addParametricFunction(services, "seqSingleton");
        seqConcat = addParametricFunction(services, "seqConcat");
        seqSub = addParametricFunction(services, "seqSub");
        seqReverse = addParametricFunction(services, "seqReverse");
        seqUpd = addParametricFunction(services, "seqUpd");
        seqDef = addParametricFunction(services, "seqDef");
        seqSwap = addParametricFunction(services, "seqSwap");
        seqRemove = addParametricFunction(services, "seqRemove");
        seqNPermInv = addFunction(services, "seqNPermInv");
    }

    public ParametricFunctionDecl getSeqGet() {
        return seqGet;
    }

    public boolean isSeqGetOp(org.key_project.logic.op.Operator op) {
        return op instanceof ParametricFunctionInstance pfi && pfi.getBase() == seqGet;
    }

    public ParametricFunctionDecl getSeqLen() {
        return seqLen;
    }

    public ParametricFunctionDecl getSeqIndexOf() {
        return seqIndexOf;
    }

    public ParametricFunctionDecl getSeqGetOutside() {
        return seqGetOutside;
    }

    public ParametricFunctionDecl getSeqEmpty() {
        return seqEmpty;
    }

    public ParametricFunctionDecl getSeqSingleton() {
        return seqSingleton;
    }

    public ParametricFunctionDecl getSeqConcat() {
        return seqConcat;
    }

    public ParametricFunctionDecl getSeqSub() {
        return seqSub;
    }

    public ParametricFunctionDecl getSeqReverse() {
        return seqReverse;
    }

    public ParametricFunctionDecl getSeqUpd() {
        return seqUpd;
    }

    public ParametricFunctionDecl getSeqDef() {
        return seqDef;
    }

    public ParametricFunctionDecl getSeqSwap() {
        return seqSwap;
    }

    public ParametricFunctionDecl getSeqRemove() {
        return seqRemove;
    }

    public Function getSeqNPermInv() {
        return seqNPermInv;
    }

    @Override
    public boolean isResponsible(BinaryExpression.Operator op, Term[] subs, Services services) {
        return false;
    }

    @Override
    public boolean isResponsible(BinaryExpression.Operator op, Term left, Term right,
            Services services) {
        return false;
    }

    @Override
    public boolean isResponsible(BinaryExpression.Operator op, Term sub, Services services) {
        return false;
    }

    @Override
    public Term translateLiteral(LiteralExpression lit, Services services) {
        return null;
    }

    @Override
    public @Nullable Function getFunctionFor(BinaryExpression.Operator op, Services services) {
        return null;
    }
}
