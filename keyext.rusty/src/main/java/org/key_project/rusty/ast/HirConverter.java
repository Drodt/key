/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast;

import java.math.BigInteger;
import java.util.*;

import org.key_project.logic.Name;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.abstraction.*;
import org.key_project.rusty.ast.abstraction.Enum;
import org.key_project.rusty.ast.abstraction.GenericEnum;
import org.key_project.rusty.ast.expr.*;
import org.key_project.rusty.ast.expr.Expr;
import org.key_project.rusty.ast.fn.Function;
import org.key_project.rusty.ast.fn.FunctionParam;
import org.key_project.rusty.ast.fn.FunctionParamPattern;
import org.key_project.rusty.ast.pat.*;
import org.key_project.rusty.ast.stmt.ExpressionStatement;
import org.key_project.rusty.ast.stmt.ItemStatement;
import org.key_project.rusty.ast.stmt.LetStatement;
import org.key_project.rusty.ast.stmt.Statement;
import org.key_project.rusty.ast.ty.*;
import org.key_project.rusty.logic.op.ProgramFunction;
import org.key_project.rusty.logic.op.ProgramVariable;
import org.key_project.rusty.logic.sort.GenericParameter;
import org.key_project.rusty.logic.sort.GenericSort;
import org.key_project.rusty.logic.sort.ParametricSortDecl;
import org.key_project.rusty.logic.sort.SortImpl;
import org.key_project.rusty.parser.hir.*;
import org.key_project.rusty.parser.hir.expr.*;
import org.key_project.rusty.parser.hir.hirty.*;
import org.key_project.rusty.parser.hir.item.Fn;
import org.key_project.rusty.parser.hir.item.FnRetTy;
import org.key_project.rusty.parser.hir.item.ImplicitSelfKind;
import org.key_project.rusty.parser.hir.pat.ByRef;
import org.key_project.rusty.parser.hir.pat.Pat;
import org.key_project.rusty.parser.hir.pat.PatExprKind;
import org.key_project.rusty.parser.hir.pat.PatKind;
import org.key_project.rusty.parser.hir.stmt.LetStmt;
import org.key_project.rusty.parser.hir.stmt.Stmt;
import org.key_project.rusty.parser.hir.stmt.StmtKind;
import org.key_project.rusty.parser.hir.ty.*;
import org.key_project.rusty.speclang.FnSpecConverter;
import org.key_project.rusty.speclang.LoopSpecConverter;
import org.key_project.rusty.speclang.spec.FnSpec;
import org.key_project.rusty.speclang.spec.LoopSpec;
import org.key_project.rusty.speclang.spec.SpecMap;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.Nullable;

public class HirConverter {
    private final Services services;

    private final @Nullable Map<DefId, FnSpec> fnSpecs;
    private final @Nullable Map<HirId, LoopSpec> loopSpecs;
    private final Map<DefId, Adt> adts = new HashMap<>();
    private final FnSpecConverter fnSpecConverter;
    private final LoopSpecConverter loopSpecConverter;
    private @Nullable GenericTyParam[] currentParams = null;

    public HirConverter(Services services, @Nullable SpecMap specs) {
        this.services = services;
        fnSpecConverter = new FnSpecConverter(services);
        loopSpecConverter = new LoopSpecConverter(services);
        if (specs != null) {
            fnSpecs = new HashMap<>(specs.fnSpecs().length);
            loopSpecs = new HashMap<>(specs.loopSpecs().length);
            for (var e : specs.fnSpecs()) {
                fnSpecs.put(e.id(), e.value());
            }
            for (var e : specs.loopSpecs()) {
                loopSpecs.put(e.id(), e.value());
            }
        } else {
            fnSpecs = null;
            loopSpecs = null;
        }
    }

    public Services getServices() {
        return services;
    }

    private final Map<HirId, ProgramVariable> pvs = new HashMap<>();
    private final Map<HirId, Type> types = new HashMap<>();
    private final Map<LocalDefId, Function> localFns = new HashMap<>();
    private final Map<Function, FnSpec> fn2Spec = new HashMap<>();

    private @Nullable Function currentFn = null;

    /// We first convert all functions except their bodies. Then we convert those later.
    private final Map<Function, Fn> fnsToComplete =
        new HashMap<>();

    private ProgramVariable getPV(HirId id) {
        return Objects.requireNonNull(pvs.get(id), "Unknown variable " + id);
    }

    private void declarePV(HirId id, ProgramVariable pv) {
        pvs.put(id, pv);
    }

