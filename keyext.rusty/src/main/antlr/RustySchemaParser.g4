parser grammar RustySchemaParser;

options { tokenVocab = RustySchemaLexer; }

@header {
package org.key_project.rusty.parsing;
}

function_
   : KW_FN identifier LPAREN functionParams? RPAREN functionRetTy? (blockExpr | SEMI)
   ;

functionParams
   :
   functionParam (COMMA functionParam)* COMMA?
   ;

functionParam
   : pattern ':' type_
   ;

functionRetTy
   : '->' type_
   ;

identifier
   : NON_KEYWORD_IDENTIFIER
   | RAW_IDENTIFIER
   ;

schemaVariable
   : SCHEMA_IDENTIFIER
   ;

genericArgs
   : LT GT
   | LT genericArgsLifetimes (COMMA genericArgsTypes)? (COMMA genericArgsBindings)? COMMA? GT
   | LT genericArgsTypes (COMMA genericArgsBindings)? COMMA? GT
   | LT (genericArg COMMA)* genericArg COMMA? GT
   ;

genericArg
   : lifetime
   | type_
   | genericArgsConst
   | genericArgsBinding
   ;

genericArgsConst
   : blockExpr
   | MINUS? literalExpr
   | simplePathSegment
   ;

genericArgsLifetimes
   : lifetime (COMMA lifetime)*
   ;

genericArgsTypes
   : type_ (COMMA type_)*
   ;

genericArgsBindings
   : genericArgsBinding (COMMA genericArgsBinding)*
   ;

genericArgsBinding
   : identifier EQ type_
   ;

lifetime
   : LIFETIME_OR_LABEL
   | KW_STATICLIFETIME
   | KW_UNDERLINELIFETIME
   ;

simplePath
   : PATHSEP? simplePathSegment (PATHSEP simplePathSegment)*
   ;

simplePathSegment
   : identifier
   | KW_SUPER
   | KW_SELFVALUE
   | KW_CRATE
   | KW_DOLLARCRATE
   ;

// === Expr ===

blockExpr
   : LCURLYBRACE CONTEXT_START stmts? CONTEXT_END RCURLYBRACE # ContextBlockExpr
   | LCURLYBRACE stmts? RCURLYBRACE # StandardBlockExpr
   ;

expr
   : schemaVariable # SchemaVarExpression
   | KW_CONST schemaVariable # ConstBlockSchema
   | EXPAND_FN_BODY LPAREN schemaVariable RPAREN # ExpandFnBody
   | FN_FRAME LPAREN schemaVariable COMMA blockExpr RPAREN # FnFrame
   | literalExpr # LiteralExpression
   | pathExpr # PathExpression
   | expr DOT pathExprSegment LPAREN callParams? RPAREN # MethodCallExpression
   | expr DOT (schemaVariable | identifier) # FieldExpression
   | expr DOT tupleIndex # TupleIndexingExpression
   | expr DOT KW_AWAIT # AwaitExpression
   | expr LPAREN callParams? RPAREN # CallExpression
   | expr LPAREN callParams? RPAREN AT # FunctionBodyExpression
   | expr LSQUAREBRACKET expr RSQUAREBRACKET # IndexExpression
   | (AND | ANDAND) KW_MUT? expr # BorrowExpression
   | STAR expr # DereferenceExpression
   | (MINUS | NOT) expr # NegationExpression
   | expr KW_AS type_ # TypeCastExpression
   | expr (STAR | SLASH | PERCENT) expr # ArithmeticOrLogicalExpression
   | expr (PLUS | MINUS) expr # ArithmeticOrLogicalExpression
   | expr (shl | shr) expr # ArithmeticOrLogicalExpression
   | expr AND expr # ArithmeticOrLogicalExpression
   | expr CARET expr # ArithmeticOrLogicalExpression
   | expr OR expr # ArithmeticOrLogicalExpression
   | expr comparisonOperator expr # ComparisonExpression
   | expr ANDAND expr # LazyBooleanExpression
   | expr OROR expr # LazyBooleanExpression
   | expr EQ expr # AssignmentExpression
   | expr compoundAssignOperator expr # CompoundAssignmentExpression
   | KW_CONTINUE label? # ContinueExpression
   | KW_BREAK label? expr? # BreakExpression
   | KW_RETURN expr? # ReturnExpression
   | LPAREN expr RPAREN # GroupedExpression
   | LSQUAREBRACKET arrayElements? RSQUAREBRACKET # ArrayExpression
   | LPAREN tupleElements? RPAREN # TupleExpression
   | structExpr # StructExpression_
   | enumerationVariantExpr # EnumerationVariantExpression_
   | closureExpr # ClosureExpression_
   | exprWithBlock # ExpressionWithBlock_
   | PANIC LPAREN RPAREN # EmptyPanic
   | GHOST blockExpr # GhostBlockExpression
   | SNAPSHOT LPAREN schemaVariable RPAREN # SnapshotExpression
   ;

