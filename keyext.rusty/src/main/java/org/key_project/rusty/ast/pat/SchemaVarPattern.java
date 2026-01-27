/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.pat;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.RustyProgramElement;
import org.key_project.rusty.ast.SourceData;
import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.rusty.logic.op.sv.OperatorSV;
import org.key_project.rusty.logic.op.sv.ProgramSV;
import org.key_project.rusty.logic.sort.ProgramSVSort;
import org.key_project.rusty.rule.MatchConditions;
import org.key_project.rusty.rule.inst.SVInstantiations;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record SchemaVarPattern(boolean reference, boolean mut, OperatorSV operatorSV)
        implements Pattern {
    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0)
            return operatorSV;
        throw new IndexOutOfBoundsException("SchemaVarPattern has only one child");
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public @Nullable MatchConditions match(SourceData source, @Nullable MatchConditions mc) {
        final Services services = source.getServices();
        final RustyProgramElement src = Objects.requireNonNull(source.getSource());

        assert mc != null;

        ProgramSVSort sort = (ProgramSVSort) operatorSV().sort();
        // TODO: This is rather ugly, fix!
        if (!ProgramSVSort.PATTERN.canStandFor(src, services)) {
            return null;
        }
        var bp = (BindingPattern) src;
        if (bp.mut() != mut() || bp.mutRef() && (!mut() || !reference())
                || bp.ref() && (mut() || !reference())) {
            return null;
        }
        if (sort == ProgramSVSort.VARIABLE) {
            mc = ((ProgramSV) operatorSV).match(new SourceData(src, 0, services), mc);
            if (mc == null) {
                return null;
            }
        } else {

            final SVInstantiations instantiations = mc.getInstantiations();
            final Object instant = instantiations.getInstantiation(operatorSV);
            if (instant == null || instant.equals(src)
                    || (instant instanceof Term t && t.op().equals(src))) {
                mc = addPatternInstantiation(src, mc, instantiations, instant, services);
                if (mc == null) {
                    return null;
                }
            } else {
                return null;
            }
        }
        source.next();
        return mc;
    }

    private @Nullable MatchConditions addPatternInstantiation(RustyProgramElement pe,
            @Nullable MatchConditions mc, SVInstantiations insts, @Nullable Object foundInst,
            Services services) {
        if (mc == null) {
            return null;
        }

        if (foundInst != null) {
            final Object newInst;
            if (foundInst instanceof Term) {
                newInst = services.convertToLogicElement(pe);
            } else {
                newInst = pe;
            }

            if (foundInst.equals(newInst)) {
                return mc;
            } else {
                return null;
            }
        }

        insts = insts.add(operatorSV, pe, services);
        return insts == null ? null : mc.setInstantiations(insts);
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnSchemaVarPattern(this);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        if (reference) {
            sb.append("&");
        }
        if (mut)
            sb.append("mut ");
        sb.append(operatorSV);
        return sb.toString();
    }
}