    public Crate convertCrate(org.key_project.rusty.parser.hir.Crate crate) {
        currentFn = null;
        for (var adt : crate.adts()) {
            var def = getAdt(adt.def());
            adts.put(adt.defId(), def);
        }
        Crate crate1 = new Crate(convertMod(crate.topMod()));
        for (var m : crate.types()) {
            var ty = convertTy(m.ty());
            types.put(m.hirId(), ty);
        }
        for (var fn : fnsToComplete.keySet()) {
            currentFn = fn;
            var spec = fn2Spec.get(fn);
            var hirFn = fnsToComplete.get(fn);
            boolean isCtxFn = fn.name().toString().equals(Context.TMP_FN_NAME);
            int paramLength = hirFn.sig().decl().inputs().length;
            int selfCount = 0;
            if (hirFn.sig().decl().implicitSelf() != ImplicitSelfKind.None) {
                selfCount++;
            }
            var params = new ArrayList<FunctionParam>(paramLength + selfCount);
            for (int i = 0; i < paramLength; i++) {
                var ty = hirFn.sig().decl().inputs()[i];
                var pat = hirFn.body().params()[i].pat();
                RustType type = convertHirTy(ty);
                params.add(new FunctionParamPattern(convertPat(pat, isCtxFn, type.type()), type,
                    services.getRustInfo().getKeYRustyType(type.type())));
            }
            fn.setParams(new ImmutableArray<>(params));
            fn.setBody((BlockExpression) convertExpr(hirFn.body().value()));
            services.getRustInfo().registerFunction(fn);
            if (spec != null) {
                var contracts =
                    fnSpecConverter.convert(spec,
                        Objects.requireNonNull(services.getRustInfo().getFunction(fn)));
                for (var contract : contracts) {
                    services.getSpecificationRepository().addContract(contract);
                }
            }
            currentFn = null;
        }
        return crate1;
    }

    private String convertIdent(Ident ident) {
        return ident.name();
    }

    private Mod convertMod(org.key_project.rusty.parser.hir.Mod mod) {
        return new Mod(
            Arrays.stream(mod.items()).map(this::convertItem).collect(ImmutableList.collector()));
    }

    private Item convertItem(org.key_project.rusty.parser.hir.item.Item item) {
        return switch (item.kind()) {
            case org.key_project.rusty.parser.hir.item.Use use -> convertUse(use);
            case Fn fn -> convertFn(fn, item.ownerId().defId());
            case org.key_project.rusty.parser.hir.item.ExternCrate ec -> convertExternCrate(ec);
            default -> throw new IllegalArgumentException("Unknown item: " + item);
        };
    }

    private Item convertUse(org.key_project.rusty.parser.hir.item.Use use) {
        var path = convertPath(use.path(), rs -> {
            var lst = Arrays.stream(rs).map(this::convertRes).toList();
            return new ImmutableArray<>(lst);
        });
        var kind = switch (use.useKind()) {
            case org.key_project.rusty.parser.hir.item.Use.UseKind.Single -> Use.UseKind.Single;
            case org.key_project.rusty.parser.hir.item.Use.UseKind.Glob -> Use.UseKind.Glob;
            case org.key_project.rusty.parser.hir.item.Use.UseKind.ListStem -> Use.UseKind.ListStem;
        };
        return new Use(path, kind);
    }

    private Function convertFn(Fn fn, LocalDefId id) {
        var ident = fn.ident().name();
        var name = new Name(ident);
        var retTy = convertFnRetTy(fn.sig().decl().output());
        @SuppressWarnings("argument.type.incompatible")
        Function function = new Function(name, Function.ImplicitSelfKind.None,
            null, retTy, null);
        if (fnSpecs != null) {
            var spec = fnSpecs.get(new DefId(id.localDefIndex(), 0));
            if (spec != null)
                fn2Spec.put(function, Objects.requireNonNull(spec, name.toString()));
        }
        localFns.put(id, function);
        fnsToComplete.put(function, fn);
        return function;
    }

    private RustType convertFnRetTy(FnRetTy retTy) {
        return switch (retTy) {
            case FnRetTy.DefaultReturn ignored -> TupleRustType.UNIT;
            case FnRetTy.Return(var ty) -> convertHirTy(ty);
            default -> throw new IllegalArgumentException("Unknown return type: " + retTy);
        };
    }

    private Item convertExternCrate(org.key_project.rusty.parser.hir.item.ExternCrate ec) {
        return new ExternCrate(ec.ident().name(), ec.symbol());
    }

    private Expr convertExpr(org.key_project.rusty.parser.hir.expr.Expr expr) {
        var id = expr.hirId();
        var ty = Objects.requireNonNull(types.get(id), "No type for " + expr);
        return switch (expr.kind()) {
            case ExprKind.ConstBlock e -> convertConstBlockExpr(e);
            case ExprKind.Array e -> convertArrayExpr(e, ty);
            case ExprKind.Call e -> convertCall(e);
            case ExprKind.MethodCall e -> convertMethodCall(e);
            case ExprKind.Tup e -> convertTupleExpr(e, ty);
            case ExprKind.Binary e -> convertBinary(e);
            case ExprKind.Unary e -> convertUnary(e, ty);
            case ExprKind.LitExpr(var e) -> convertLitExpr(e, ty);
            case ExprKind.CastExpr e -> convertCastExpr(e);
            // TypeExpr? When does this occur?
            case ExprKind.DropTemps(var e) -> convertExpr(e);
            case ExprKind.Let(var l) -> convertLetExpr(l);
            case ExprKind.If e -> convertIfExpr(e, ty);
            case ExprKind.Loop e -> convertLoopExpr(e, id, ty);
            case ExprKind.Match e -> convertMatchExpr(e, ty);
            case ExprKind.Closure e -> convertClosure(e);
            case ExprKind.BlockExpr e -> convertBlockExpr(e);
            case ExprKind.Assign e -> convertAssign(e, ty);
            case ExprKind.AssignOp e -> convertAssignOp(e);
            case ExprKind.Field e -> convertFieldExpr(e);
            case ExprKind.Index e -> convertIndexExpr(e, ty);
            case ExprKind.Path(var e) -> convertPathExpr(e, ty);
            case ExprKind.AddrOf e -> convertAddrOf(e);
            case ExprKind.Break e -> convertBreakExpr(e);
            case ExprKind.Continue e -> convertContinue(e);
            case ExprKind.Ret e -> convertReturn(e);
            // Become? What even is that?
            // InlineAsm?
            // OffsetOf?
            case ExprKind.Struct e -> convertStructExpr(e);
            case ExprKind.Repeat e -> convertRepeat(e, ty);
            // case ExprKind.Yield e -> convertYieldExpr(e);
            default -> throw new IllegalArgumentException("Unknown expression: " + expr);
        };
    }

