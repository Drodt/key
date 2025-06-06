/**
 * This package contains classes that cover the Java programming language.
 * The classes in the subpackages are mainly taken from the Recoder
 * framework and made immutable. They are transformed into this data
 * structure from a Recoder structure by {@link
 * de.uka.ilkd.key.java.Recoder2KeY} or {@link
 * de.uka.ilkd.key.java.SchemaRecoder2KeY}. However, in some details both
 * data structures might differ more.
 * The following explanations are adapted from the
 * documentation of the Recoder framework.
 * <DL>
 * <DT>Source and Program Elements</DT>
 * <DD>
 * A {@link de.uka.ilkd.key.java.SourceElement} is a syntactical entity and not
 * necessary a {@link de.uka.ilkd.key.java.ModelElement}, such as a
 * {@link de.uka.ilkd.key.java.Comment}.
 * <p>
 * A {@link de.uka.ilkd.key.java.ProgramElement} is a {@link de.uka.ilkd.key.java.SourceElement}
 * and a {@link de.uka.ilkd.key.java.ModelElement}. It is aware of its parent in the syntax
 * tree, while a pure {@link de.uka.ilkd.key.java.SourceElement} is not considered as a
 * member of the AST even though it is represented in the sources.
 * <p>
 * {@link de.uka.ilkd.key.java.ProgramElement}s are further
 * classified into {@link de.uka.ilkd.key.java.TerminalProgramElement}s and
 * {@link de.uka.ilkd.key.java.NonTerminalProgramElement}s. While
 * {@link de.uka.ilkd.key.java.TerminalProgramElement}
 * is just a tag class, {@link de.uka.ilkd.key.java.NonTerminalProgramElement}s know
 * their AST children (while it is possible that they do not have any).
 * A complete source file occurs as a {@link de.uka.ilkd.key.java.CompilationUnit}.
 * <p>
 * {@link de.uka.ilkd.key.java.JavaSourceElement} and
 * {@link de.uka.ilkd.key.java.JavaProgramElement} are abstract classes defining
 * standard implementations that already know their
 * {@link de.uka.ilkd.key.java.KeYJavaASTFactory}.
 * </DD>
 * <p>
 * <DT>Expressions and Statements</DT>
 * <DD>
 * {@link de.uka.ilkd.key.java.Expression} and {@link de.uka.ilkd.key.java.Statement} are
 * self-explanatory. A {@link de.uka.ilkd.key.java.LoopInitializer} is a special
 * {@link de.uka.ilkd.key.java.Statement} valid as initializer of
 * {@link de.uka.ilkd.key.java.statement.For} loops.
 * {@link de.uka.ilkd.key.java.LoopInitializer} is subtyped by
 * {@link de.uka.ilkd.key.java.expression.ExpressionStatement} and
 * {@link de.uka.ilkd.key.java.declaration.LocalVariableDeclaration}).
 * <p>
 * Concrete classes and further abstractions are bundled in the
 * {@link de.uka.ilkd.key.java.expression} and {@link de.uka.ilkd.key.java.statement} packages.
 * </DD>
 * <p>
 * <DT>Syntax Tree Parents</DT>
 * <DD>
 * There are a couple of abstractions dealing with properties of being a
 * parent node.
 * <p>
 * These are {@link de.uka.ilkd.key.java.declaration.TypeDeclarationContainer},
 * {@link de.uka.ilkd.key.java.ExpressionContainer},
 * {@link de.uka.ilkd.key.java.StatementContainer},
 * {@link de.uka.ilkd.key.java.ParameterContainer},
 * {@link de.uka.ilkd.key.java.NamedProgramElement} and
 * {@link de.uka.ilkd.key.java.reference.TypeReferenceContainer}. A
 * An {@link de.uka.ilkd.key.java.ExpressionContainer} contains
 * {@link de.uka.ilkd.key.java.Expression}s, a
 * {@link de.uka.ilkd.key.java.StatementContainer} contains
 * {@link de.uka.ilkd.key.java.Statement}s, a
 * {@link de.uka.ilkd.key.java.ParameterContainer}
 * (either a {@link de.uka.ilkd.key.java.declaration.MethodDeclaration} or a
 * {@link de.uka.ilkd.key.java.statement.Catch} statement) contains
 * {@link de.uka.ilkd.key.java.declaration.ParameterDeclaration}s.
 * A {@link de.uka.ilkd.key.java.NamedProgramElement} is a subtype of
 * {@link de.uka.ilkd.key.java.NamedModelElement}.
 * A {@link de.uka.ilkd.key.java.reference.TypeReferenceContainer} contains one or
 * several names, but these are names of types that are referred to explicitely
 * by a {@link de.uka.ilkd.key.java.reference.TypeReference}.
 * </DD>
 * <p>
 * <DT>References</DT>
 * <DD>
 * A {@link de.uka.ilkd.key.java.Reference} is an explicite use of an entity. Most of
 * these {@link de.uka.ilkd.key.java.Reference}s are
 * {@link de.uka.ilkd.key.java.reference.NameReference}s
 * and as such {@link de.uka.ilkd.key.java.NamedProgramElement}s, e.g. the
 * {@link de.uka.ilkd.key.java.reference.TypeReference}.
 * Subtypes of {@link de.uka.ilkd.key.java.Reference}s are bundled in the
 * {@link de.uka.ilkd.key.java.reference} package.
 * </DD>
 * <p>
 * <DT>Modifiers and Declarations</DT>
 * <DD>
 * {@link de.uka.ilkd.key.java.declaration.Modifier}s are (exclusively) used in the
 * context of {@link de.uka.ilkd.key.java.Declaration}s.
 * {@link de.uka.ilkd.key.java.declaration.Modifier}s occur explicitly, since they occur
 * as syntactical tokens that might be indented and commented.
 * {@link de.uka.ilkd.key.java.Declaration}s are either
 * declarations of types or other entities such as
 * {@link de.uka.ilkd.key.java.declaration.MemberDeclaration} or
 * {@link de.uka.ilkd.key.java.declaration.VariableDeclaration}. Concrete
 * {@link de.uka.ilkd.key.java.declaration.Modifier}s and
 * {@link de.uka.ilkd.key.java.Declaration}s are
 * bundled in the {@link de.uka.ilkd.key.java.declaration.modifier} and
 * {@link de.uka.ilkd.key.java.declaration} packages.
 * </DD>
 * </DL>
 */
package de.uka.ilkd.key.java;
