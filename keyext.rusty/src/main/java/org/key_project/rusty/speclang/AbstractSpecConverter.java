/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.speclang;

import java.util.*;

import org.key_project.logic.Term;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.abstraction.GenericConstParam;
import org.key_project.rusty.ast.abstraction.GenericParam;
import org.key_project.rusty.ast.abstraction.PrimitiveType;
import org.key_project.rusty.logic.RustyDLTheory;
import org.key_project.rusty.logic.TermBuilder;
import org.key_project.rusty.logic.TermFactory;
import org.key_project.rusty.logic.op.*;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.rusty.parser.hir.*;
import org.key_project.rusty.parser.hir.QPath;
import org.key_project.rusty.parser.hir.expr.BinOp;
import org.key_project.rusty.parser.hir.expr.BinOpKind;
import org.key_project.rusty.parser.hir.expr.LitKind;
import org.key_project.rusty.parser.hir.expr.UnOp;
import org.key_project.rusty.speclang.spec.QuantorKind;
import org.key_project.rusty.speclang.spec.QuantorParam;
import org.key_project.rusty.speclang.spec.TermKind;

import org.jspecify.annotations.Nullable;

public abstract class AbstractSpecConverter {
    protected final Services services;
    protected final TermBuilder tb;
    protected final TermFactory tf;

    protected @Nullable Map<LocalDefId, GenericParam> localParams = null;
    protected @Nullable ConversionCtx ctx = null;

    public static final class ConversionCtx {
        private final Map<HirId, ProgramVariable> pvMap;
        private final Stack<HirId> boundVars = new Stack<>();
        private final Map<HirId, Sort> bvSorts = new HashMap<>();

        public ConversionCtx(Map<HirId, ProgramVariable> pvMap) {
            this.pvMap = pvMap;
        }

        @Nullable
        ProgramVariable getPV(HirId id) {
            return pvMap.get(id);
        }

        @Nullable
        LogicVariable getLogicVar(HirId id) {
            for (int i = 0; i < boundVars.size(); i++) {
                if (boundVars.get(i).equals(id)) {
                    return LogicVariable.create(i + 1, bvSorts.get(id));
                }
            }
            return null;
        }

        void registerBoundVar(HirId id, BoundVariable bv) {
            boundVars.push(id);
            bvSorts.put(id, bv.sort());
        }

        void popBoundVar() {
            var id = boundVars.pop();
            bvSorts.remove(id);
        }

        Term getTerm(HirId id, TermBuilder tb) {
            var pv = getPV(id);
            if (pv != null) {
                return tb.var(pv);
            }
            var lv = Objects.requireNonNull(getLogicVar(id), "No PV or LV for " + id);
            return tb.var(lv);
        }

        @Override
        public String toString() {
            return "ConversionCtx[" +
                "pvMap=" + pvMap + ", " +
                "boundVars=" + boundVars + ", " +
                "bvSorts=" + bvSorts + ']';
        }
    }

    public AbstractSpecConverter(Services services) {
        this.services = services;
        this.tb = services.getTermBuilder();
        this.tf = services.getTermFactory();
    }

    protected void setCtx(ConversionCtx ctx) {
        this.ctx = ctx;
    }

    protected void clearCtx() {
        this.ctx = null;
    }

    protected void setLocalParams(Map<LocalDefId, GenericParam> localParams) {
        this.localParams = localParams;
    }

    protected void clearLocalParams() {
        this.localParams = null;
    }

    public Term convert(BinOp op, Term left, Term right) {
        // TODO: make this "proper"
        var intLDT = services.getLDTs().getIntLDT();
        var o = switch (op.node()) {
            case BinOpKind.Add -> intLDT.getAdd();
            case BinOpKind.Sub -> intLDT.getSub();
            case BinOpKind.Mul -> intLDT.getMul();
            case BinOpKind.Div -> intLDT.getDiv();
            case BinOpKind.And -> Junctor.AND;
            case BinOpKind.Or -> Junctor.OR;
            case BinOpKind.Lt -> intLDT.getLessThan();
            case BinOpKind.Le -> intLDT.getLessOrEquals();
            case BinOpKind.Gt -> intLDT.getGreaterThan();
            case BinOpKind.Ge -> intLDT.getGreaterOrEquals();
            case BinOpKind.Eq, BinOpKind.LogEq -> Equality.EQUALS;
            case BinOpKind.BitXor, BinOpKind.BitAnd, BinOpKind.BitOr, BinOpKind.Shl, BinOpKind.Rem,
                    BinOpKind.Shr ->
                throw new RuntimeException("TODO");
            case BinOpKind.Ne -> Junctor.NOT;
            case BinOpKind.Implication -> Junctor.IMP;
        };
        if (o == Junctor.NOT) {
            return tb.not(tb.equals(left, right));
        }
        if (o == Equality.EQUALS) {
            if (left.sort() == RustyDLTheory.FORMULA && right.sort() == RustyDLTheory.FORMULA
                    || left.sort() != RustyDLTheory.FORMULA
                            && right.sort() != RustyDLTheory.FORMULA) {
                return tb.equals(left, right);
            }
            if (left.sort() == RustyDLTheory.FORMULA) {
                return tb.equals(left, tb.equals(right, tb.TRUE()));
            }
            return tb.equals(tb.equals(left, tb.TRUE()), right);
        }
        return tf.createTerm(o, left, right);
    }