    private ConstBlockExpression convertConstBlockExpr(ExprKind.ConstBlock e) {
        var body = (BlockExpression) convertExpr(e.block().body().value());
        return new ConstBlockExpression(body);
    }

    private ArrayExpression convertArrayExpr(ExprKind.Array e, Type ty) {
        var exprs = new Expr[e.exprs().length];
        for (int i = 0; i < exprs.length; i++) {
            exprs[i] = convertExpr(e.exprs()[i]);
        }
        return new ArrayExpression(new ImmutableArray<>(exprs), ty);
    }

    private MethodCallExpression convertMethodCall(ExprKind.MethodCall e) {
        var args = new Expr[e.args().length];
        for (int i = 0; i < e.args().length; i++) {
            args[i] = convertExpr(e.args()[i]);
        }
        return new MethodCallExpression(convertExpr(e.callee()), convertPathSegment(e.segment()),
            new ImmutableArray<>(args));
    }

    private TupleExpression convertTupleExpr(ExprKind.Tup e, Type type) {
        if (e.exprs().length == 0)
            return TupleExpression.UNIT;
        var exprs = new Expr[e.exprs().length];
        for (int i = 0; i < exprs.length; i++) {
            exprs[i] = convertExpr(e.exprs()[i]);
        }
        return new TupleExpression(new ImmutableArray<>(exprs), type);
    }

    private Expr convertCastExpr(ExprKind.CastExpr e) {
        return new TypeCastExpression(convertExpr(e.expr()), convertHirTy(e.ty()));
    }

    private MatchExpression convertMatchExpr(ExprKind.Match e, Type ty) {
        var arms = new MatchArm[e.arms().length];
        for (int i = 0; i < arms.length; i++) {
            arms[i] = convertArm(e.arms()[i], ty);
        }
        return new MatchExpression(convertExpr(e.expr()), new ImmutableArray<>(arms));
    }

    private MatchArm convertArm(Arm arm, Type ty) {
        return new MatchArm(convertPat(arm.pat(), ty),
            arm.guard() == null ? null : convertExpr(arm.guard()), convertExpr(arm.body()));
    }

    private ClosureExpression convertClosure(ExprKind.Closure e) {
        var c = e.closure();
        var params = new ClosureParam[c.body().params().length];
        // TODO
        RustType ty = null;
        var body = convertExpr(c.body().value());
        return new ClosureExpression(c.captureClause() instanceof CaptureBy.Value,
            new ImmutableArray<>(params), ty, body);
    }

    private FieldExpression convertFieldExpr(ExprKind.Field e) {
        return new FieldExpression(convertExpr(e.expr()),
            new Identifier(new Name(convertIdent(e.field()))));
    }

    private ContinueExpression convertContinue(ExprKind.Continue e) {
        var label = e.dest() == null ? null : convertDest(e.dest());
        return new ContinueExpression(label);
    }

    private ReturnExpression convertReturn(ExprKind.Ret e) {
        var expr = e.expr() == null ? null : convertExpr(e.expr());
        return new ReturnExpression(expr);
    }

    private StructExpression convertStructExpr(ExprKind.Struct e) {
        var path = convertQPath(e.path());
        var fields = new StructExprField[e.fields().length];
        for (int i = 0; i < e.fields().length; i++) {
            fields[i] = convertExprField(e.fields()[i]);
        }
        var tail = switch (e.tail()) {
            case StructTailExpr.None ignored -> null;
            case StructTailExpr.Base b -> new BaseStructTailExpression(convertExpr(b.base()));
            case StructTailExpr.DefaultFields ignored -> new DefaultStructTailExpression();
            default -> throw new IllegalArgumentException("Unknown struct tail: " + e.tail());
        };
        return new StructExpression(path, new ImmutableArray<>(fields), tail);
    }

    private StructExprField convertExprField(ExprField field) {
        var name = new Name(field.ident().name());
        var expr = convertExpr(field.expr());
        return new StructExprField(new Identifier(name), expr, field.isShorthand());
    }

    private CallExpression convertCall(ExprKind.Call call) {
        var callee = convertExpr(call.callee());
        var args = Arrays.stream(call.args()).map(this::convertExpr).toList();
        return new CallExpression(callee, new ImmutableArray<>(args));
    }

    private BlockExpression convertBlockExpr(ExprKind.BlockExpr expr) {
        var stmts = Arrays.stream(expr.block().stmts()).map(this::convertStmt).toList();
        var value = expr.block().expr() == null ? null : convertExpr(expr.block().expr());
        return new BlockExpression(ImmutableList.fromList(stmts), value);
    }

