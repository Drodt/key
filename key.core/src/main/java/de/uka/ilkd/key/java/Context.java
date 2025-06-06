/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.java;

import de.uka.ilkd.key.java.recoderext.ImplicitIdentifier;
import de.uka.ilkd.key.java.recoderext.KeYCrossReferenceServiceConfiguration;
import de.uka.ilkd.key.util.SpecDataLocation;

import recoder.java.declaration.ClassDeclaration;
import recoder.java.declaration.TypeDeclaration;
import recoder.list.generic.ASTArrayList;
import recoder.list.generic.ASTList;

/**
 * this class stores recoder related contextinformation used to parse in program parts in which
 * non-declared variables are used
 */
class Context {

    private final recoder.java.CompilationUnit compilationUnitContext;
    private final ClassDeclaration classContext;
    public static final String PARSING_CONTEXT_CLASS_NAME = "<KeYSpecialParsing>";



    /**
     * creates a new context object
     *
     * @param compilationUnitContext a recoder.java.CompilationUnit
     * @param classContext a recoder.java.declaration.ClassDeclaration
     */
    public Context(KeYCrossReferenceServiceConfiguration servConf,
            recoder.java.CompilationUnit compilationUnitContext, ClassDeclaration classContext) {
        this.compilationUnitContext = compilationUnitContext;
        this.classContext = classContext;
    }

    /**
     * creates a new context object
     *
     * @param compilationUnitContext a recoder.java.declaration.CompilationUnit
     */
    public Context(KeYCrossReferenceServiceConfiguration servConf,
            recoder.java.CompilationUnit compilationUnitContext) {
        this(servConf, compilationUnitContext, createClassDecl(servConf));
    }


    /**
     * creates a new context object
     *
     * @param classContext a recoder.java.declaration.ClassDeclaration
     */
    public Context(KeYCrossReferenceServiceConfiguration servConf, ClassDeclaration classContext) {
        this(servConf, createCompUnit(classContext), classContext);
    }

    private static recoder.java.CompilationUnit createCompUnit(ClassDeclaration classContext) {
        recoder.java.CompilationUnit cu = new recoder.java.CompilationUnit(null,
            new ASTArrayList<>(0), inList(classContext));
        cu.setDataLocation(new SpecDataLocation("INTERNAL", classContext.getFullName()));
        return cu;
    }


    public static ASTList<TypeDeclaration> inList(TypeDeclaration td) {
        ASTList<TypeDeclaration> tdml = new ASTArrayList<>();
        tdml.add(td);
        return tdml;
    }

    /** returns the compilation context */
    public recoder.java.CompilationUnit getCompilationUnitContext() {
        return compilationUnitContext;
    }

    /** returns the compilation context */
    public ClassDeclaration getClassContext() {
        return classContext;
    }


    private static ClassDeclaration createClassDecl(
            KeYCrossReferenceServiceConfiguration servConf) {
        return servConf.getProgramFactory().createClassDeclaration(null,
            new ImplicitIdentifier(PARSING_CONTEXT_CLASS_NAME), null, null, null);
    }


}