callParams
   : expr (',' expr)* ','?
   ;

tupleIndex
   : INTEGER_LITERAL
   ;

shl
   : LT
   {_input.LA(1) == LT}? LT
   ;

shr
   : GT
   {_input.LA(1) == GT}? GT
   ;

comparisonOperator
   : '=='
   | '!='
   | '>'
   | '<'
   | '>='
   | '<='
   ;

compoundAssignOperator
   : PLUSEQ
   | MINUSEQ
   | STAREQ
   | SLASHEQ
   | PERCENTEQ
   | ANDEQ
   | OREQ
   | CARETEQ
   | SHLEQ
   | SHREQ
   ;

arrayElements
   : expr (',' expr)* ','?
   | expr ';' expr
   ;
   // 8.2.7

tupleElements
   : (expr ',')+ expr?
   ;

structExpr
   : structExprStruct
   | structExprTuple
   | structExprUnit
   ;

structExprStruct
   : pathInExpr LCURLYBRACE (structExprFields | structBase)? RCURLYBRACE
   ;

structExprFields
   : structExprField (COMMA structExprField)* (COMMA structBase | COMMA?)
   ;
   // outerAttribute here is not in doc

structExprField
   : (identifier | (identifier | tupleIndex) COLON expr)
   ;

structBase
   : DOTDOT expr
   ;

structExprTuple
   : pathInExpr LPAREN (expr (COMMA expr)* COMMA?)? RPAREN
   ;

structExprUnit
   : pathInExpr
   ;

enumerationVariantExpr
   : enumExprStruct
   | enumExprTuple
   | enumExprFieldless
   ;

enumExprStruct
   : pathInExpr LCURLYBRACE enumExprFields? RCURLYBRACE
   ;

enumExprFields
   : enumExprField (COMMA enumExprField)* COMMA?
   ;

enumExprField
   : identifier
   | (identifier | tupleIndex) COLON expr
   ;

enumExprTuple
   : pathInExpr LPAREN (expr (COMMA expr)* COMMA?)? RPAREN
   ;

enumExprFieldless
   : pathInExpr
   ;

closureExpr
   : KW_MOVE? (OROR | OR closureParameters? OR) (expr | RARROW type_ blockExpr)
   ;

closureParameters
   : closureParam (COMMA closureParam)* COMMA?
   ;

closureParam
   : pattern (COLON type_)?
   ;

exprWithBlock
   : blockExpr
   | loopExpr
   | ifExpr
   | ifLetExpr
   | matchExpr
   ;

ifExpr
   : KW_IF expr (thenBlock=blockExpr | thenSV=schemaVariable) (KW_ELSE (elseBlock=blockExpr | elseIf=ifExpr | elseSV=schemaVariable))?
   ;

ifLetExpr
  : KW_IF KW_LET (pattern | patternSV=schemaVariable) EQ expr (thenBlock=blockExpr | thenSV=schemaVariable) (KW_ELSE (elseBlock=blockExpr | elseIf=ifExpr | elseIfLet=ifLetExpr | elseSV=schemaVariable))?
  ;