    private LiteralExpression convertLitExpr(Lit expr, Type type) {
        return switch (expr.node()) {
            case LitKind.Bool(var v) -> new BooleanLiteralExpression(v);
            case LitKind.Int(var val, LitIntTy.Unsigned(var uintTy)) ->
                new IntegerLiteralExpression(new BigInteger(String.valueOf(val)), switch (uintTy) {
                    case UintTy.U8 -> IntegerLiteralExpression.IntegerSuffix.u8;
                    case UintTy.U16 -> IntegerLiteralExpression.IntegerSuffix.u16;
                    case UintTy.U32 -> IntegerLiteralExpression.IntegerSuffix.u32;
                    case UintTy.U64 -> IntegerLiteralExpression.IntegerSuffix.u64;
                    case UintTy.U128 -> IntegerLiteralExpression.IntegerSuffix.u128;
                    case UintTy.Usize -> IntegerLiteralExpression.IntegerSuffix.usize;
                }, type);
            case LitKind.Int(var val, LitIntTy.Signed(var intTy)) -> new IntegerLiteralExpression(
                new BigInteger(String.valueOf(val)), switch (intTy) {
                    case Isize -> IntegerLiteralExpression.IntegerSuffix.isize;
                    case I8 -> IntegerLiteralExpression.IntegerSuffix.i8;
                    case I16 -> IntegerLiteralExpression.IntegerSuffix.i16;
                    case I32 -> IntegerLiteralExpression.IntegerSuffix.i32;
                    case I64 -> IntegerLiteralExpression.IntegerSuffix.i64;
                    case I128 -> IntegerLiteralExpression.IntegerSuffix.i128;
                }, type);
            case LitKind.Int(var val, LitIntTy.Unsuffixed u) ->
                new IntegerLiteralExpression(new BigInteger(String.valueOf(val)),
                    IntegerLiteralExpression.IntegerSuffix.None, type);
            default -> throw new IllegalArgumentException("Unknown lit: " + expr.node());
        };
    }

    private LetExpression convertLetExpr(LetExpr let) {
        var init = convertExpr(let.init());
        var pat = convertPat(let.pat(), init.type(services));
        var ty = let.ty() == null ? null : convertHirTy(let.ty());
        return new LetExpression(pat, ty, init);
    }

    private IfExpression convertIfExpr(ExprKind.If i, Type type) {
        return new IfExpression(convertExpr(i.cond()), (ThenBranch) convertExpr(i.then()),
            i.els() == null ? null : (ElseBranch) convertExpr(i.els()), type);
    }

    private LoopExpression convertLoopExpr(ExprKind.Loop l, HirId id, Type type) {
        var body = convertBlockExpr(new ExprKind.BlockExpr(l.block()));
        var le = new InfiniteLoopExpression(null, body);

        if (loopSpecs != null && loopSpecs.containsKey(id)) {
            var ls = loopSpecConverter.convert(loopSpecs.get(id), Objects.requireNonNull(currentFn),
                le, pvs);
            services.getSpecificationRepository().addLoopSpec(ls);
        }

        return le;
    }

    private Expr convertPathExpr(org.key_project.rusty.parser.hir.QPath path, Type ty) {
        if (path instanceof org.key_project.rusty.parser.hir.QPath.Resolved r
                && r.path().segments().length == 1
                && r.path().res() instanceof org.key_project.rusty.parser.hir.Res.Local(HirId id)) {
            return getPV(id);
        }
        if (path instanceof org.key_project.rusty.parser.hir.QPath.Resolved r) {
            var cPath = convertPath(r.path(), this::convertRes);
            return new PathExpr(cPath, ty);
        }
        throw new IllegalArgumentException("Unknown path: " + path);
    }

    private BorrowExpression convertAddrOf(ExprKind.AddrOf addrOf) {
        return new BorrowExpression(addrOf.mut(), convertExpr(addrOf.expr()));
    }

    private BreakExpression convertBreakExpr(ExprKind.Break b) {
        var label = b.dest() == null ? null : convertDest(b.dest());
        return new BreakExpression(label, b.expr() != null ? convertExpr(b.expr()) : null);
    }

    private @Nullable Label convertDest(Destination dest) {
        if (dest.label() == null) {
            return null;
        }
        throw new UnsupportedOperationException("TODO: destination");
    }

    private AssignmentExpression convertAssign(ExprKind.Assign assign, Type type) {
        return new AssignmentExpression(convertExpr(assign.left()), convertExpr(assign.right()));
    }

    private CompoundAssignmentExpression convertAssignOp(ExprKind.AssignOp assignOp) {
        return new CompoundAssignmentExpression(convertExpr(assignOp.left()),
            convertAssignOp(assignOp.op().node()), convertExpr(assignOp.right()));
    }

    private BinaryExpression convertBinary(ExprKind.Binary binary) {
        return new BinaryExpression(convertBinOp(binary.op().node()), convertExpr(binary.left()),
            convertExpr(binary.right()));
    }

    private UnaryExpression convertUnary(ExprKind.Unary unary, Type type) {
        return new UnaryExpression(switch (unary.op()) {
            case Deref -> UnaryExpression.Operator.Deref;
            case Not -> UnaryExpression.Operator.Not;
            case Neg -> UnaryExpression.Operator.Neg;
            case PtrMetadata -> throw new UnsupportedOperationException("PtrMetadata UnOp");
        }, convertExpr(unary.expr()));
    }

