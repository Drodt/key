/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedHashMap;

import de.uka.ilkd.key.java.recoderext.ImplicitIdentifier;
import de.uka.ilkd.key.java.recoderext.SchemaCrossReferenceServiceConfiguration;
import de.uka.ilkd.key.java.recoderext.SchemaJavaProgramFactory;
import de.uka.ilkd.key.logic.NamespaceSet;
import de.uka.ilkd.key.util.KeYRecoderExcHandler;

import org.key_project.logic.Namespace;
import org.key_project.logic.op.sv.SchemaVariable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recoder.ParserException;
import recoder.java.declaration.ClassDeclaration;
import recoder.java.declaration.TypeDeclaration;
import recoder.list.generic.ASTArrayList;
import recoder.list.generic.ASTList;

public class SchemaRecoder2KeY extends Recoder2KeY implements SchemaJavaReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaRecoder2KeY.class);

    /** the namespace containing the program schema variables allowed here */
    protected Namespace<SchemaVariable> svns;

    /** caches access to methods for reflection */
    private final static HashMap<?, ?> schemaCt2meth = new LinkedHashMap<>(400);

    /** caches constructor access for reflection */
    private final static HashMap<?, ?> recClass2schemakeyClassCons =
        new LinkedHashMap<>(400);

    // could this be the servConf of the super class?
    private static final SchemaCrossReferenceServiceConfiguration schemaServConf =
        new SchemaCrossReferenceServiceConfiguration(new KeYRecoderExcHandler());

    public SchemaRecoder2KeY(Services services, NamespaceSet nss) {
        super(services, nss);
    }

    @Override
    protected Recoder2KeYConverter makeConverter(Services services, NamespaceSet nss) {
        return new SchemaRecoder2KeYConverter(this, services, nss);
    }

    /**
     * returns the hashmap of a concrete RecodeR class to the constructor of its corresponding KeY
     * class. Speeds up reflection. Attention must be overwritten by subclasses!
     */
    protected HashMap<?, ?> getKeYClassConstructorCache() {
        return recClass2schemakeyClassCons;
    }

    protected HashMap<?, ?> getMethodCache() {
        return schemaCt2meth;
    }

    public void setSVNamespace(Namespace<SchemaVariable> svns) {
        this.svns = svns;
    }

    /**
     * creates an empty RECODER compilation unit
     *
     * @return the recoder.java.CompilationUnit
     */
    public Context createEmptyContext() {
        return new Context(schemaServConf, new recoder.java.CompilationUnit(),
            schemaServConf.getProgramFactory().createClassDeclaration(null,
                new ImplicitIdentifier("<KeYSpecialParsing>"), null, null, null));
    }

    /**
     * wraps a RECODER ClassDeclaration in a compilation unit
     *
     * @param classDecl the recoder.java.ClassDeclaration to wrap
     * @param context the Context containing the recoder.java.CompilationUnit where the class is
     *        wrapped
     * @return the enclosing recoder.java.CompilationUnit
     */
    protected recoder.java.CompilationUnit embedClass(
            ClassDeclaration classDecl, Context context) {

        recoder.java.CompilationUnit cUnit = context.getCompilationUnitContext();

        // add class to compilation unit
        ASTList<TypeDeclaration> typeDecls = cUnit.getDeclarations();

        if (typeDecls == null) {
            typeDecls = new ASTArrayList<>(0);
        } else {
            typeDecls = typeDecls.deepClone();
        }
        typeDecls.add(classDecl);

        recoder.java.CompilationUnit compUnitContext = cUnit.deepClone();

        compUnitContext.setDeclarations(typeDecls);
        compUnitContext.makeParentRoleValid();
        schemaServConf.getChangeHistory().attached(compUnitContext);
        schemaServConf.getChangeHistory().updateModel();
        return compUnitContext;
    }

    /**
     * parses a given JavaBlock using the context to determine the right references and returns a
     * statement block of recoder.
     *
     * @param block a String describing a java block
     * @param context recoder.java.CompilationUnit in which the block has to be interpreted
     * @return the parsed and resolved recoder statement block
     */
    protected recoder.java.StatementBlock recoderBlock(String block, Context context) {
        recoder.java.StatementBlock bl = null;

        SchemaJavaProgramFactory factory =
            (SchemaJavaProgramFactory) schemaServConf.getProgramFactory();
        factory.setSVNamespace(svns);
        Reader br = null;
        try {
            br = new BufferedReader(new StringReader(block));
            try {
                bl = factory.parseStatementBlock(br);
            } finally {
                br.close();
            }
        } catch (ParserException e) {
            LOGGER.debug(
                "readSchemaJavaBlock(Reader,CompilationUnit)" + " caused the " + "exception:\n", e);
            throw new ConvertException("Parsing: \n **** BEGIN ****\n " + block
                + "\n **** END ****\n failed. Thrown Exception:" + e, e);
        } catch (IOException ioe) {
            LOGGER.debug(
                "readSchemaJavaBlock(Reader,CompilationUnit)" + " caused the IO exception:", ioe);
            throw new ConvertException("IO Error when parsing: \n **** BEGIN ****\n " + block
                + "\n **** END ****\n failed. Thrown IOException:" + ioe, ioe);
        }

        embedClass(embedMethod(embedBlock(bl), context), context);

        return bl;
    }

    /**
     * there is no need to parse special classes in this case, so this is empty
     *
     * @see Recoder2KeY#parseSpecialClasses()
     */
    public void parseSpecialClasses() {
    }
}