matchExpr
   : KW_MATCH expr LCURLYBRACE matchArms? RCURLYBRACE
   ;

matchArms
   : (matchArm FATARROW matchArmExpression)* matchArm FATARROW expr COMMA?
   ;

matchArmExpression
   : expr COMMA
   | exprWithBlock COMMA?
   ;

matchArm
   : pattern matchArmGuard?
   ;

matchArmGuard
   : KW_IF expr
   ;

literalExpr
   : CHAR_LITERAL
   | STRING_LITERAL
   | RAW_STRING_LITERAL
   | BYTE_LITERAL
   | BYTE_STRING_LITERAL
   | RAW_BYTE_STRING_LITERAL
   | INTEGER_LITERAL
   // | FLOAT_LITERAL
   | KW_TRUE
   | KW_FALSE
   ;

pathExpr
   : pathInExpr
   | qualifiedPathInExpr
   ;

pathInExpr
   : PATHSEP? pathExprSegment (PATHSEP pathExprSegment)*
   ;

pathExprSegment
   : pathIdentSegment (PATHSEP genericArgs)?
   ;

pathIdentSegment
   : identifier
   | KW_SUPER
   | KW_SELFVALUE
   | KW_SELFTYPE
   | KW_CRATE
   | KW_DOLLARCRATE
   ;
   //TODO: let x : T<_>=something;

qualifiedPathInExpr
   : qualifiedPathType (PATHSEP pathExprSegment)+
   ;

qualifiedPathType
   : LT type_ (KW_AS typePath)? GT
   ;

loopExpr
   : loopLabel? (infiniteLoopExpr
    | loopScope)
   ;

infiniteLoopExpr
   : 'loop' (block=blockExpr | sv=schemaVariable)
   ;

loopScope
    : 'loop_scope!' '(' idx=schemaVariable ',' ret=schemaVariable ',' blockExpr ')';

loopLabel
   : label COLON
   ;

label
    : LIFETIME_OR_LABEL | schemaVariable
    ;

// === Stmt ===

stmts
   : stmt+ expr?
   | expr
   ;

stmt
   : letStmt
   | exprStmt
   | schemaStmt SEMI
   | PANIC_FRAME LPAREN schemaVariable COMMA blockExpr RPAREN
   ;

letStmt
   : 'let' patternNoTopAlt (':' type_)? ('=' expr)? ';'
   ;

schemaStmt
   : schemaVariable
   ;

exprStmt
   : expr ';'
   | exprWithBlock ';'?
   ;

// === Type ===

type_
   : parenthesizedType
//   | implTraitTypeOneBound
//   | traitObjectTypeOneBound
   | typePath
   | tupleType
   | neverType
   | rawPointerType
   | referenceType
   | arrayType
   | sliceType
   | inferredType
   | qualifiedPathInType
   | bareFunctionType
   | typeOf
   | schemaVariable
   ;

parenthesizedType
   : LPAREN type_ RPAREN
   ;

typePath
   : PATHSEP? typePathSegment (PATHSEP typePathSegment)*
   ;

typePathSegment
   : pathIdentSegment PATHSEP? (genericArgs | typePathFn)?
   ;

typePathFn
   : LPAREN typePathInputs? RPAREN (RARROW type_)?
   ;

typePathInputs
   : type_ (COMMA type_)* COMMA?
   ;

neverType
   : '!'
   ;
   // 10.1.5

tupleType
   : '(' ((type_ ',')+ type_?)? ')'
   ;
   // 10.1.6

arrayType
   : '[' type_ ';' expr ']'
   ;

sliceType
   : LSQUAREBRACKET type_ RSQUAREBRACKET
   ;

referenceType
   : AND lifetime? KW_MUT? type_
   ;

rawPointerType
   : STAR (KW_MUT | KW_CONST) type_
   ;

qualifiedPathInType
   : qualifiedPathType (PATHSEP typePathSegment)+
   ;

bareFunctionType
   : KW_FN LPAREN functionParametersMaybeNamedVariadic? RPAREN bareFunctionReturnType?
   ;