    private RepeatExpression convertRepeat(ExprKind.Repeat repeat, Type type) {
        return new RepeatExpression(convertExpr(repeat.expr()),
            convertConstArg(repeat.len()), type);
    }

    private Expr convertConstArg(ConstArg len) {
        var ac = ((ConstArgKind.Anon) len.kind()).ac();
        return convertExpr(ac.body().value());
    }

    private IndexExpression convertIndexExpr(ExprKind.Index index, Type type) {
        return new IndexExpression(convertExpr(index.base()), convertExpr(index.idx()), type);
    }

    private BinaryExpression.Operator convertBinOp(BinOpKind binOp) {
        return switch (binOp) {
            case Add -> BinaryExpression.Operator.Add;
            case Sub -> BinaryExpression.Operator.Sub;
            case Mul -> BinaryExpression.Operator.Mul;
            case Div -> BinaryExpression.Operator.Div;
            case Rem -> BinaryExpression.Operator.Rem;
            case And -> BinaryExpression.Operator.And;
            case Or -> BinaryExpression.Operator.Or;
            case BitXor -> BinaryExpression.Operator.BitXor;
            case BitAnd -> BinaryExpression.Operator.BitAnd;
            case BitOr -> BinaryExpression.Operator.BitOr;
            case Shl -> BinaryExpression.Operator.Shl;
            case Shr -> BinaryExpression.Operator.Shr;
            case Eq, LogEq -> BinaryExpression.Operator.Eq;
            case Lt -> BinaryExpression.Operator.Lt;
            case Le -> BinaryExpression.Operator.Le;
            case Ne -> BinaryExpression.Operator.Ne;
            case Ge -> BinaryExpression.Operator.Ge;
            case Gt -> BinaryExpression.Operator.Gt;
        };
    }

    private BinaryExpression.Operator convertAssignOp(AssignOpKind binOp) {
        return switch (binOp) {
            case AddAssign -> BinaryExpression.Operator.Add;
            case SubAssign -> BinaryExpression.Operator.Sub;
            case MulAssign -> BinaryExpression.Operator.Mul;
            case DivAssign -> BinaryExpression.Operator.Div;
            case RemAssign -> BinaryExpression.Operator.Rem;
            case BitXorAssign -> BinaryExpression.Operator.BitXor;
            case BitAndAssign -> BinaryExpression.Operator.BitAnd;
            case BitOrAssign -> BinaryExpression.Operator.BitOr;
            case ShlAssign -> BinaryExpression.Operator.Shl;
            case ShrAssign -> BinaryExpression.Operator.Shr;
        };
    }

    private Statement convertStmt(Stmt stmt) {
        return switch (stmt.kind()) {
            case StmtKind.Let(var let) -> convertLet(let);
            case StmtKind.ItemStmt(var item) -> new ItemStatement(convertItem(item));
            case StmtKind.ExprStmt(var e) -> new ExpressionStatement(convertExpr(e), false);
            case StmtKind.Semi(var e) -> new ExpressionStatement(convertExpr(e), true);
            default -> throw new IllegalArgumentException("Unknown stmt: " + stmt.kind());
        };
    }

    private LetStatement convertLet(LetStmt let) {
        var pat = convertPat(let.pat(), null);
        var ty = let.ty() == null ? null : convertHirTy(let.ty());
        var init = let.init() == null ? null : convertExpr(let.init());
        return new LetStatement(pat, ty, init);
    }

    private RustType convertHirTy(HirTy ty) {
        return switch (ty.kind()) {
            case HirTyKind.Slice(var s) -> convertSliceHirTy(s);
            case HirTyKind.Array(var a, var l) -> convertArrayHirTy(a, l);
            case HirTyKind.Ptr(var p) -> convertPtrHirTy(p);
            case HirTyKind.Ref(var m) -> convertMutHirTy(m);
            case HirTyKind.Never ignored -> new NeverRustType();
            case HirTyKind.Tup(var tys) -> convertTupHirType(tys);
            case HirTyKind.Path p -> convertPathHirTy(p);
            case HirTyKind.Infer ignored -> new InferHirTy();
            default -> throw new IllegalArgumentException("Unknown hirty type: " + ty);
        };
    }

    private RustType convertTupHirType(HirTy[] tys) {
        var innerTys = Arrays.stream(tys).map(this::convertHirTy).toList();
        return new TupleRustType(new ImmutableArray<>(innerTys));
    }

    private RustType convertSliceHirTy(HirTy s) {
        return new SliceRustType(convertHirTy(s));
    }

    private RustType convertArrayHirTy(HirTy a, ConstArg l) {
        return new ArrayRustType(convertHirTy(a), convertConstArg(l), services);
    }

    private RustType convertPtrHirTy(HirTy p) {
        return new PtrRustType(convertHirTy(p));
    }

