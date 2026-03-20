/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.varcond;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.TermBuilder;
import org.key_project.rusty.logic.op.ElementaryUpdate;
import org.key_project.rusty.logic.op.MutatingUpdate;
import org.key_project.rusty.logic.op.ParametricFunctionInstance;
import org.key_project.rusty.logic.op.UpdateJunctor;
import org.key_project.rusty.logic.op.sv.UpdateSV;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.rusty.rule.inst.SVInstantiations;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ApplyElementariesOnDerefMCondition implements VariableCondition {
    private final UpdateSV u;
    private final SchemaVariable mr;
    private final SchemaVariable result;

    private static final class SplitUpdate {
        private @Nullable Term elems;
        private @Nullable Term muts;
        private boolean failed = false;
        private final Services services;

        private SplitUpdate(Services services) {
            this.elems = null;
            this.muts = null;
            this.services = services;
        }

        void addElem(Term upd) {
            if (elems == null)
                elems = upd;
            else
                elems = services.getTermBuilder().parallel(elems, upd);
        }

        void addMut(Term upd) {
            if (muts == null)
                muts = upd;
            else
                muts = services.getTermBuilder().parallel(muts, upd);
        }

        public @Nullable Term elems() {
            return elems;
        }

        public @Nullable Term muts() {
            return muts;
        }

        void fail() {
            this.failed = true;
        }

        public Services services() {
            return services;
        }
    }

    public ApplyElementariesOnDerefMCondition(UpdateSV u,
            SchemaVariable mr,
            SchemaVariable x2) {
        this.u = u;
        this.mr = mr;
        this.result = x2;
    }

    private static void collectElementaries(Term update, SplitUpdate su, Services services) {
        if (update.op() instanceof ElementaryUpdate) {
            su.addElem(update);
        } else if (update.op() instanceof MutatingUpdate) {
            su.addMut(update);
        } else if (update.op() == UpdateJunctor.PARALLEL_UPDATE) {
            collectElementaries(update.sub(0), su, services);
            collectElementaries(update.sub(1), su, services);
        } else {
            if (update.op() != UpdateJunctor.SKIP) {
                su.fail();
            }
        }
    }

    private static @Nullable Term applyElementariesOnDerefM(Term update, Term mr,
            Services services) {
        var su = new SplitUpdate(services);
        collectElementaries(update, su, services);
        if (su.failed || su.elems() == null)
            return null;
        var base = services.getLDTs().getmRefLDT().getDerefM();
        var sort = (ParametricSortInstance) mr.sort();
        var derefM = ParametricFunctionInstance.get(base, ImmutableList.of(sort.getArgs().get(0)));
        TermBuilder tb = services.getTermBuilder();
        var elems = su.elems() == null ? tb.skip() : su.elems();
        var muts = su.muts() == null ? tb.skip() : su.muts();
        return tb.apply(muts, tb.func(derefM, tb.apply(elems, mr)));
    }

    @Override
    public @Nullable MatchResultInfo check(@Nullable SchemaVariable var,
            @Nullable SyntaxElement instCandidate, @NonNull MatchResultInfo mc,
            @NonNull LogicServices services) {
        SVInstantiations svInst = (SVInstantiations) mc.getInstantiations();
        Term uInst = (Term) svInst.getInstantiation(u);
        Term mrInst = (Term) svInst.getInstantiation(mr);
        Term resultInst = (Term) svInst.getInstantiation(result);
        if (uInst == null || mrInst == null) {
            return mc;
        }

        Term properResultInst = applyElementariesOnDerefM(uInst, mrInst, (Services) services);
        if (properResultInst == null) {
            return null;
        } else if (resultInst == null) {
            svInst = svInst.add(result, properResultInst, services);
            return mc.setInstantiations(svInst);
        } else if (resultInst.equals(properResultInst)) {
            return mc;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "\\applyElementariesOnDerefM(" + u + ", " + mr + ", " + result + ")";
    }
}
