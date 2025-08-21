package org.key_project.rusty.speclang;

import org.key_project.logic.Term;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.RustyDLTheory;
import org.key_project.rusty.logic.TermBuilder;
import org.key_project.rusty.logic.TermFactory;
import org.key_project.rusty.logic.op.Equality;
import org.key_project.rusty.logic.op.Junctor;
import org.key_project.rusty.logic.op.ProgramVariable;
import org.key_project.rusty.parser.hir.HirId;
import org.key_project.rusty.parser.hir.QPath;
import org.key_project.rusty.parser.hir.expr.BinOp;
import org.key_project.rusty.parser.hir.expr.BinOpKind;
import org.key_project.rusty.parser.hir.expr.LitKind;
import org.key_project.rusty.parser.hir.expr.UnOp;
import org.key_project.rusty.speclang.spec.TermKind;

import java.util.Arrays;
import java.util.Map;

public abstract class AbstractSpecConverter {
    protected final Services services;
    protected final TermBuilder tb;
    protected final TermFactory tf;

    public AbstractSpecConverter(Services services) {
        this.services = services;
        this.tb = services.getTermBuilder();
        this.tf = services.getTermFactory();
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
            case BinOpKind.Eq, BinOpKind.LogEq ->
                left.sort() == RustyDLTheory.FORMULA ? Equality.EQV : Equality.EQUALS;
            case BinOpKind.BitXor, BinOpKind.BitAnd, BinOpKind.BitOr, BinOpKind.Shl, BinOpKind.Rem,
                    BinOpKind.Shr ->
                throw new RuntimeException("TODO");
            case BinOpKind.Ne -> Junctor.NOT;
        };
        if (o == Junctor.NOT) {
            return tb.not(tb.equals(left, right));
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

    public Term convertPath(QPath path, Map<HirId, ProgramVariable> pvMap) {
        if (path instanceof QPath.Resolved r
                && r.path().segments().length == 1
                && r.path().res() instanceof org.key_project.rusty.parser.hir.Res.Local(HirId id)) {
            var pvo = pvMap.get(id);
            if (pvo != null)
                return tb.var(pvo);
        }
        throw new IllegalArgumentException("Unknown path: " + path);
    }

    public Term convert(org.key_project.rusty.speclang.spec.Term term,
            Map<HirId, ProgramVariable> pvMap) {
        return switch (term.kind()) {
            case TermKind.Binary(var op, var left, var right) -> {
                var l = convert(left, pvMap);
                var r = convert(right, pvMap);
                yield convert(op, l, r);
            }
            case TermKind.Unary(var op, var child) -> {
                var c = convert(child, pvMap);
                yield convert(op, c);
            }
            case TermKind.Lit(var l) -> switch (l.node()) {
                case LitKind.Bool(var b) -> b ? tb.tt() : tb.ff();
                case LitKind.Int(var i, var ignored) -> tb.zTerm(i);
                default -> throw new IllegalStateException("Unexpected value: " + l.node());
            };
            case TermKind.Tup(var ts) -> {
                var terms =
                    Arrays.stream(ts).map(t -> convert(t, pvMap)).toArray(Term[]::new);
                yield tb.tuple(terms);
            }
            case TermKind.Path(var p) -> convertPath(p, pvMap);
            default -> throw new IllegalStateException("Unexpected value: " + term);
        };
    }
}