    private RustType convertPathHirTy(HirTyKind.Path ty) {
        if (ty.path() instanceof org.key_project.rusty.parser.hir.QPath.Resolved(HirTy ty1, org.key_project.rusty.parser.hir.Path<org.key_project.rusty.parser.hir.Res> path)
                && ty1 == null
                && path.res() instanceof org.key_project.rusty.parser.hir.Res.PrimTy(PrimHirTy ty2)) {
            return convertPrimHirType(ty2);
        }
        if (ty.path() instanceof org.key_project.rusty.parser.hir.QPath.Resolved(var ty1, var path)
                &&
                path.res() instanceof org.key_project.rusty.parser.hir.Res.DefRes(var def)) {
            switch (def.kind()) {
                case DefKind.Enum ignored -> {
                    var adt = adts.get(def.id());
                    var args = new ArrayList<GenericTyArg>();
                    for (var a : path.segments()[path.segments().length - 1].args().args()) {
                        if (a instanceof GenericArg.Type(HirTy ty2)) {
                            RustType hirTy = convertHirTy(ty2);
                            args.add(new GenericTyArgType(hirTy.type()));
                        }
                    }
                    if (args.isEmpty()) {
                        return new PathRustType((Type) adt);
                    }
                    var type = ((GenericAdt) adt).instantiate(new ImmutableArray<>(args), services);
                    return new PathRustType(type);
                }
                case DefKind.Struct ignored -> {
                    var adt = adts.get(def.id());
                    var args = new ArrayList<GenericTyArg>();
                    for (var a : path.segments()[path.segments().length - 1].args().args()) {
                        if (a instanceof GenericArg.Type(HirTy ty2)) {
                            RustType hirTy = convertHirTy(ty2);
                            args.add(new GenericTyArgType(hirTy.type()));
                        }
                    }
                    if (args.isEmpty()) {
                        return new PathRustType((Type) adt);
                    }
                    var type = ((GenericAdt) adt).instantiate(new ImmutableArray<>(args), services);
                    return new PathRustType(type);
                }
                default -> {
                }
            }
            throw new IllegalArgumentException("Unknown def kind: " + def.kind());
        }
        throw new IllegalArgumentException("Unknown path type: " + ty.path());
    }

    private RustType convertMutHirTy(MutHirTy m) {
        RustType inner = convertHirTy(m.ty());
        boolean isMut = m.mutbl();
        return new ReferenceRustType(isMut, inner, ReferenceType.get(inner.type(), isMut));
    }

    private PrimitiveRustType convertPrimHirType(PrimHirTy pty) {
        var primTy = switch (pty) {
            case PrimHirTy.Bool ignored -> PrimitiveType.BOOL;
            case PrimHirTy.Uint(var uintTy) -> switch (uintTy) {
                case UintTy.U8 -> PrimitiveType.U8;
                case UintTy.U16 -> PrimitiveType.U16;
                case UintTy.U32 -> PrimitiveType.U32;
                case UintTy.U64 -> PrimitiveType.U64;
                case UintTy.U128 -> PrimitiveType.U128;
                case UintTy.Usize -> PrimitiveType.USIZE;
            };
            case PrimHirTy.Int(var intTy) -> switch (intTy) {
                case IntTy.I8 -> PrimitiveType.I8;
                case IntTy.I16 -> PrimitiveType.I16;
                case IntTy.I32 -> PrimitiveType.I32;
                case IntTy.I64 -> PrimitiveType.I64;
                case IntTy.I128 -> PrimitiveType.I128;
                case IntTy.Isize -> PrimitiveType.ISIZE;
            };
            default -> throw new IllegalArgumentException("Unknown prim type: " + pty);
        };
        return new PrimitiveRustType(primTy);
    }

    private Pattern convertPat(Pat pat, @Nullable Type ty) {
        return convertPat(pat, false, ty);
    }

    private Pattern convertPat(Pat pat, boolean isCtxFnParam, @Nullable Type ty) {
        return switch (pat.kind()) {
            case PatKind.Binding p -> {
                boolean ref = false;
                boolean mutRef = false;
                if (p.mode().byRef() instanceof ByRef.Yes y) {
                    ref = true;
                    mutRef = y.mut();
                }
                boolean mut = p.mode().mut();
                var name = new Name(convertIdent(p.ident()));
                var id = p.hirId();
                ProgramVariable pv;
                if (isCtxFnParam) {
                    pv = Objects
                            .requireNonNull(
                                services.getNamespaces().programVariables().lookup(name));
                } else {
                    pv = new ProgramVariable(name,
                        services.getRustInfo()
                                .getKeYRustyType(Objects.requireNonNull(types.get(id))));
                }
                declarePV(id, pv);
                Pattern opt =
                    p.pat() == null ? null
                            : convertPat(p.pat(), pv.getKeYRustyType().getRustyType());
                yield new BindingPattern(ref, mutRef, mut, pv, opt);
            }
            case PatKind.Wild w -> WildCardPattern.WILDCARD;
            case PatKind.Path p -> {
                yield new PathPattern();
            }
            case PatKind.Range r -> {
                var left = r.lhs() == null ? null : convertPatExpr(r.lhs(), ty);
                var right = r.rhs() == null ? null : convertPatExpr(r.rhs(), ty);
                var bounds =
                    r.inclusive() ? RangePattern.Bounds.Inclusive : RangePattern.Bounds.Exclusive;
                yield new RangePattern(left, bounds, right);
            }
            default -> throw new IllegalArgumentException("Unknown pat: " + pat);
        };
    }

    private PatExpr convertPatExpr(org.key_project.rusty.parser.hir.pat.PatExpr pe,
            @Nullable Type ty) {
        return switch (pe.kind()) {
            case PatExprKind.Lit(var l, var n) ->
                new LitPatExpr(convertLitExpr(l, Objects.requireNonNull(ty)), n);
            default -> throw new IllegalArgumentException("Unknown patExpr: " + pe);
        };
    }

