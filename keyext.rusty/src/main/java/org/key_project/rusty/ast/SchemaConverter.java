/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast;

import java.math.BigInteger;
import java.util.*;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.abstraction.KeYRustyType;
import org.key_project.rusty.ast.abstraction.PrimitiveType;
import org.key_project.rusty.ast.abstraction.SchemaType;
import org.key_project.rusty.ast.expr.*;
import org.key_project.rusty.ast.fn.Function;
import org.key_project.rusty.ast.fn.FunctionParam;
import org.key_project.rusty.ast.fn.FunctionParamPattern;
import org.key_project.rusty.ast.pat.*;
import org.key_project.rusty.ast.stmt.EmptyStatement;
import org.key_project.rusty.ast.stmt.ExpressionStatement;
import org.key_project.rusty.ast.stmt.LetStatement;
import org.key_project.rusty.ast.stmt.Statement;
import org.key_project.rusty.ast.ty.*;
import org.key_project.rusty.logic.op.ProgramVariable;
import org.key_project.rusty.logic.op.sv.OperatorSV;
import org.key_project.rusty.logic.op.sv.ProgramSV;
import org.key_project.rusty.logic.sort.ProgramSVSort;
import org.key_project.rusty.parsing.RustySchemaParser;
import org.key_project.rusty.rule.metaconstruct.ExpandFnBody;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class SchemaConverter {
    private Namespace<@NonNull SchemaVariable> svNS;

    // TODO: Rework this properly
    private final Map<String, VariableDeclaration> variables = new HashMap<>();
    private final Map<VariableDeclaration, ProgramVariable> programVariables = new HashMap<>();

    private final Services services;
    /// Whether the converter is in declaration mode, i.e., any IdentPattern encountered must create
    /// a new PV.
    private boolean inDeclarationMode = false;
    private @Nullable ProgramVariable declaredVariable = null;
    private @Nullable KeYRustyType declaredType = null;
    private boolean inContextFunction = false;

    public SchemaConverter(Namespace<@NonNull SchemaVariable> svNS, Services services) {
        this.svNS = svNS;
        this.services = services;
    }

    public Services getServices() {
        return services;
    }

    private VariableDeclaration getDecl(PathInExpression path) {
        // TODO: For now, only use local vars, i.e., ignore all but the last segment
        return Objects.requireNonNull(variables.get(
            Objects.requireNonNull(path.segments().last()).segment().ident().name().toString()));
    }

    private ProgramVariable getProgramVariable(PathInExpression path) {
        return Objects.requireNonNull(programVariables.get(getDecl(path)));
    }

    private @NonNull ProgramVariable getProgramVariable(Identifier path) {
        return Objects.requireNonNull(programVariables.get(getDecl(path)));
    }

    private VariableDeclaration getDecl(Identifier path) {
        return Objects.requireNonNull(variables.get(path.name().toString()));
    }

    private Label getLabel(String name) {
        throw new UnsupportedOperationException("TODO @ DD : implement getLabel");
    }

    public OperatorSV lookupSchemaVariable(String s) {
        OperatorSV sv;
        SchemaVariable n = svNS.lookup(new Name(s));
        if (n instanceof OperatorSV asv) {
            sv = asv;
        } else {
            throw new RuntimeException("Schema variable not declared: " + s);
        }
        return sv;
    }

    private Crate convertCrate(
            RustySchemaParser.CrateContext ctx) {
        return new Crate(new Mod(ctx.item().stream().map(this::convertItem)
                .collect(ImmutableList.collector())));
    }

    private Item convertItem(RustySchemaParser.ItemContext ctx) {
        // TODO: Rework
        return convertFunction(ctx.function_());
    }

    public Function convertFunction(
            RustySchemaParser.Function_Context ctx) {
        Name name = convertIdentifier(ctx.identifier()).name();
        if (name.toString().equals(Context.TMP_FN_NAME))
            inContextFunction = true;
        ImmutableArray<FunctionParam> params =
            convertFunctionParams(ctx.functionParams());
        inContextFunction = false;
        RustType returnType = ctx.functionRetTy() == null ? TupleRustType.UNIT
                : convertRustType(ctx.functionRetTy().type_());
        BlockExpression body =
            convertBlockExpr(
                ctx.blockExpr());
        var self = ctx.functionParams() == null ? null : ctx.functionParams().selfParam();
        var kind = Function.ImplicitSelfKind.None;
        if (self != null) {
            if (self.shorthandSelf() != null) {
                if (self.shorthandSelf().AND() != null) {
                    if (self.shorthandSelf().KW_MUT() != null) {
                        kind = Function.ImplicitSelfKind.RefMut;
                    } else {
                        kind = Function.ImplicitSelfKind.RefImm;
                    }
                } else {
                    if (self.shorthandSelf().KW_MUT() != null) {
                        kind = Function.ImplicitSelfKind.Mut;
                    } else {
                        kind = Function.ImplicitSelfKind.Imm;
                    }
                }
            }
        }
        return new Function(name, kind,
            params,
            returnType,
            body);
    }

    private Expr convertExpr(RustySchemaParser.ExprContext ctx) {
        if (ctx instanceof RustySchemaParser.LiteralExpressionContext lit)
            return convertLiteralExpr(lit.literalExpr());
        if (ctx instanceof RustySchemaParser.PathExpressionContext path)
            return convertPathExpr(path.pathExpr());
        if (ctx instanceof RustySchemaParser.MethodCallExpressionContext x)
            return convertMethodCallExpression(x);
        if (ctx instanceof RustySchemaParser.FieldExpressionContext x)
            return convertFieldExpression(x);
        if (ctx instanceof RustySchemaParser.TupleIndexingExpressionContext x)
            return convertTupleIndexingExpression(x);
        if (ctx instanceof RustySchemaParser.CallExpressionContext x)
            return convertCallExpression(x);
        if (ctx instanceof RustySchemaParser.IndexExpressionContext x)
            return convertIndexExpression(x);
        if (ctx instanceof RustySchemaParser.BorrowExpressionContext x)
            return convertBorrowExpression(x);
        if (ctx instanceof RustySchemaParser.DereferenceExpressionContext x)
            return convertDereferenceExpression(x);
        if (ctx instanceof RustySchemaParser.NegationExpressionContext x)
            return convertNegationExpression(x);
        if (ctx instanceof RustySchemaParser.TypeCastExpressionContext x)
            return convertTypeCastExpression(x);
        if (ctx instanceof RustySchemaParser.ArithmeticOrLogicalExpressionContext ale)
            return convertArithmeticOrLogicalExpression(ale);
        if (ctx instanceof RustySchemaParser.ComparisonExpressionContext x)
            return convertComparisonExpression(x);
        if (ctx instanceof RustySchemaParser.LazyBooleanExpressionContext x)
            return convertLazyBooleanExpression(x);
        if (ctx instanceof RustySchemaParser.AssignmentExpressionContext ae)
            return convertAssignmentExpression(ae);
        if (ctx instanceof RustySchemaParser.CompoundAssignmentExpressionContext x)
            return convertCompoundAssignmentExpression(x);
        if (ctx instanceof RustySchemaParser.ContinueExpressionContext x)
            return convertContinueExpression(x);
        if (ctx instanceof RustySchemaParser.BreakExpressionContext x)
            return convertBreakExpression(x);
        if (ctx instanceof RustySchemaParser.ReturnExpressionContext x)
            return convertReturnExpression(x);
        if (ctx instanceof RustySchemaParser.GroupedExpressionContext x)
            return convertGroupedExpression(x);
        if (ctx instanceof RustySchemaParser.ArrayExpressionContext x)
            if (x.arrayElements() == null || x.arrayElements().SEMI() == null)
                return convertEnumeratedArrayExpression(x);
            else
                return convertRepeatedArrayExpression(x);
        else if (ctx instanceof RustySchemaParser.TupleExpressionContext x)
            return convertTupleExpression(x);
        else if (ctx instanceof RustySchemaParser.StructExpression_Context x) {
            if (x.structExpr().structExprUnit() != null)
                return convertUnitStructExpression(x.structExpr().structExprUnit());
            if (x.structExpr().structExprTuple() != null)
                return convertTupleStructExpression(x.structExpr().structExprTuple());
            if (x.structExpr().structExprStruct() != null)
                return convertStructStructExpression(x.structExpr().structExprStruct());
        } else if (ctx instanceof RustySchemaParser.EnumerationVariantExpression_Context x) {
            if (x.enumerationVariantExpr().enumExprStruct() != null)
                return convertEnumVariantStruct(x.enumerationVariantExpr().enumExprStruct());
            if (x.enumerationVariantExpr().enumExprTuple() != null)
                return convertEnumVariantTuple(x.enumerationVariantExpr().enumExprTuple());
            if (x.enumerationVariantExpr().enumExprFieldless() != null)
                return convertEnumVariantFieldless(x.enumerationVariantExpr().enumExprFieldless());
        }
        if (ctx instanceof RustySchemaParser.ClosureExpression_Context x)
            return convertClosureExpression(x.closureExpr());
        if (ctx instanceof RustySchemaParser.ExpressionWithBlock_Context x)
            return convertExprWithBlock(x.exprWithBlock());
        if (ctx instanceof RustySchemaParser.SchemaVarExpressionContext se)
            return convertSchemaVarExpression(se);
        if (ctx instanceof RustySchemaParser.EmptyPanicContext p)
            return new EmptyPanic();
        if (ctx instanceof RustySchemaParser.ExpandFnBodyContext e)
            return new ExpandFnBody(
                (ProgramSV) lookupSchemaVariable(e.schemaVariable().getText().substring(2)));
        if (ctx instanceof RustySchemaParser.FnFrameContext ff) {
            var resultVar =
                (ProgramSV) lookupSchemaVariable(ff.schemaVariable().getText().substring(2));
            @SuppressWarnings("argument.type.incompatible")
            FunctionFrame functionFrame =
                new FunctionFrame(resultVar, null, convertBlockExpr(ff.blockExpr()));
            return functionFrame;
        }
        if (ctx instanceof RustySchemaParser.ConstBlockSchemaContext cb) {
            var sv =
                (ProgramSV) lookupSchemaVariable(cb.schemaVariable().getText().substring(2));
            return new ConstBlockExpression(sv);
        }
        throw new UnsupportedOperationException(
            "Unknown expr: " + ctx.getText() + " class: " + ctx.getClass());
    }

    private Expr convertLiteralExpr(
            RustySchemaParser.LiteralExprContext ctx) {
        if (ctx.KW_TRUE() != null)
            return new BooleanLiteralExpression(true);
        if (ctx.KW_FALSE() != null)
            return new BooleanLiteralExpression(false);
        var intLit = ctx.INTEGER_LITERAL();
        if (intLit != null) {
            var text = intLit.getText();
            return getIntegerLiteralExpression(text);
        }

        throw new IllegalArgumentException("Expected boolean or integer literal");
    }

    private static IntegerLiteralExpression getIntegerLiteralExpression(String text) {
        var signed = text.contains("i");
        var split = text.split("[ui]");
        var size = split[split.length - 1];
        var suffix = IntegerLiteralExpression.IntegerSuffix.get(signed, size);
        var lit = split[0];

        var value = new BigInteger(
            lit);
        var ty = switch (suffix) {
            case None -> throw new RuntimeException("Missing suffix");
            case u8 -> PrimitiveType.U8;
            case u16 -> PrimitiveType.U16;
            case u32 -> PrimitiveType.U32;
            case u64 -> PrimitiveType.U64;
            case u128 -> PrimitiveType.U128;
            case usize -> PrimitiveType.USIZE;
            case IntegerLiteralExpression.IntegerSuffix.i8 -> PrimitiveType.I8;
            case IntegerLiteralExpression.IntegerSuffix.i16 -> PrimitiveType.I16;
            case IntegerLiteralExpression.IntegerSuffix.i32 -> PrimitiveType.I32;
            case IntegerLiteralExpression.IntegerSuffix.i64 -> PrimitiveType.I64;
            case IntegerLiteralExpression.IntegerSuffix.i128 -> PrimitiveType.I128;
            case isize -> PrimitiveType.ISIZE;
        };
        return new IntegerLiteralExpression(value, suffix, ty);
    }

    private ProgramVariable convertPathExpr(
            RustySchemaParser.PathExprContext ctx) {
        if (ctx.qualifiedPathInExpr() != null)
            throw new IllegalArgumentException("TODO @ DD: Qual path");
        else {
            var pieCtx = ctx.pathInExpr();
            var segments =
                pieCtx.pathExprSegment().stream().map(this::convertPathExprSegment).toList();
            var pie = new PathInExpression(new ImmutableArray<>(segments));
            return Objects.requireNonNull(getProgramVariable(pie));
        }
    }

    private MethodCallExpression convertMethodCallExpression(
            RustySchemaParser.MethodCallExpressionContext ctx) {
        var callee = convertExpr(ctx.expr());
        var seg = convertPathExprSegment(ctx.pathExprSegment());
        ImmutableArray<Expr> params = ctx.callParams() == null ? new ImmutableArray<>()
                : new ImmutableArray<>(
                    ctx.callParams().expr().stream().map(this::convertExpr).toList());
        return new MethodCallExpression(callee,
            new PathSegment(seg.segment().ident().toString(), null), params);
    }

    private FieldExpression convertFieldExpression(
            RustySchemaParser.FieldExpressionContext ctx) {
        var base = convertExpr(ctx.expr());
        if (ctx.identifier() != null) {
            throw new UnsupportedOperationException();
        } else {
            var sv = ctx.schemaVariable();
            return new FieldExpression(base,
                (ProgramSV) lookupSchemaVariable(sv.SCHEMA_IDENTIFIER().getText().substring(2)));
        }
    }

    private FieldExpression convertTupleIndexingExpression(
            RustySchemaParser.TupleIndexingExpressionContext ctx) {
        var base = convertExpr(ctx.expr());
        var idx = new Identifier(new Name(ctx.tupleIndex().INTEGER_LITERAL().getText()));
        return new FieldExpression(base, new FieldIdentifier(idx, null));
    }

    private CallExpression convertCallExpression(
            RustySchemaParser.CallExpressionContext ctx) {
        var callee = convertExpr(ctx.expr());
        ImmutableArray<Expr> params = ctx.callParams() == null ? new ImmutableArray<>()
                : new ImmutableArray<>(
                    ctx.callParams().expr().stream().map(this::convertExpr).toList());
        return new CallExpression(callee, params);
    }

    private IndexExpression convertIndexExpression(
            RustySchemaParser.IndexExpressionContext ctx) {
        var base = convertExpr(ctx.expr(0));
        var idx = convertExpr(ctx.expr(1));
        return new IndexExpression(base, idx, null);
    }

    private BorrowExpression convertBorrowExpression(
            RustySchemaParser.BorrowExpressionContext ctx) {
        var base = convertExpr(ctx.expr());
        var e = new BorrowExpression(ctx.KW_MUT() != null, base);
        if (ctx.ANDAND() != null) {
            e = new BorrowExpression(false, e);
        }
        return e;
    }

    private UnaryExpression convertDereferenceExpression(
            RustySchemaParser.DereferenceExpressionContext ctx) {
        var base = convertExpr(ctx.expr());
        return new UnaryExpression(UnaryExpression.Operator.Deref, base);
    }

    private UnaryExpression convertNegationExpression(
            RustySchemaParser.NegationExpressionContext ctx) {
        var base = convertExpr(ctx.expr());
        var op =
            ctx.NOT() != null ? UnaryExpression.Operator.Not : UnaryExpression.Operator.Neg;
        return new UnaryExpression(op, base);
    }

    private TypeCastExpression convertTypeCastExpression(
            RustySchemaParser.TypeCastExpressionContext ctx) {
        var base = convertExpr(ctx.expr());
        var ty = convertTypeNoBounds(ctx.typeNoBounds());
        return new TypeCastExpression(base, ty);
    }

    private Expr convertArithmeticOrLogicalExpression(
            RustySchemaParser.ArithmeticOrLogicalExpressionContext ctx) {
        BinaryExpression.Operator op = null;
        if (ctx.AND() != null)
            op = BinaryExpression.Operator.BitAnd;
        if (ctx.OR() != null)
            op = BinaryExpression.Operator.BitOr;
        if (ctx.CARET() != null)
            op = BinaryExpression.Operator.BitXor;
        if (ctx.PLUS() != null)
            op = BinaryExpression.Operator.Add;
        if (ctx.MINUS() != null)
            op = BinaryExpression.Operator.Sub;
        if (ctx.PERCENT() != null)
            op = BinaryExpression.Operator.Rem;
        if (ctx.STAR() != null)
            op = BinaryExpression.Operator.Mul;
        if (ctx.SLASH() != null)
            op = BinaryExpression.Operator.Div;
        if (ctx.shl() != null)
            op = BinaryExpression.Operator.Shl;
        if (ctx.shr() != null)
            op = BinaryExpression.Operator.Shr;
        assert op != null;
        return new BinaryExpression(op, convertExpr(ctx.expr(0)),
            convertExpr(ctx.expr(1)));
    }

    private BinaryExpression convertComparisonExpression(
            RustySchemaParser.ComparisonExpressionContext ctx) {
        var left = convertExpr(ctx.expr(0));
        var right = convertExpr(ctx.expr(1));
        var opCtx = ctx.comparisonOperator();
        var op = opCtx.EQEQ() != null ? BinaryExpression.Operator.Eq
                : opCtx.GT() != null ? BinaryExpression.Operator.Gt
                        : opCtx.LT() != null ? BinaryExpression.Operator.Lt
                                : opCtx.NE() != null ? BinaryExpression.Operator.Ne
                                        : opCtx.GE() != null
                                                ? BinaryExpression.Operator.Ge
                                                : opCtx.LE() != null
                                                        ? BinaryExpression.Operator.Le
                                                        : null;
        assert op != null;
        return new BinaryExpression(op, left, right);
    }

    private BinaryExpression convertLazyBooleanExpression(
            RustySchemaParser.LazyBooleanExpressionContext ctx) {
        var left = convertExpr(ctx.expr(0));
        var right = convertExpr(ctx.expr(1));
        var op = ctx.ANDAND() != null ? BinaryExpression.Operator.And
                : BinaryExpression.Operator.Or;
        return new BinaryExpression(op, left, right);
    }

    public AssignmentExpression convertAssignmentExpression(
            RustySchemaParser.AssignmentExpressionContext ctx) {
        var lhs = convertExpr(ctx.expr(0));
        var rhs = convertExpr(ctx.expr(1));
        return new AssignmentExpression(lhs, rhs);
    }

    private CompoundAssignmentExpression convertCompoundAssignmentExpression(
            RustySchemaParser.CompoundAssignmentExpressionContext ctx) {
        var left = convertExpr(ctx.expr(0));
        var right = convertExpr(ctx.expr(1));
        var opCtx = ctx.compoundAssignOperator();
        var op = opCtx.PLUSEQ() != null ? BinaryExpression.Operator.Add
                : opCtx.MINUSEQ() != null ? BinaryExpression.Operator.Sub
                        : opCtx.STAREQ() != null ? BinaryExpression.Operator.Mul
                                : opCtx.PERCENTEQ() != null
                                        ? BinaryExpression.Operator.Rem
                                        : opCtx.ANDEQ() != null
                                                ? BinaryExpression.Operator.BitAnd
                                                : opCtx.OREQ() != null
                                                        ? BinaryExpression.Operator.BitOr
                                                        : opCtx.CARETEQ() != null
                                                                ? BinaryExpression.Operator.BitXor
                                                                : opCtx.SHLEQ() != null
                                                                        ? BinaryExpression.Operator.Shl
                                                                        : BinaryExpression.Operator.Shr;
        return new CompoundAssignmentExpression(left, op, right);
    }

    private ContinueExpression convertContinueExpression(
            RustySchemaParser.ContinueExpressionContext ctx) {
        var label = ctx.label() != null ? convertLabel(ctx.label()) : null;
        return new ContinueExpression(label);
    }

    private BreakExpression convertBreakExpression(
            RustySchemaParser.BreakExpressionContext ctx) {
        var label = ctx.label() != null ? convertLabel(ctx.label()) : null;
        if (label instanceof SchemaLabel(ProgramSV sv)
                && sv.sort() != ProgramSVSort.LABEL) {
            return new BreakExpression(null, sv);
        }
        var expr = ctx.expr() != null ? convertExpr(ctx.expr()) : null;
        return new BreakExpression(label, expr);
    }

    private ReturnExpression convertReturnExpression(
            RustySchemaParser.ReturnExpressionContext ctx) {
        var expr = ctx.expr() != null ? convertExpr(ctx.expr()) : null;
        return new ReturnExpression(expr);
    }

    private Expr convertGroupedExpression(
            RustySchemaParser.GroupedExpressionContext ctx) {
        return convertExpr(ctx.expr());
    }

    private ArrayExpression convertEnumeratedArrayExpression(
            RustySchemaParser.ArrayExpressionContext ctx) {
        if (ctx.arrayElements() == null)
            return new ArrayExpression(new ImmutableArray<>(), null);
        assert ctx.arrayElements().SEMI() == null;
        return new ArrayExpression(new ImmutableArray<>(
            ctx.arrayElements().expr().stream().map(this::convertExpr).toList()), null);
    }

    private RepeatExpression convertRepeatedArrayExpression(
            RustySchemaParser.ArrayExpressionContext ctx) {
        return new RepeatExpression(convertExpr(ctx.arrayElements().expr(0)),
            convertExpr(ctx.arrayElements().expr(1)), null);
    }

    private TupleExpression convertTupleExpression(
            RustySchemaParser.TupleExpressionContext ctx) {
        if (ctx.tupleElements() == null)
            return TupleExpression.UNIT;
        return new TupleExpression(new ImmutableArray<>(
            ctx.tupleElements().expr().stream().map(this::convertExpr).toList()), null);
    }

    private PathExpr convertUnitStructExpression(
            RustySchemaParser.StructExprUnitContext ctx) {
        throw new UnsupportedOperationException("TODO @ DD: Unit struct expr");
    }

    private CallExpression convertTupleStructExpression(
            RustySchemaParser.StructExprTupleContext ctx) {
        throw new UnsupportedOperationException("TODO @ DD: Tuple struct expr");
    }

    private StructExpression convertStructStructExpression(
            RustySchemaParser.StructExprStructContext ctx) {
        throw new UnsupportedOperationException("TODO @ DD: Field struct expr");
    }

    private PathExpr convertEnumVariantFieldless(
            RustySchemaParser.EnumExprFieldlessContext ctx) {
        throw new UnsupportedOperationException("TODO @ DD: Fieldless enum variant expr");
    }

    private Expr convertEnumVariantTuple(
            RustySchemaParser.EnumExprTupleContext ctx) {
        throw new UnsupportedOperationException("TODO @ DD: Tuple enum variant expr");
    }

    private Expr convertEnumVariantStruct(
            RustySchemaParser.EnumExprStructContext ctx) {
        throw new UnsupportedOperationException("TODO @ DD: Struct enum variant expr");
    }

    private ClosureExpression convertClosureExpression(
            RustySchemaParser.ClosureExprContext ctx) {
        ImmutableArray<ClosureParam> params =
            ctx.closureParameters() == null ? new ImmutableArray<>()
                    : new ImmutableArray<>(ctx.closureParameters().closureParam().stream()
                            .map(this::convertClosureParam).toList());
        var ty = ctx.typeNoBounds() == null ? null : convertTypeNoBounds(ctx.typeNoBounds());
        var body = ctx.expr() == null ? convertBlockExpr(ctx.blockExpr()) : convertExpr(ctx.expr());
        return new ClosureExpression(ctx.KW_MOVE() != null, params, ty, body);
    }

    private Expr convertExprWithBlock(
            RustySchemaParser.ExprWithBlockContext ctx) {
        if (ctx.blockExpr() != null)
            return convertBlockExpr(ctx.blockExpr());
        if (ctx.loopExpr() != null)
            return convertLoopExpr(ctx.loopExpr());
        if (ctx.ifExpr() != null)
            return convertIfExpr(ctx.ifExpr());
        if (ctx.ifLetExpr() != null)
            return convertIfLetExpr(ctx.ifLetExpr());
        return convertMatchExpr(ctx.matchExpr());
    }

    private BlockExpression convertBlockExpr(
            RustySchemaParser.BlockExprContext ctx) {
        if (ctx instanceof RustySchemaParser.ContextBlockExprContext cctx) {
            return convertContextBlockExpr(cctx);
        }
        return convertStandardBlockExpr((RustySchemaParser.StandardBlockExprContext) ctx);
    }

    private BlockExpression convertStandardBlockExpr(
            RustySchemaParser.StandardBlockExprContext ctx) {
        var stmtsCtx = ctx.stmts();

        if (stmtsCtx == null)
            return new BlockExpression(ImmutableSLList.nil(), null);

        var stmts = stmtsCtx.stmt().stream().map(this::convertStmt)
                .collect(ImmutableList.collector());
        if (!stmts.isEmpty() && stmts.get(stmts.size() - 1) instanceof ProgramSV psv
                && (psv.sort() == ProgramSVSort.EXPRESSION
                        || psv.sort() == ProgramSVSort.SIMPLE_EXPRESSION
                        || psv.sort() == ProgramSVSort.NON_SIMPLE_EXPRESSION
                        || psv.sort() == ProgramSVSort.BOOL_EXPRESSION
                        || psv.sort() == ProgramSVSort.SIMPLE_BOOL_EXPRESSION
                        || psv.sort() == ProgramSVSort.NON_SIMPLE_BOOL_EXPRESSION)) {
            ImmutableList<Statement> firstStmts = ImmutableSLList.nil();
            for (int i = 0; i < stmts.size() - 1; i++) {
                firstStmts = firstStmts.append(stmts.get(i));
            }
            return new BlockExpression(firstStmts, psv);
        }
        var value = stmtsCtx.expr() == null ? null : convertExpr(stmtsCtx.expr());

        return new BlockExpression(stmts, value);
    }

    private ContextBlockExpression convertContextBlockExpr(
            RustySchemaParser.ContextBlockExprContext ctx) {
        var stmtsCtx = ctx.stmts();

        if (stmtsCtx == null)
            return new ContextBlockExpression(ImmutableSLList.nil(), null);

        ImmutableList<Statement> stmts = stmtsCtx.stmt().stream().map(this::convertStmt)
                .collect(ImmutableList.collector());
        if (!stmts.isEmpty()) {
            var last = stmts.get(stmts.size() - 1);
            ImmutableList<Statement> firstStmts = ImmutableSLList.nil();
            for (int i = 0; i < stmts.size() - 1; i++) {
                firstStmts = firstStmts.append(stmts.get(i));
            }
            if (last instanceof ProgramSV psv
                    && (psv.sort() == ProgramSVSort.EXPRESSION
                            || psv.sort() == ProgramSVSort.SIMPLE_EXPRESSION
                            || psv.sort() == ProgramSVSort.NON_SIMPLE_EXPRESSION
                            || psv.sort() == ProgramSVSort.BOOL_EXPRESSION
                            || psv.sort() == ProgramSVSort.SIMPLE_BOOL_EXPRESSION
                            || psv.sort() == ProgramSVSort.NON_SIMPLE_BOOL_EXPRESSION)) {
                return new ContextBlockExpression(firstStmts, psv);
            } else if (last instanceof ExpressionStatement es && !es.hasSemi()) {
                return new ContextBlockExpression(firstStmts, es.getExpression());
            }
        }
        var value = stmtsCtx.expr() == null ? null : convertExpr(stmtsCtx.expr());

        return new ContextBlockExpression(stmts, value);
    }

    public ProgramSV convertSchemaVarExpression(
            RustySchemaParser.SchemaVarExpressionContext ctx) {
        return (ProgramSV) lookupSchemaVariable(ctx.schemaVariable().getText().substring(2));
    }

    private LoopExpression convertLoopExpr(
            RustySchemaParser.LoopExprContext ctx) {
        var label =
            ctx.loopLabel() == null ? null : convertLabel(ctx.loopLabel().label());
        if (ctx.infiniteLoopExpr() != null)
            return convertInfiniteLoopExpr(ctx.infiniteLoopExpr(), label);
        if (ctx.loopScope() != null)
            return convertLoopScope(ctx.loopScope());
        throw new UnsupportedOperationException("Unknown loop: " + ctx.getText());
    }

    private LoopScope convertLoopScope(RustySchemaParser.LoopScopeContext ctx) {
        var sv = (ProgramSV) lookupSchemaVariable(ctx.schemaVariable().getText().substring(2));
        return new LoopScope(sv, convertBlockExpr(ctx.blockExpr()));
    }

    private InfiniteLoopExpression convertInfiniteLoopExpr(
            RustySchemaParser.InfiniteLoopExprContext ctx,
            @Nullable Label label) {
        Expr body;
        if (ctx.block != null)
            body = convertBlockExpr(ctx.block);
        else
            body = (ProgramSV) lookupSchemaVariable(ctx.schemaVariable().getText().substring(2));
        return new InfiniteLoopExpression(label, body);
    }

    private IfExpression convertIfExpr(
            RustySchemaParser.IfExprContext ctx) {
        var cond = convertExpr(ctx.expr());
        ThenBranch then = ctx.thenBlock != null ? convertBlockExpr(ctx.thenBlock)
                : (ProgramSV) lookupSchemaVariable(ctx.thenSV.getText().substring(2));
        ElseBranch else_ = ctx.elseBlock != null ? convertBlockExpr(ctx.elseBlock)
                : ctx.elseIf != null ? convertIfExpr(ctx.ifExpr())
                        : ctx.elseSV != null
                                ? (ProgramSV) lookupSchemaVariable(
                                    ctx.elseSV.getText().substring(2))
                                : null;
        return new IfExpression(cond, then, else_, null);
    }

    private IfExpression convertIfLetExpr(
            RustySchemaParser.IfLetExprContext ctx) {
        var pat = ctx.pattern() != null ? convertPattern(ctx.pattern())
                : (ProgramSV) lookupSchemaVariable(ctx.patternSV.getText().substring(2));
        var expr = convertExpr(ctx.expr());
        var then = ctx.thenBlock != null ? convertBlockExpr(ctx.thenBlock)
                : (ProgramSV) lookupSchemaVariable(ctx.thenSV.getText().substring(2));
        var else_ = ctx.elseBlock != null ? convertBlockExpr(ctx.elseBlock)
                : ctx.elseIf != null ? convertIfExpr(ctx.elseIf)
                        : ctx.elseIfLet != null ? convertIfLetExpr(ctx.elseIfLet)
                                : ctx.elseSV != null
                                        ? (ProgramSV) lookupSchemaVariable(
                                            ctx.elseSV.getText().substring(2))
                                        : null;
        return new IfExpression(new LetExpression(pat, null, expr), then, else_,
            null);
    }

    private MatchExpression convertMatchExpr(
            RustySchemaParser.MatchExprContext ctx) {
        var expr = convertExpr(ctx.expr());
        ImmutableArray<MatchArm> arms = ctx.matchArms() == null ? new ImmutableArray<>()
                : convertMatchArms(ctx.matchArms());
        return new MatchExpression(expr, arms);
    }

    private ImmutableArray<MatchArm> convertMatchArms(
            RustySchemaParser.MatchArmsContext ctx) {
        if (ctx.expr() != null) {
            var arms = new MatchArm[ctx.matchArm().size()];
            for (int i = 0; i < ctx.matchArm().size() - 1; i++) {
                var armCtx = ctx.matchArm().get(i);
                var pat = convertPattern(armCtx.pattern());
                var expr = armCtx.matchArmGuard() == null ? null
                        : convertExpr(armCtx.matchArmGuard().expr());
                var armExprCtx = ctx.matchArmExpression(i);
                var body = armExprCtx.expr() != null ? convertExpr(armExprCtx.expr())
                        : convertExprWithBlock(armExprCtx.exprWithBlock());
                arms[i] = new MatchArm(pat, expr, body);
            }
            var armCtx = ctx.matchArm().get(arms.length - 1);
            var pat = convertPattern(armCtx.pattern());
            var expr =
                armCtx.matchArmGuard() == null ? null : convertExpr(armCtx.matchArmGuard().expr());
            arms[arms.length - 1] = new MatchArm(pat, expr, convertExpr(ctx.expr()));
            return new ImmutableArray<>(arms);
        } else {
            var arms = new MatchArm[ctx.matchArm().size()];
            for (int i = 0; i < ctx.matchArm().size(); i++) {
                var armCtx = ctx.matchArm().get(i);
                var pat = convertPattern(armCtx.pattern());
                var expr = armCtx.matchArmGuard() == null ? null
                        : convertExpr(armCtx.matchArmGuard().expr());
                var armExprCtx = ctx.matchArmExpression(i);
                var body = armExprCtx.expr() != null ? convertExpr(armExprCtx.expr())
                        : convertExprWithBlock(armExprCtx.exprWithBlock());
                arms[i] = new MatchArm(pat, expr, body);
            }
            return new ImmutableArray<>(arms);
        }
    }

    private Statement convertExprStmt(
            RustySchemaParser.ExprStmtContext ctx) {
        if (ctx.expr() != null) {
            Expr expr = convertExpr(ctx.expr());
            if (expr instanceof ProgramSV sv && sv.sort() == ProgramSVSort.STATEMENT) {
                // A schema statement like `s#sv;` can be parsed as an expression statement with
                // expr `s#sv`.
                // Here, we convert back when the schema var has the correct sort.
                return sv;
            }
            return new ExpressionStatement(expr, ctx.SEMI() != null);
        }
        return new ExpressionStatement(convertExprWithBlock(ctx.exprWithBlock()),
            ctx.SEMI() != null);
    }

    private Statement convertLetStmt(
            RustySchemaParser.LetStmtContext ctx) {
        RustType type = ctx.type_() == null ? null : convertRustType(ctx.type_());
        declaredType =
            type == null ? null : new KeYRustyType(Objects.requireNonNull(type.getSort(services)));
        inDeclarationMode = true;
        Pattern pat = convertPatternNoTopAlt(ctx.patternNoTopAlt());
        inDeclarationMode = false;
        Expr init = ctx.expr() == null ? null : convertExpr(ctx.expr());

        LetStatement letStatement = new LetStatement(pat,
            type,
            init);
        if (!(pat instanceof SchemaVarPattern)) {
            assert declaredVariable != null;
            variables.put(declaredVariable.name().toString(), letStatement);
            programVariables.put(letStatement, Objects.requireNonNull(declaredVariable));
        }
        declaredVariable = null;
        declaredType = null;
        return letStatement;
    }

    private Statement convertStmt(
            RustySchemaParser.StmtContext ctx) {
        if (ctx.SEMI() != null && ctx.schemaStmt() == null) {
            return new EmptyStatement();
        }
        if (ctx.item() != null)
            return convertItem(ctx.item());
        if (ctx.letStmt() != null)
            return convertLetStmt(ctx.letStmt());
        if (ctx.exprStmt() != null)
            return convertExprStmt(ctx.exprStmt());
        if (ctx.schemaStmt() != null)
            return convertSchemaStmt(ctx.schemaStmt());
        throw new IllegalArgumentException("Expected statement, got: " + ctx.getText());
    }

    private Statement convertSchemaStmt(
            RustySchemaParser.SchemaStmtContext ctx) {
        return (ProgramSV) lookupSchemaVariable(ctx.schemaVariable().getText().substring(2));
    }

    private Identifier convertIdentifier(
            RustySchemaParser.IdentifierContext ctx) {
        return new Identifier(new Name(ctx.getText()));
    }

    private Pattern convertPattern(
            RustySchemaParser.PatternContext ctx) {
        var alts = ctx.patternNoTopAlt();
        if (alts.size() == 1) {
            return convertPatternNoTopAlt(alts.getFirst());
        }
        return new AltPattern(
            new ImmutableArray<>(alts.stream().map(this::convertPatternNoTopAlt).toList()));
    }

    private Pattern convertPatternNoTopAlt(
            RustySchemaParser.PatternNoTopAltContext ctx) {
        if (ctx.patternWithoutRange() != null) {
            var pat = ctx.patternWithoutRange();
            if (pat.literalPattern() != null) {
                return new LiteralPattern();
            }
            if (pat.identifierPattern() != null) {
                boolean reference = pat.identifierPattern().KW_REF() != null;
                boolean mutable = pat.identifierPattern().KW_MUT() != null;
                if (pat.identifierPattern().identifier() != null) {
                    var ident = convertIdentifier(pat.identifierPattern().identifier());
                    ProgramVariable pv;
                    if (inDeclarationMode) {
                        assert declaredVariable == null;
                        assert declaredType != null;
                        declaredVariable =
                            new ProgramVariable(ident.name(), Objects.requireNonNull(declaredType));
                        pv = declaredVariable;
                    } else if (inContextFunction) {
                        pv = services.getNamespaces().programVariables().lookup(ident.name());
                        assert pv != null;
                        declaredVariable = pv;
                    } else {
                        pv = getProgramVariable(ident);
                    }
                    return new IdentPattern(reference, mutable, pv);
                }
                return new SchemaVarPattern(reference, mutable, lookupSchemaVariable(
                    pat.identifierPattern().schemaVariable().getText().substring(2)));
            }
            if (pat.wildcardPattern() != null) {
                return WildCardPattern.WILDCARD;
            }
        }
        if (ctx.rangePattern() != null) {
            return convertRangePattern(ctx.rangePattern());
        }
        throw new IllegalArgumentException("Unknown pattern " + ctx.getText());
    }

    private Pattern convertRangePattern(
            RustySchemaParser.RangePatternContext ctx) {
        if (ctx.rangeExclusivePattern() != null)
            return convertRangeExclusivePattern(ctx.rangeExclusivePattern());
        if (ctx.rangeInclusivePattern() != null)
            return convertRangeInclusivePattern(ctx.rangeInclusivePattern());
        if (ctx.rangeFromPattern() != null)
            return convertRangeFromPattern(ctx.rangeFromPattern());
        if (ctx.rangeToInclusivePattern() != null)
            return convertRangeToInclusivePattern(ctx.rangeToInclusivePattern());
        if (ctx.obsoleteRangePattern() != null)
            return convertObsoleteRangePattern(ctx.obsoleteRangePattern());
        throw new IllegalArgumentException("Unknown range pattern " + ctx.getText());
    }

    private Pattern convertRangeExclusivePattern(
            RustySchemaParser.RangeExclusivePatternContext ctx) {
        return new RangePattern(convertRangePatternBound(ctx.rangePatternBound(0)),
            RangePattern.Bounds.Exclusive, convertRangePatternBound(ctx.rangePatternBound(1)));
    }

    private Pattern convertRangeInclusivePattern(
            RustySchemaParser.RangeInclusivePatternContext ctx) {
        return new RangePattern(convertRangePatternBound(ctx.rangePatternBound(0)),
            RangePattern.Bounds.Inclusive, convertRangePatternBound(ctx.rangePatternBound(1)));
    }

    private Pattern convertRangeFromPattern(
            RustySchemaParser.RangeFromPatternContext ctx) {
        return new RangePattern(convertRangePatternBound(ctx.rangePatternBound()),
            RangePattern.Bounds.Exclusive, null);
    }

    private Pattern convertRangeToInclusivePattern(
            RustySchemaParser.RangeToInclusivePatternContext ctx) {
        return new RangePattern(null,
            RangePattern.Bounds.Inclusive, convertRangePatternBound(ctx.rangePatternBound()));
    }

    private Pattern convertObsoleteRangePattern(
            RustySchemaParser.ObsoleteRangePatternContext ctx) {
        return new RangePattern(convertRangePatternBound(ctx.rangePatternBound(0)),
            RangePattern.Bounds.Obsolete, convertRangePatternBound(ctx.rangePatternBound(1)));
    }

    private PatExpr convertRangePatternBound(
            RustySchemaParser.RangePatternBoundContext ctx) {
        if (ctx.schemaVariable() != null) {
            return (ProgramSV) lookupSchemaVariable(ctx.schemaVariable().getText().substring(2));
        }
        if (ctx.INTEGER_LITERAL() != null) {
            var text = ctx.INTEGER_LITERAL().getText();
            var litExpr = getIntegerLiteralExpression(text);
            return new LitPatExpr(litExpr, ctx.MINUS() != null);
        }
        // TODO implement more bounds (char, byte, float, pathexpression)
        throw new IllegalArgumentException("Unknown rangePatternBound: " + ctx.getText());
    }

    private RustType convertRustType(
            RustySchemaParser.Type_Context ctx) {
        if (ctx.typeNoBounds() != null) {
            return convertTypeNoBounds(ctx.typeNoBounds());
        }
        throw new IllegalArgumentException("Unknown type " + ctx.getText());
    }

    private RustType convertTypeNoBounds(
            RustySchemaParser.TypeNoBoundsContext ctx) {
        if (ctx.parenthesizedType() != null)
            return convertParenthesizedType(ctx.parenthesizedType());
        if (ctx.traitObjectTypeOneBound() != null)
            return convertTraitObjectOneBound(ctx.traitObjectTypeOneBound());
        if (ctx.typePath() != null)
            return convertTypePath(ctx.typePath());
        if (ctx.typeOf() != null) {
            var sv = (ProgramSV) convertExpr(ctx.typeOf().expr());
            return new TypeOf(sv);
        }
        if (ctx.tupleType() != null)
            throw new IllegalArgumentException("TODO @ DD");
        if (ctx.neverType() != null)
            throw new IllegalArgumentException("TODO @ DD");
        if (ctx.schemaVariable() != null) {
            var sv = (ProgramSV) lookupSchemaVariable(ctx.schemaVariable().getText().substring(2));
            return new SchemaRustType(new SchemaType(sv));
        }
        throw new IllegalArgumentException("Unknown type " + ctx.getText());
    }

    private RustType convertParenthesizedType(
            RustySchemaParser.ParenthesizedTypeContext ctx) {
        return convertRustType(ctx.type_());
    }

    private RustType convertTraitObjectOneBound(
            RustySchemaParser.TraitObjectTypeOneBoundContext ctx) {
        var tbCtx = ctx.traitBound();
        if (ctx.KW_DYN() == null && tbCtx.QUESTION() == null && tbCtx.forLifetimes() == null) {
            return convertTypePath(tbCtx.typePath());
        }
        throw new IllegalArgumentException("TODO @ DD");
    }

    private PrimitiveRustType convertTypePath(
            RustySchemaParser.TypePathContext ctx) {
        assert ctx.typePathSegment().size() == 1;
        var text = ctx.typePathSegment(0).pathIdentSegment().identifier().getText();
        var pt = switch (text) {
            case "bool" -> PrimitiveType.BOOL;
            case "u8" -> PrimitiveType.U8;
            case "u16" -> PrimitiveType.U16;
            case "u32" -> PrimitiveType.U32;
            case "u64" -> PrimitiveType.U64;
            case "u128" -> PrimitiveType.U128;
            case "usize" -> PrimitiveType.USIZE;
            case "i8" -> PrimitiveType.I8;
            case "i16" -> PrimitiveType.I16;
            case "i32" -> PrimitiveType.I32;
            case "i64" -> PrimitiveType.I64;
            case "i128" -> PrimitiveType.I128;
            case "isize" -> PrimitiveType.ISIZE;
            case "char" -> PrimitiveType.CHAR;
            case "str" -> PrimitiveType.STR;
            case "!" -> PrimitiveType.NEVER;
            default -> throw new IllegalArgumentException("Unknown type '" + text + "'");
        };
        return new PrimitiveRustType(pt);
    }

    private ImmutableArray<FunctionParam> convertFunctionParams(
            RustySchemaParser.FunctionParamsContext ctx) {
        if (ctx == null)
            return new ImmutableArray<>();
        List<FunctionParam> params = new LinkedList<>();
        for (var param : ctx.functionParam()) {
            params.add(convertFunctionParamPattern(param.functionParamPattern()));
        }
        return new ImmutableArray<>(params);
    }

    private FunctionParamPattern convertFunctionParamPattern(
            RustySchemaParser.FunctionParamPatternContext ctx) {
        RustType type = convertRustType(ctx.type_());
        declaredType = services.getRustInfo().getKeYRustyType(type.type());
        inDeclarationMode = !inContextFunction;
        Pattern pat = convertPattern(ctx.pattern());
        inDeclarationMode = false;
        assert declaredType != null;
        assert declaredVariable != null;
        FunctionParamPattern param = new FunctionParamPattern(pat, type, declaredType);
        variables.put(declaredVariable.name().toString(), param);
        programVariables.put(param, Objects.requireNonNull(declaredVariable));
        declaredVariable = null;
        declaredType = null;
        return param;
    }

    private PathExprSegment convertPathExprSegment(
            RustySchemaParser.PathExprSegmentContext ctx) {
        return new PathExprSegment(convertPathIdentSegment(ctx.pathIdentSegment()));
    }

    private PathIdentSegment convertPathIdentSegment(
            RustySchemaParser.PathIdentSegmentContext ctx) {
        return new PathIdentSegment(convertIdentifier(ctx.identifier()));
    }

    private Label convertLabel(RustySchemaParser.LabelContext ctx) {
        if (ctx.LIFETIME_OR_LABEL() != null)
            return getLabel(ctx.LIFETIME_OR_LABEL().getText().substring(1));
        else
            return new SchemaLabel(getProgramSV(ctx));
    }

    private ProgramSV getProgramSV(RustySchemaParser.LabelContext ctx) {
        return (ProgramSV) lookupSchemaVariable(ctx.schemaVariable().getText().substring(2));
    }

    private ClosureParam convertClosureParam(
            RustySchemaParser.ClosureParamContext ctx) {
        var pat = convertPattern(ctx.pattern());
        var ty = ctx.type_() == null ? null : convertRustType(ctx.type_());
        return new ClosureParam(pat, ty);
    }
}