functionParametersMaybeNamedVariadic
   : maybeNamedFunctionParameters
   | maybeNamedFunctionParametersVariadic
   ;

maybeNamedFunctionParameters
   : maybeNamedParam (COMMA maybeNamedParam)* COMMA?
   ;

maybeNamedParam
   : ((identifier | UNDERSCORE) COLON)? type_
   ;

maybeNamedFunctionParametersVariadic
   : (maybeNamedParam COMMA)* maybeNamedParam COMMA DOTDOTDOT
   ;

bareFunctionReturnType
   : RARROW type_
   ;

inferredType
   : UNDERSCORE
   ;

typeOf
   : TYPE_OF LPAREN expr RPAREN
   ;

// === Pattern ===

pattern
   : OR? patternNoTopAlt (OR patternNoTopAlt)*
   ;

patternNoTopAlt
   : patternWithoutRange
   | rangePattern
   ;

patternWithoutRange
   : literalPattern
   | identifierPattern
   | wildcardPattern
   | restPattern
   | referencePattern
   | structPattern
   | tupleStructPattern
   | tuplePattern
   | groupedPattern
   | slicePattern
   | pathPattern
   ;

literalPattern
   : KW_TRUE
   | KW_FALSE
   | CHAR_LITERAL
   | BYTE_LITERAL
   | STRING_LITERAL
   | RAW_STRING_LITERAL
   | BYTE_STRING_LITERAL
   | RAW_BYTE_STRING_LITERAL
   | MINUS? INTEGER_LITERAL
   // | MINUS? FLOAT_LITERAL
   ;

identifierPattern
   : KW_REF? KW_MUT? (identifier | schemaVariable) (AT pattern)?
   ;

wildcardPattern
   : UNDERSCORE
   ;

restPattern
   : DOTDOT
   ;

rangePattern
   : rangeExclusivePattern
   | rangeInclusivePattern
   | rangeFromPattern
   | rangeToInclusivePattern
   | obsoleteRangePattern
   ;

rangeExclusivePattern
   : rangePatternBound DOTDOT rangePatternBound
   ;

rangeInclusivePattern
   : rangePatternBound DOTDOTEQ rangePatternBound
   ;

rangeFromPattern
   : rangePatternBound DOTDOT
   ;

rangeToInclusivePattern
   : DOTDOTEQ rangePatternBound
   ;

obsoleteRangePattern
   : rangePatternBound DOTDOTDOT rangePatternBound
   ;

rangePatternBound
   : schemaVariable
   | CHAR_LITERAL
   | BYTE_LITERAL
   | MINUS? INTEGER_LITERAL
   // | MINUS? FLOAT_LITERAL
   | pathExpr
   ;

referencePattern
   : (AND | ANDAND) KW_MUT? patternWithoutRange
   ;

structPattern
   : pathInExpr LCURLYBRACE structPatternElements? RCURLYBRACE
   ;

structPatternElements
   : structPatternFields (COMMA structPatternEtCetera?)?
   | structPatternEtCetera
   ;

structPatternFields
   : structPatternField (COMMA structPatternField)*
   ;

structPatternField
   : tupleIndex COLON pattern
   | identifier COLON pattern
   | KW_REF? KW_MUT? identifier
   ;

structPatternEtCetera
   : DOTDOT
   ;

tupleStructPattern
   : pathInExpr LPAREN tupleStructItems? RPAREN
   ;

tupleStructItems
   : pattern (COMMA pattern)* COMMA?
   ;

tuplePattern
   : LPAREN tuplePatternItems? RPAREN
   ;

tuplePatternItems
   : pattern COMMA
   | restPattern
   | pattern (COMMA pattern)+ COMMA?
   ;

groupedPattern
   : LPAREN pattern RPAREN
   ;

slicePattern
   : LSQUAREBRACKET slicePatternItems? RSQUAREBRACKET
   ;

slicePatternItems
   : pattern (COMMA pattern)* COMMA?
   ;

pathPattern
   : pathInExpr
   | qualifiedPathInExpr
   ;