    private <R, S> Path<R> convertPath(org.key_project.rusty.parser.hir.Path<S> path,
            java.util.function.Function<S, R> convertR) {
        var res = convertR.apply(path.res());
        var segments = Arrays.stream(path.segments()).map(this::convertPathSegment).toList();
        return new Path<>(res, new ImmutableArray<>(segments));
    }

    private QPath convertQPath(org.key_project.rusty.parser.hir.QPath qPath) {
        return switch (qPath) {
            case org.key_project.rusty.parser.hir.QPath.Resolved(var selfTy, var path) ->
                new QPathResolved(convertHirTy(selfTy), convertPath(path, this::convertRes));
            default -> throw new IllegalArgumentException("Unknown path: " + qPath);
        };
    }

    private PathSegment convertPathSegment(org.key_project.rusty.parser.hir.PathSegment segment) {
        return new PathSegment(segment.ident().name(), convertRes(segment.res()));
    }

    private Res convertRes(org.key_project.rusty.parser.hir.Res res) {
        return switch (res) {
            case org.key_project.rusty.parser.hir.Res.PrimTy(var ty) -> convertPrimHirType(ty);
            case org.key_project.rusty.parser.hir.Res.Local(var id) -> getPV(id);
            case org.key_project.rusty.parser.hir.Res.DefRes(var def) ->
                new ResDef(convertDef(def));
            case org.key_project.rusty.parser.hir.Res.Err e -> new ResErr();
            default -> throw new IllegalArgumentException("Unknown hirty type: " + res);
        };
    }

    private Def convertDef(org.key_project.rusty.parser.hir.Def def) {
        return switch (def.kind()) {
            case DefKind.Fn f -> {
                Function lfn =
                    Objects.requireNonNull(localFns.get(new LocalDefId(def.id().index())));
                ProgramFunction fn = services.getRustInfo().getFunction(lfn);
                // TODO: We might have to resolve this in a 2nd step to avoid this being done before
                // the fn is loaded
                yield fn;
            }
            case DefKind.Mod m -> null;
            case DefKind.Constructor(Ctor(var of, var isFnCtor)) -> {
                if (of == CtorOf.Variant) {
                    // TODO: more info
                    yield new VariantConstructor();
                } else {
                    throw new UnsupportedOperationException("Struct ctor: " + def);
                }
            }
            case DefKind.Enum e -> {
                yield null;
            }
            case DefKind.Struct e -> {
                yield null;
            }
            default -> throw new IllegalArgumentException("Unknown def: " + def);
        };
    }

    private Type convertTy(Ty ty) {
        Type type = switch (ty) {
            case Ty.Bool ignored -> PrimitiveType.BOOL;
            case Ty.Int(var i) -> switch (i) {
                case Isize -> PrimitiveType.ISIZE;
                case I8 -> PrimitiveType.I8;
                case I16 -> PrimitiveType.I16;
                case I32 -> PrimitiveType.I32;
                case I64 -> PrimitiveType.I64;
                case I128 -> PrimitiveType.I128;
            };
            case Ty.Uint(var u) -> switch (u) {
                case Usize -> PrimitiveType.USIZE;
                case U8 -> PrimitiveType.U8;
                case U16 -> PrimitiveType.U16;
                case U32 -> PrimitiveType.U32;
                case U64 -> PrimitiveType.U64;
                case U128 -> PrimitiveType.U128;
            };
            case Ty.Adt(var def, var args) -> convertAdtTy(def, args);
            case Ty.Ref(var t, var m) -> ReferenceType.get(convertTy(t), m);
            case Ty.FnDef(var id, var args) -> {
                if (id.krate() == 0) {
                    var fn = Objects.requireNonNull(localFns.get(new LocalDefId(id.index())));
                    yield new FnDefType(fn);
                }
                yield new ForeignFnType(id, convertGenericArgs(args));
            }
            case Ty.Closure c -> new Closure();
            case Ty.Never ignored -> Never.INSTANCE;
            case Ty.Tuple(var ts) ->
                TupleType.getInstance(Arrays.stream(ts).map(this::convertTy).toList());
            case Ty.Array(var arrTy, var len) -> {
                Type elementType = convertTy(arrTy);
                yield ArrayType.getInstance(elementType, convertTyConst(len), services);
            }
            case Ty.Param(var p) -> {
                assert currentParams != null;
                yield currentParams[p.index()];
            }
            default -> throw new IllegalArgumentException("Unknown ty: " + ty);
        };
        services.getRustInfo().registerType(type);
        return type;
    }

