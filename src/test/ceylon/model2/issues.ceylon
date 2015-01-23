import ceylon.language.meta.declaration {
  FunctionDeclaration, ClassOrInterfaceDeclaration, Package, ValueDeclaration
}
import check { check, fail }

shared class Fuera() {
  shared class Dentro() {
    shared void f() {}
    shared class Cebolla() {
      shared void g() {}
    }
  }
  shared interface Vacia {}
}

shared annotation DumbAnnotation dumb() => DumbAnnotation();

shared final annotation class DumbAnnotation()
        satisfies OptionalAnnotation<DumbAnnotation,FunctionDeclaration> {}

shared class Padre() {
  shared void a() {}
  shared dumb void b() {}
}
shared class Hijo() extends Padre() {
  shared void c() {}
  shared dumb void d() {}
}

void issues() {
    try {
        value g1 = `function Fuera.Dentro.Cebolla.g`;
        value g2 = `Fuera.Dentro.Cebolla.g`;
        check(g1.name == "g", "Member method of member type declaration");
        check(g2.declaration.name == "g", "Member method of member type");
        value methods = `class Hijo`.annotatedMemberDeclarations<FunctionDeclaration,DumbAnnotation>();
        value declMethods = `class Hijo`.annotatedDeclaredMemberDeclarations<FunctionDeclaration,DumbAnnotation>();
        check(methods.size==2, "annotatedMemberDeclarations expected 2, got ``methods.size``: ``methods``");
        check(declMethods.size==1, "annotatedMemberDeclarations expected 2, got ``declMethods.size``: ``declMethods``");
        value types = `class Fuera`.memberDeclarations<ClassOrInterfaceDeclaration>();
        check(types.size==2, "member types expected 2, got ``types.size``: ``types``");
    } catch (Throwable e) {
        if ("Cannot read property '$$' of undefined" in e.message) {
            fail("Member declaration tests won't work in lexical scope style");
        } else {
            fail("Something bad: ``e.message``");
            e.printStackTrace();
        }
    }
    value module489=`module nesting`;
    check(module489.string=="module nesting/0.1", "#489.1 module is ' ``module489.string``'");
    value packs489=module489.members;
    check(packs489.size==2, "#489.2");
    void checkPackage(Package p) {
        value funs=p.members<FunctionDeclaration>();
        check(funs.size>=2, "#489.5 expected 2 functions found ``funs``");
        value vals=p.members<ValueDeclaration>();
        check(vals.size>=4, "#489.6 expected 4 values found ``vals``");
        value typs=p.members<ClassOrInterfaceDeclaration>();
        check(typs.size>=2, "#489.7 expected 2 classes found ``typs``");
    }
    if (exists rootp489=packs489.find((p)=>p.name=="nesting")) {
        checkPackage(rootp489);
    } else {
        fail("#489.3 package 'nesting' not found");
    }
    if (exists subp489=packs489.find((p)=>p.name=="nesting.sub")) {
        checkPackage(subp489);
    } else {
        fail("#489.4 package 'nesting.sub' not found");
    }
}