    public Term convert(UnOp op, Term child) {
        var intLDT = services.getLDTs().getIntLDT();
        var boolLDT = services.getLDTs().getBoolLDT();
        return switch (op) {
            case Deref -> throw new RuntimeException("TODO");
            case Not -> child.sort() == RustyDLTheory.FORMULA ? tb.not(child)
                    : tb.equals(child, boolLDT.getFalseTerm());
            case Neg -> tf.createTerm(intLDT.getNeg(), child);
            case PtrMetadata -> throw new UnsupportedOperationException("PtrMetadata UnOp");
        };
    }

    public Term convertPath(QPath path) {
        if (path instanceof QPath.Resolved r
                && r.path().segments().length == 1
                && r.path().res() instanceof org.key_project.rusty.parser.hir.Res.Local(HirId id)) {
            return ctx.getTerm(id, tb);
        }
        if (path instanceof org.key_project.rusty.parser.hir.QPath.Resolved r) {
            return convertResToTerm(r.path().res());
        }
        throw new IllegalArgumentException("Unknown path: " + path);
    }

    public Term convert(org.key_project.rusty.speclang.spec.Term term) {
        return switch (term.kind()) {
            case TermKind.Binary(var op, var left, var right) -> {
                var l = convert(left);
                var r = convert(right);
                yield convert(op, l, r);
            }
            case TermKind.Unary(var op, var child) -> {
                var c = convert(child);
                yield convert(op, c);
            }
            case TermKind.Lit(var l) -> switch (l.node()) {
                case LitKind.Bool(var b) -> b ? tb.tt() : tb.ff();
                case LitKind.Int(var i, var ignored) -> tb.zTerm(i);
                default -> throw new IllegalStateException("Unexpected value: " + l.node());
            };
            case TermKind.Tup(var ts) -> {
                var terms =
                    Arrays.stream(ts).map(this::convert).toArray(Term[]::new);
                yield tb.tuple(terms);
            }
            case TermKind.Path(var p) -> convertPath(p);
            case TermKind.Quantor q -> convertQuantifier(q);
            case TermKind.Index i -> convertIndex(i);
            default -> throw new IllegalStateException("Unexpected value: " + term);
        };
    }

    private Term convertIndex(TermKind.Index i) {
        var indexed = convert(i.term());
        var idx = convert(i.idx());
        return constructIndexTerm(indexed, idx);
    }

    private Term constructIndexTerm(Term indexed, Term idx) {
        if (indexed.sort() instanceof ParametricSortInstance psi) {
            if (psi.getBase() == services.getLDTs().getArrayLDT().parametricSort()) {
                return tb.arrayGet(indexed, idx);
            }
            if (psi.getBase() == services.getLDTs().getsRefLDT().parametricSort()) {
                var base = services.getLDTs().getsRefLDT().getDerefS();
                var deref = ParametricFunctionInstance.get(base, psi.getArgs());
                return constructIndexTerm(tb.func(deref, indexed), idx);
            }
        }
        throw new IllegalArgumentException("Index undefined for: " + indexed);
    }

    private Term convertQuantifier(TermKind.Quantor q) {
        var param = convertQuantorParam(q.param());
        ctx.registerBoundVar(q.param().hirId(), param);
        var term = convert(q.term());
        // TODO: get type from rml
        var ty = services.getRustInfo().getKeYRustyType(PrimitiveType.USIZE);
        var inRange = tb.reachableValue(tb.var(LogicVariable.create(1, ty.getSort())), ty);
        ctx.popBoundVar();
        if (q.kind() == QuantorKind.Exists) {
            return tb.ex(param, tb.and(inRange, term));
        } else {
            if (term.op() == Junctor.IMP) {
                // Optimize a bit
                term = tb.imp(tb.and(inRange, term.sub(0)), term.sub(1));
            } else {
                term = tb.imp(inRange, term);
            }
            return tb.all(param, term);
        }
    }

    private BoundVariable convertQuantorParam(QuantorParam p) {
        var name = services.getVariableNamer().getTemporaryNameProposal(p.ident().name());
        // TODO: get type from rml
        var ty = services.getRustInfo().getKeYRustyType(PrimitiveType.USIZE);
        return new BoundVariable(name, ty.getSort());
    }

    private Term convertResToTerm(org.key_project.rusty.parser.hir.Res res) {
        return switch (res) {
            case org.key_project.rusty.parser.hir.Res.Local(var id) -> ctx.getTerm(id, tb);
            case org.key_project.rusty.parser.hir.Res.DefRes(var def) ->
                convertDefToTerm(def);
            default -> throw new IllegalArgumentException("Unknown hirty type: " + res);
        };
    }

    private Term convertDefToTerm(org.key_project.rusty.parser.hir.Def def) {
        final LocalDefId localDefId = new LocalDefId(def.id().index());
        return switch (def.kind()) {
            case DefKind.ConstParam ignored -> {
                var param = (GenericConstParam) localParams.get(localDefId);
                // TODO: Fix once RML is reworked
                if (param == null && localParams.size() == 1) {
                    param = (GenericConstParam) localParams.values().iterator().next();
                }
                yield tb.func(param.fn());
            }
            default -> throw new IllegalArgumentException("Unknown def: " + def);
        };
    }
}