    private Adt getAdt(AdtDef def) {
        ImmutableArray<GenericTyParam> generics;
        if (def.foreignGenerics() == null)
            throw new UnsupportedOperationException("Local generics");
        else
            generics = convertGenerics(def.foreignGenerics());
        Name name1 = new Name(def.pathStr());
        var name = name1;
        var genSortParams = getGenericParameters(generics);
        Adt adt = switch (def.kind()) {
            case Struct -> {
                assert def.variants().size() == 1;
                if (generics.isEmpty()) {
                    var fields = convertFields(def.pathStr(), def.variants().get(0).fields());
                    yield new Struct(name, fields, null, null);
                } else {
                    var fields =
                        convertFields(def.pathStr(), def.variants().get(0).fields(), genSortParams);
                    var sortDecl = getSortDecl(name, genSortParams);
                    yield new GenericStruct(name, fields, generics, sortDecl);
                }
            }
            case Union -> null;
            case Enum -> {
                if (generics.isEmpty()) {
                    var variants = new Variant[def.variants().size()];
                    for (var e : def.variants().entrySet()) {
                        VariantDef value = e.getValue();
                        variants[e.getKey()] =
                            new Variant(name,
                                convertFields(def.pathStr() + value.name(), value.fields()));
                    }
                    var sort = new SortImpl(name1, false);
                    services.getNamespaces().sorts().addSafely(sort);
                    yield new Enum(name1, new ImmutableArray<>(variants), sort);
                }
                var sortDecl = getSortDecl(name, genSortParams);
                var variants = new GenericVariant[def.variants().size()];
                for (var e : def.variants().entrySet()) {
                    VariantDef value = e.getValue();
                    variants[e.getKey()] =
                        new GenericVariant(name, convertFields(def.pathStr() + value.name(),
                            value.fields(), genSortParams));
                }
                yield new GenericEnum(name, new ImmutableArray<>(variants), generics, sortDecl);
            }
        };
        currentParams = null;
        return adt;
    }

    public ParametricSortDecl getSortDecl(Name name, ImmutableList<GenericParameter> params) {
        var psd = new ParametricSortDecl(name, false, params, null);
        var alreadyDefined = services.getNamespaces().parametricSorts().lookup(psd.name());
        if (alreadyDefined != null) {
            return alreadyDefined;
        } else {
            services.getNamespaces().parametricSorts().addSafely(psd);
            return psd;
        }
    }

    private ImmutableList<GenericParameter> getGenericParameters(
            ImmutableArray<GenericTyParam> params) {
        if (params.isEmpty())
            return null;
        ImmutableList<GenericParameter> sortParams = ImmutableSLList.nil();
        for (int i = params.size() - 1; i >= 0; i--) {
            sortParams = sortParams.prepend(params.get(i).toSortParam(services));
        }
        return sortParams;
    }

    private Type convertAdtTy(AdtDef def, GenericTyArgKind[] args) {
        Adt adt = adts.get(def.did());
        return switch (adt) {
            case GenericEnum g -> g.instantiate(convertGenericArgs(args), services);
            case Enum e -> e;
            case GenericStruct g -> g.instantiate(convertGenericArgs(args), services);
            case Struct s -> s;
            default -> throw new IllegalArgumentException("Unknown adt: " + adt);
        };

    }

    private ImmutableArray<Field> convertFields(String prefix, Map<Integer, TyFieldDef> fields) {
        var res = new Field[fields.size()];
        for (var e : fields.entrySet()) {
            var field = e.getValue();
            Type type = convertTy(field.ty());
            Name name = new Name(field.name());
            var fieldConst = services.getLDTs().getFieldLDT().createField(prefix, name, type);
            res[e.getKey()] = new Field(name, type, fieldConst);
        }
        return new ImmutableArray<>(res);
    }

    private ImmutableArray<GenericField> convertFields(String prefix,
            Map<Integer, TyFieldDef> fields, ImmutableList<GenericParameter> generics) {
        var res = new GenericField[fields.size()];
        for (var e : fields.entrySet()) {
            var field = e.getValue();
            Type type = convertTy(field.ty());
            Name name = new Name(field.name());
            var fieldConst =
                services.getLDTs().getFieldLDT().createGenericField(prefix, name, type, generics);
            res[e.getKey()] = new GenericField(name, type, fieldConst);
        }
        return new ImmutableArray<>(res);
    }

    private ImmutableArray<GenericTyArg> convertGenericArgs(GenericTyArgKind[] args) {
        var res = new ArrayList<GenericTyArg>();
        for (var e : args) {
            switch (e) {
                case GenericTyArgKind.Const c ->
                    throw new UnsupportedOperationException("TODO: const generic arg");
                case GenericTyArgKind.Type t -> res.add(new GenericTyArgType(convertTy(t.ty())));
                case GenericTyArgKind.Lifetime ignored -> {
                }
                default -> throw new IllegalArgumentException("Unknown arg type: " + e);
            }
        }
        return new ImmutableArray<>(res);
    }

    private ImmutableArray<GenericTyParam> convertGenerics(TyGenerics generics) {
        var res = new ArrayList<GenericTyParam>();
        assert currentParams == null;
        currentParams = new GenericTyParam[generics.params().length];
        for (var p : generics.params()) {
            Name name = new Name(p.name());
            if (p.kind() instanceof TyGenericParamDefKind.Type) {
                GenericTyParam tyParam = new GenericTyParam(name, false, new GenericSort(name));
                currentParams[p.index()] = tyParam;
                res.add(tyParam);
            } else if (p.kind() instanceof TyGenericParamDefKind.Const) {
                GenericTyParam tyParam = new GenericTyParam(name, true, null);
                currentParams[p.index()] = tyParam;
                res.add(tyParam);
            }
        }
        return new ImmutableArray<>(res);
    }

    // TODO: something other than int
    private int convertTyConst(TyConst tc) {
        var vc = (TyConst.ValueConst) tc;
        return (int) switch (vc.value().valtree()) {
            case ValTree.Leaf(var si) -> si.data();
            default -> throw new IllegalArgumentException("Unknown ty const: " + tc);
        };
    }
}
