package com.redhat.ceylon.compiler.js;

import com.redhat.ceylon.compiler.typechecker.tree.Tree;

public class AttributeGenerator {

    static void getter(final Tree.AttributeGetterDefinition that, final GenerateJsVisitor gen) {
        gen.beginBlock();
        gen.initSelf(that);
        gen.visitStatements(that.getBlock().getStatements());
        gen.endBlock();
    }

    static void setter(final Tree.AttributeSetterDefinition that, final GenerateJsVisitor gen) {
        if (that.getSpecifierExpression() == null) {
            gen.beginBlock();
            gen.initSelf(that);
            gen.visitStatements(that.getBlock().getStatements());
            gen.endBlock();
        } else {
            gen.out("{");
            gen.initSelf(that);
            gen.out("return ");
            if (!gen.isNaturalLiteral(that.getSpecifierExpression().getExpression().getTerm())) {
                that.getSpecifierExpression().visit(gen);
            }
            gen.out(";}");
        }
    }

}
