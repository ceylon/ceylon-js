package com.redhat.ceylon.compiler.js;

import java.util.Map;

import com.redhat.ceylon.compiler.loader.MetamodelGenerator;
import com.redhat.ceylon.compiler.typechecker.model.Module;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Visitor;

/** Generates the metamodel for all objects in a module.
 * 
 * @author Enrique Zamudio
 */
public class MetamodelVisitor extends Visitor {

    private final MetamodelGenerator gen;

    public MetamodelVisitor(Module module) {
        this.gen = new MetamodelGenerator(module);
    }

    /** Returns the in-memory model as a collection of maps.
     * The top-level map represents the module. */
    public Map<String, Object> getModel() {
        return gen.getModel();
    }

    @Override public void visit(Tree.MethodDeclaration that) {
        gen.encodeMethod(that.getDeclarationModel());
        super.visit(that);
    }

    /** Create and store the model of a method definition. */
    @Override public void visit(Tree.MethodDefinition that) {
        gen.encodeMethod(that.getDeclarationModel());
        super.visit(that);
    }

    /** Create and store the metamodel info for an attribute. */
    @Override public void visit(Tree.AttributeDeclaration that) {
        gen.encodeAttribute(that.getDeclarationModel());
        super.visit(that);
    }

    @Override
    public void visit(Tree.ClassDefinition that) {
        gen.encodeClass(that.getDeclarationModel());
        super.visit(that);
    }

    @Override
    public void visit(Tree.InterfaceDefinition that) {
        gen.encodeInterface(that.getDeclarationModel());
        super.visit(that);
    }

    @Override
    public void visit(Tree.ObjectDefinition that) {
        gen.encodeObject(that.getDeclarationModel());
        super.visit(that);
    }

    @Override
    public void visit(Tree.AttributeGetterDefinition that) {
        gen.encodeGetter(that.getDeclarationModel());
        super.visit(that);
    }

    @Override
    public void visit(Tree.TypeAliasDeclaration that) {
        gen.encodeTypeAlias(that.getDeclarationModel());
    }
    @Override
    public void visit(Tree.ClassDeclaration that) {
        gen.encodeClass(that.getDeclarationModel());
    }
    @Override
    public void visit(Tree.InterfaceDeclaration that) {
        gen.encodeInterface(that.getDeclarationModel());
    }

}
