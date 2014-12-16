package com.redhat.ceylon.compiler.js;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.redhat.ceylon.compiler.loader.MetamodelGenerator;
import com.redhat.ceylon.compiler.typechecker.model.Annotation;
import com.redhat.ceylon.compiler.typechecker.model.ClassOrInterface;
import com.redhat.ceylon.compiler.typechecker.model.Constructor;
import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.model.Generic;
import com.redhat.ceylon.compiler.typechecker.model.IntersectionType;
import com.redhat.ceylon.compiler.typechecker.model.Method;
import com.redhat.ceylon.compiler.typechecker.model.MethodOrValue;
import com.redhat.ceylon.compiler.typechecker.model.Module;
import com.redhat.ceylon.compiler.typechecker.model.Parameter;
import com.redhat.ceylon.compiler.typechecker.model.ParameterList;
import com.redhat.ceylon.compiler.typechecker.model.ProducedType;
import com.redhat.ceylon.compiler.typechecker.model.Scope;
import com.redhat.ceylon.compiler.typechecker.model.Setter;
import com.redhat.ceylon.compiler.typechecker.model.SiteVariance;
import com.redhat.ceylon.compiler.typechecker.model.TypeAlias;
import com.redhat.ceylon.compiler.typechecker.model.TypeDeclaration;
import com.redhat.ceylon.compiler.typechecker.model.TypedDeclaration;
import com.redhat.ceylon.compiler.typechecker.model.TypeParameter;
import com.redhat.ceylon.compiler.typechecker.model.UnionType;
import com.redhat.ceylon.compiler.typechecker.model.Util;
import com.redhat.ceylon.compiler.typechecker.model.Value;
import com.redhat.ceylon.compiler.typechecker.tree.Node;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;

/** A convenience class to help with the handling of certain type declarations. */
public class TypeUtils {

    static final List<String> splitMetamodelAnnotations = Arrays.asList("ceylon.language::doc",
            "ceylon.language::throws", "ceylon.language::see", "ceylon.language::by");

    /** Prints the type arguments, usually for their reification. */
    public static void printTypeArguments(final Node node, final Map<TypeParameter,ProducedType> targs,
            final GenerateJsVisitor gen, final boolean skipSelfDecl, final Map<TypeParameter, SiteVariance> overrides) {
        gen.out("{");
        boolean first = true;
        for (Map.Entry<TypeParameter,ProducedType> e : targs.entrySet()) {
            if (first) {
                first = false;
            } else {
                gen.out(",");
            }
            gen.out(e.getKey().getName(), "$", e.getKey().getDeclaration().getName(), ":");
            final ProducedType pt = e.getValue();
            if (pt == null) {
                gen.out("'", e.getKey().getName(), "'");
            } else if (!outputTypeList(node, pt, gen, skipSelfDecl)) {
                boolean hasParams = pt.getTypeArgumentList() != null && !pt.getTypeArgumentList().isEmpty();
                boolean closeBracket = false;
                final TypeDeclaration d = pt.getDeclaration();
                if (d instanceof TypeParameter) {
                    resolveTypeParameter(node, (TypeParameter)d, gen, skipSelfDecl);
                } else {
                    closeBracket = pt.getDeclaration() instanceof TypeAlias==false;
                    if (closeBracket)gen.out("{t:");
                    outputQualifiedTypename(node,
                            node != null && gen.isImported(node.getUnit().getPackage(), pt.getDeclaration()),
                            pt, gen, skipSelfDecl);
                }
                if (hasParams) {
                    gen.out(",a:");
                    printTypeArguments(node, pt.getTypeArguments(), gen, skipSelfDecl, pt.getVarianceOverrides());
                }
                SiteVariance siteVariance = overrides == null ? null : overrides.get(e.getKey());
                if (siteVariance != null) {
                    gen.out(",", MetamodelGenerator.KEY_US_VARIANCE, ":");
                    if (siteVariance == SiteVariance.IN) {
                        gen.out("'in'");
                    } else {
                        gen.out("'out'");
                    }
                }
                if (closeBracket) {
                    gen.out("}");
                }
            }
        }
        gen.out("}");
    }

    static void outputQualifiedTypename(final Node node, final boolean imported, final ProducedType pt,
            final GenerateJsVisitor gen, final boolean skipSelfDecl) {
        TypeDeclaration t = pt.getDeclaration();
        final String qname = t.getQualifiedNameString();
        if (qname.equals("ceylon.language::Nothing")) {
            //Hack in the model means hack here as well
            gen.out(gen.getClAlias(), "Nothing");
        } else if (qname.equals("ceylon.language::null") || qname.equals("ceylon.language::Null")) {
            gen.out(gen.getClAlias(), "Null");
        } else if (pt.isUnknown()) {
            if (!gen.isInDynamicBlock()) {
                gen.out("/*WARNING unknown type");
                gen.location(node);
                gen.out("*/");
            }
            gen.out(gen.getClAlias(), "Anything");
        } else {
            gen.out(qualifiedTypeContainer(node, imported, t, gen));
            boolean _init = !imported && pt.getDeclaration().isDynamic();
            if (_init && !pt.getDeclaration().isToplevel()) {
                Declaration dynintc = Util.getContainingClassOrInterface(node.getScope());
                if (dynintc == null || dynintc instanceof Scope==false ||
                        !Util.contains((Scope)dynintc, pt.getDeclaration())) {
                    _init=false;
                }
            }
            if (_init) {
                gen.out("$init$");
            }

            if (!outputTypeList(null, pt, gen, skipSelfDecl)) {
                gen.out(gen.getNames().name(t));
            }
            if (_init) {
                gen.out("()");
            }
        }
    }

    static String qualifiedTypeContainer(final Node node, final boolean imported, final TypeDeclaration t,
            final GenerateJsVisitor gen) {
        final String modAlias = imported ? gen.getNames().moduleAlias(t.getUnit().getPackage().getModule()) : null;
        final StringBuilder sb = new StringBuilder();
        if (modAlias != null && !modAlias.isEmpty()) {
            sb.append(modAlias).append('.');
        }
        if (t.getContainer() instanceof ClassOrInterface) {
            final Scope scope = node == null ? null : Util.getContainingClassOrInterface(node.getScope());
            List<ClassOrInterface> parents = new ArrayList<>();
            ClassOrInterface parent = (ClassOrInterface)t.getContainer();
            parents.add(0, parent);
            while (parent.getContainer() instanceof ClassOrInterface) {
                parent = (ClassOrInterface)parent.getContainer();
                parents.add(0, parent);
            }
            for (ClassOrInterface p : parents) {
                if (p==scope) {
                    if (gen.opts.isOptimize()) {
                        sb.append(gen.getNames().self(p)).append('.');
                    }
                } else {
                    sb.append(gen.getNames().name(p)).append('.');
                }
            }
        }
        return sb.toString();
    }

    /** Prints out an object with a type constructor under the property "t" and its type arguments under
     * the property "a", or a union/intersection type with "u" or "i" under property "t" and the list
     * of types that compose it in an array under the property "l", or a type parameter as a reference to
     * already existing params. */
    static void typeNameOrList(final Node node, final ProducedType pt, final GenerateJsVisitor gen, final boolean skipSelfDecl) {
        TypeDeclaration type = pt.getDeclaration();
        if (!outputTypeList(node, pt, gen, skipSelfDecl)) {
            if (type instanceof TypeParameter) {
                resolveTypeParameter(node, (TypeParameter)type, gen, skipSelfDecl);
            } else if (type instanceof TypeAlias) {
                outputQualifiedTypename(node, node != null && gen.isImported(node.getUnit().getPackage(), type),
                        pt, gen, skipSelfDecl);
            } else {
                gen.out("{t:");
                outputQualifiedTypename(node, node != null && gen.isImported(node.getUnit().getPackage(), type),
                        pt, gen, skipSelfDecl);
                if (!pt.getTypeArgumentList().isEmpty()) {
                    final Map<TypeParameter,ProducedType> targs;
                    if (pt.getDeclaration().isToplevel()) {
                        targs = pt.getTypeArguments();
                    } else {
                        //Gather all type parameters from containers
                        Scope scope = node.getScope();
                        final HashSet<TypeParameter> parenttp = new HashSet<>();
                        while (scope != null) {
                            if (scope instanceof Generic) {
                                for (TypeParameter tp : ((Generic)scope).getTypeParameters()) {
                                    parenttp.add(tp);
                                }
                            }
                            scope = scope.getScope();
                        }
                        targs = new HashMap<>();
                        targs.putAll(pt.getTypeArguments());
                        Declaration cd = Util.getContainingDeclaration(pt.getDeclaration());
                        while (cd != null) {
                            if (cd instanceof Generic) {
                                for (TypeParameter tp : ((Generic)cd).getTypeParameters()) {
                                    if (parenttp.contains(tp)) {
                                        targs.put(tp, tp.getType());
                                    }
                                }
                            }
                            cd = Util.getContainingDeclaration(cd);
                        }
                    }
                    gen.out(",a:");
                    printTypeArguments(node, targs, gen, skipSelfDecl, pt.getVarianceOverrides());
                }
                gen.out("}");
            }
        }
    }

    /** Appends an object with the type's type and list of union/intersection types. */
    static boolean outputTypeList(final Node node, final ProducedType pt, final GenerateJsVisitor gen, final boolean skipSelfDecl) {
        TypeDeclaration d = pt.getDeclaration();
        final List<ProducedType> subs;
        if (d instanceof IntersectionType) {
            gen.out("{t:'i");
            subs = d.getSatisfiedTypes();
        } else if (d instanceof UnionType) {
            gen.out("{t:'u");
            subs = d.getCaseTypes();
        } else if ("ceylon.language::Tuple".equals(d.getQualifiedNameString())) {
            subs = d.getUnit().getTupleElementTypes(pt);
            final ProducedType lastType = subs.get(subs.size()-1);
            if (lastType.isUnknown()) {
                //Revert to outputting normal Tuple with its type arguments
                gen.out("{t:", gen.getClAlias(), "Tuple,a:");
                printTypeArguments(node, pt.getTypeArguments(), gen, skipSelfDecl, pt.getVarianceOverrides());
                gen.out("}");
                return true;
            }
            if (d.getUnit().getSequenceDeclaration().equals(lastType.getDeclaration())
                    || d.getUnit().getSequentialDeclaration().equals(lastType.getDeclaration())) {
                //Non-empty, non-tuple tail; union it with its type parameter
                UnionType utail = new UnionType(d.getUnit());
                utail.setCaseTypes(Arrays.asList(lastType.getTypeArgumentList().get(0), lastType));
                subs.remove(subs.size()-1);
                subs.add(utail.getType());
            }
            gen.out("{t:'T");
        } else {
            return false;
        }
        gen.out("',l:[");
        boolean first = true;
        for (ProducedType t : subs) {
            if (!first) gen.out(",");
            typeNameOrList(node, t, gen, skipSelfDecl);
            first = false;
        }
        gen.out("]}");
        return true;
    }

    /** Finds the owner of the type parameter and outputs a reference to the corresponding type argument. */
    static void resolveTypeParameter(final Node node, final TypeParameter tp,
            final GenerateJsVisitor gen, final boolean skipSelfDecl) {
        Scope parent = Util.getRealScope(node.getScope());
        int outers = 0;
        while (parent != null && parent != tp.getContainer()) {
            if (parent instanceof TypeDeclaration &&
                    !(parent instanceof Constructor || ((TypeDeclaration) parent).isAnonymous())) {
                outers++;
            }
            parent = parent.getScope();
        }
        if (tp.getContainer() instanceof ClassOrInterface) {
            if (parent == tp.getContainer()) {
                if (!skipSelfDecl) {
                    TypeDeclaration ontoy = Util.getContainingClassOrInterface(node.getScope());
                    while (ontoy.isAnonymous())ontoy=Util.getContainingClassOrInterface(ontoy.getScope());
                    gen.out(gen.getNames().self(ontoy));
                    for (int i = 0; i < outers; i++) {
                        gen.out(".outer$");
                    }
                    gen.out(".");
                }
                gen.out("$$targs$$.", tp.getName(), "$", tp.getDeclaration().getName());
            } else {
                //This can happen in expressions such as Singleton(n) when n is dynamic
                gen.out("{/*NO PARENT*/t:", gen.getClAlias(), "Anything}");
            }
        } else if (tp.getContainer() instanceof TypeAlias) {
            if (parent == tp.getContainer()) {
                gen.out("'", tp.getName(), "$", tp.getDeclaration().getName(), "'");
            } else {
                //This can happen in expressions such as Singleton(n) when n is dynamic
                gen.out("{/*NO PARENT ALIAS*/t:", gen.getClAlias(), "Anything}");
            }
        } else {
            //it has to be a method, right?
            //We need to find the index of the parameter where the argument occurs
            //...and it could be null...
            int plistCount = -1;
            ProducedType type = null;
            for (Iterator<ParameterList> iter0 = ((Method)tp.getContainer()).getParameterLists().iterator();
                    type == null && iter0.hasNext();) {
                plistCount++;
                for (Iterator<Parameter> iter1 = iter0.next().getParameters().iterator();
                        type == null && iter1.hasNext();) {
                    if (type == null) {
                        type = typeContainsTypeParameter(iter1.next().getType(), tp);
                    }
                }
            }
            //The ProducedType that we find corresponds to a parameter, whose type can be:
            //A type parameter in the method, in which case we just use the argument's type (may be null)
            //A component of a union/intersection type, in which case we just use the argument's type (may be null)
            //A type argument of the argument's type, in which case we must get the reified generic from the argument
            if (tp.getContainer() == parent) {
                gen.out("$$$mptypes.", tp.getName(), "$", tp.getDeclaration().getName());
            } else {
                if (parent == null && node instanceof Tree.StaticMemberOrTypeExpression) {
                    if (tp.getContainer() == ((Tree.StaticMemberOrTypeExpression)node).getDeclaration()) {
                        type = ((Tree.StaticMemberOrTypeExpression)node).getTarget().getTypeArguments().get(tp);
                        typeNameOrList(node, type, gen, skipSelfDecl);
                        return;
                    }
                }
                gen.out("/*METHOD TYPEPARM plist ", Integer.toString(plistCount), "#",
                        tp.getName(), "*/'", type.getProducedTypeQualifiedName(), "' container " + tp.getContainer() + " y yo estoy en " + node);
            }
        }
    }

    static ProducedType typeContainsTypeParameter(ProducedType td, TypeParameter tp) {
        TypeDeclaration d = td.getDeclaration();
        if (d == tp) {
            return td;
        } else if (d instanceof UnionType || d instanceof IntersectionType) {
            List<ProducedType> comps = td.getCaseTypes();
            if (comps == null) comps = td.getSupertypes();
            for (ProducedType sub : comps) {
                td = typeContainsTypeParameter(sub, tp);
                if (td != null) {
                    return td;
                }
            }
        } else if (d instanceof ClassOrInterface) {
            for (ProducedType sub : td.getTypeArgumentList()) {
                if (typeContainsTypeParameter(sub, tp) != null) {
                    return td;
                }
            }
        }
        return null;
    }

    /** Find the type with the specified declaration among the specified type's supertypes, case types, satisfied types, etc. */
    static ProducedType findSupertype(TypeDeclaration d, ProducedType pt) {
        if (pt.getDeclaration().equals(d)) {
            return pt;
        }
        List<ProducedType> list = pt.getSupertypes() == null ? pt.getCaseTypes() : pt.getSupertypes();
        for (ProducedType t : list) {
            if (t.getDeclaration().equals(d)) {
                return t;
            }
        }
        return null;
    }

    static Map<TypeParameter, ProducedType> matchTypeParametersWithArguments(List<TypeParameter> params, List<ProducedType> targs) {
        if (params != null && targs != null && params.size() == targs.size()) {
            HashMap<TypeParameter, ProducedType> r = new HashMap<TypeParameter, ProducedType>();
            for (int i = 0; i < targs.size(); i++) {
                r.put(params.get(i), targs.get(i));
            }
            return r;
        }
        return null;
    }

    static Map<TypeParameter, ProducedType> wrapAsIterableArguments(ProducedType pt) {
        HashMap<TypeParameter, ProducedType> r = new HashMap<TypeParameter, ProducedType>();
        final TypeDeclaration iterable = pt.getDeclaration().getUnit().getIterableDeclaration();
        r.put(iterable.getTypeParameters().get(0), pt);
        r.put(iterable.getTypeParameters().get(1), pt.getDeclaration().getUnit().getNullDeclaration().getType());
        return r;
    }

    static boolean isUnknown(Declaration d) {
        return d == null || d.getQualifiedNameString().equals("UnknownType");
    }

    static void spreadArrayCheck(final Tree.Term term, final GenerateJsVisitor gen) {
        String tmp = gen.getNames().createTempVariable();
        gen.out("(", tmp, "=");
        term.visit(gen);
        gen.out(",Array.isArray(", tmp, ")?", tmp);
        gen.out(":function(){throw new TypeError('Expected JS Array (",
                term.getUnit().getFilename(), " ", term.getLocation(), ")')}())");
    }

    /** Generates the code to throw an Exception if a dynamic object is not of the specified type. */
    static void generateDynamicCheck(final Tree.Term term, ProducedType t,
            final GenerateJsVisitor gen, final boolean skipSelfDecl,
            final Map<TypeParameter,ProducedType> typeArguments) {
        if (t.getDeclaration().isDynamic()) {
            gen.out(gen.getClAlias(), "dre$$(");
            term.visit(gen);
            gen.out(",");
            TypeUtils.typeNameOrList(term, t, gen, skipSelfDecl);
            gen.out(",'", term.getUnit().getFilename(), " ", term.getLocation(), "')");
        } else {
            final boolean checkFloat = term.getUnit().getFloatDeclaration().equals(t.getDeclaration());
            final boolean checkInt = checkFloat ? false : term.getUnit().getIntegerDeclaration().equals(t.getDeclaration());
            String tmp = gen.getNames().createTempVariable();
            gen.out("(", tmp, "=");
            term.visit(gen);
            final String errmsg;
            if (checkFloat) {
                gen.out(",typeof(", tmp, ")==='number'?", gen.getClAlias(), "Float(", tmp, ")");
                errmsg = "Expected Float";
            } else if (checkInt) {
                gen.out(",typeof(", tmp, ")==='number'?Math.floor(", tmp, ")");
                errmsg = "Expected Integer";
            } else {
                gen.out(",", gen.getClAlias(), "is$(", tmp, ",");
                if (t.getDeclaration() instanceof TypeParameter && typeArguments != null
                        && typeArguments.containsKey(t.getDeclaration())) {
                    t = typeArguments.get(t.getDeclaration());
                }
                TypeUtils.typeNameOrList(term, t, gen, skipSelfDecl);
                gen.out(")?", tmp);
                errmsg = "Expected " + t.getProducedTypeQualifiedName();
            }
            gen.out(":function(){throw new TypeError('", errmsg, " (",
                    term.getUnit().getFilename(), " ", term.getLocation(), ")')}())");
        }
    }

    static void encodeParameterListForRuntime(Node n, ParameterList plist, GenerateJsVisitor gen) {
        boolean first = true;
        gen.out("[");
        for (Parameter p : plist.getParameters()) {
            if (first) first=false; else gen.out(",");
            gen.out("{", MetamodelGenerator.KEY_NAME, ":'", p.getName(), "',");
            gen.out(MetamodelGenerator.KEY_METATYPE, ":'", MetamodelGenerator.METATYPE_PARAMETER, "',");
            if (p.getModel() instanceof Method) {
                gen.out("$pt:'f',");
            }
            if (p.isSequenced()) {
                gen.out("seq:1,");
            }
            if (p.isDefaulted()) {
                gen.out(MetamodelGenerator.KEY_DEFAULT, ":1,");
            }
            gen.out(MetamodelGenerator.KEY_TYPE, ":");
            metamodelTypeNameOrList(n, gen.getCurrentPackage(), p.getType(), gen);
            if (p.getModel().getAnnotations() != null && !p.getModel().getAnnotations().isEmpty()) {
                new ModelAnnotationGenerator(gen, p.getModel(), n).generateAnnotations();
            }
            gen.out("}");
        }
        gen.out("]");
    }

    /** Turns a Tuple type into a parameter list. */
    static List<Parameter> convertTupleToParameters(ProducedType _tuple) {
        ArrayList<Parameter> rval = new ArrayList<>();
        int pos = 0;
        TypeDeclaration tdecl = _tuple.getDeclaration();
        final TypeDeclaration empty = tdecl.getUnit().getEmptyDeclaration();
        final TypeDeclaration tuple = tdecl.getUnit().getTupleDeclaration();
        while (!(empty.equals(tdecl) || tdecl instanceof TypeParameter)) {
            Parameter _p = null;
            if (tuple.equals(tdecl) || (tdecl.getCaseTypeDeclarations() != null
                    && tdecl.getCaseTypeDeclarations().size()==2
                    && tdecl.getCaseTypeDeclarations().contains(tuple))) {
                _p = new Parameter();
                _p.setModel(new Value());
                if (tuple.equals(tdecl)) {
                    _p.getModel().setType(_tuple.getTypeArgumentList().get(1));
                    _tuple = _tuple.getTypeArgumentList().get(2);
                } else {
                    //Handle union types for defaulted parameters
                    for (ProducedType mt : _tuple.getCaseTypes()) {
                        if (tuple.equals(mt.getDeclaration())) {
                            _p.getModel().setType(mt.getTypeArgumentList().get(1));
                            _tuple = mt.getTypeArgumentList().get(2);
                            break;
                        }
                    }
                    _p.setDefaulted(true);
                }
            } else if (tdecl.inherits(tdecl.getUnit().getSequentialDeclaration())) {
                //Handle Sequence, for nonempty variadic parameters
                _p = new Parameter();
                _p.setModel(new Value());
                _p.getModel().setType(_tuple.getTypeArgumentList().get(0));
                _p.setSequenced(true);
                _tuple = empty.getType();
            }
            else {
                if (pos > 100) {
                    return rval;
                }
            }
            if (_tuple != null) tdecl = _tuple.getDeclaration();
            if (_p != null) {
                _p.setName("arg" + pos);
                rval.add(_p);
            }
            pos++;
        }
        return rval;
    }

    /** This method encodes the type parameters of a Tuple in the same way
     * as a parameter list for runtime. */
    private static void encodeTupleAsParameterListForRuntime(final Node node,
            ProducedType _tuple, boolean nameAndMetatype, GenerateJsVisitor gen) {
        gen.out("[");
        int pos = 1;
        TypeDeclaration tdecl = _tuple.getDeclaration();
        final TypeDeclaration empty = tdecl.getUnit().getEmptyDeclaration();
        final TypeDeclaration tuple = tdecl.getUnit().getTupleDeclaration();
        while (!(empty.equals(tdecl) || tdecl instanceof TypeParameter)) {
            if (pos > 1) gen.out(",");
            gen.out("{");
            pos++;
            if (nameAndMetatype) {
                gen.out(MetamodelGenerator.KEY_NAME, ":'p", Integer.toString(pos), "',");
                gen.out(MetamodelGenerator.KEY_METATYPE, ":'", MetamodelGenerator.METATYPE_PARAMETER, "',");
            }
            gen.out(MetamodelGenerator.KEY_TYPE, ":");
            if (tuple.equals(tdecl) || (tdecl.getCaseTypeDeclarations() != null
                    && tdecl.getCaseTypeDeclarations().size()==2
                    && tdecl.getCaseTypeDeclarations().contains(tuple))) {
                if (tuple.equals(tdecl)) {
                    metamodelTypeNameOrList(node, gen.getCurrentPackage(), _tuple.getTypeArgumentList().get(1), gen);
                    _tuple = _tuple.getTypeArgumentList().get(2);
                } else {
                    //Handle union types for defaulted parameters
                    for (ProducedType mt : _tuple.getCaseTypes()) {
                        if (tuple.equals(mt.getDeclaration())) {
                            metamodelTypeNameOrList(node, gen.getCurrentPackage(), mt.getTypeArgumentList().get(1), gen);
                            _tuple = mt.getTypeArgumentList().get(2);
                            break;
                        }
                    }
                    gen.out(",", MetamodelGenerator.KEY_DEFAULT,":1");
                }
            } else if (tdecl.inherits(tdecl.getUnit().getSequentialDeclaration())) {
                ProducedType _t2 = _tuple.getSupertype(tdecl.getUnit().getSequentialDeclaration());
                //Handle Sequence, for nonempty variadic parameters
                metamodelTypeNameOrList(node, gen.getCurrentPackage(), _t2.getTypeArgumentList().get(0), gen);
                gen.out(",seq:1");
                _tuple = empty.getType();
            } else if (tdecl instanceof UnionType) {
                metamodelTypeNameOrList(node, gen.getCurrentPackage(), _tuple, gen);
                tdecl = empty; _tuple=null;
            } else {
                gen.out("\n/*WARNING3! Tuple is actually ", _tuple.getProducedTypeQualifiedName(), ", ", tdecl.getName(),"*/");
                if (pos > 100) {
                    break;
                }
            }
            gen.out("}");
            if (_tuple != null) tdecl = _tuple.getDeclaration();
        }
        gen.out("]");
    }

    /** This method encodes the Arguments type argument of a Callable the same way
     * as a parameter list for runtime. */
    static void encodeCallableArgumentsAsParameterListForRuntime(final Node node,
            ProducedType _callable, GenerateJsVisitor gen) {
        if (_callable.getCaseTypes() != null) {
            for (ProducedType pt : _callable.getCaseTypes()) {
                if (pt.getProducedTypeQualifiedName().startsWith("ceylon.language::Callable<")) {
                    _callable = pt;
                    break;
                }
            }
        } else if (_callable.getSatisfiedTypes() != null) {
            for (ProducedType pt : _callable.getSatisfiedTypes()) {
                if (pt.getProducedTypeQualifiedName().startsWith("ceylon.language::Callable<")) {
                    _callable = pt;
                    break;
                }
            }
        }
        if (!_callable.getProducedTypeQualifiedName().contains("ceylon.language::Callable<")) {
            gen.out("[/*WARNING1: got ", _callable.getProducedTypeQualifiedName(), " instead of Callable*/]");
            return;
        }
        List<ProducedType> targs = _callable.getTypeArgumentList();
        if (targs == null || targs.size() != 2) {
            gen.out("[/*WARNING2: missing argument types for Callable*/]");
            return;
        }
        encodeTupleAsParameterListForRuntime(node, targs.get(1), true, gen);
    }

    static void encodeForRuntime(Node that, final Declaration d, final GenerateJsVisitor gen) {
        if (d.getAnnotations() == null || d.getAnnotations().isEmpty()) {
            encodeForRuntime(that, d, gen, null);
        } else {
            encodeForRuntime(that, d, gen, new ModelAnnotationGenerator(gen, d, that));
        }
    }

    /** Output a metamodel map for runtime use. */
    static void encodeForRuntime(final Declaration d, final Tree.AnnotationList annotations, final GenerateJsVisitor gen) {
        encodeForRuntime(annotations, d, gen, new RuntimeMetamodelAnnotationGenerator() {
            @Override public void generateAnnotations() {
                outputAnnotationsFunction(annotations, d, gen);
            }
        });
    }

    /** Returns the list of keys to get from the package to the declaration, in the model. */
    public static List<String> generateModelPath(final Declaration d) {
        final ArrayList<String> sb = new ArrayList<>();
        final String pkgName = d.getUnit().getPackage().getNameAsString();
        sb.add(Module.LANGUAGE_MODULE_NAME.equals(pkgName)?"$":pkgName);
        if (d.isToplevel()) {
            sb.add(d.getName());
            if (d instanceof Setter) {
                sb.add("$set");
            }
        } else {
            Declaration p = d;
            final int i = sb.size();
            while (p instanceof Declaration) {
                if (p instanceof Setter) {
                    sb.add(i, "$set");
                }
                sb.add(i, TypeUtils.modelName(p));
                //Build the path in reverse
                if (!p.isToplevel()) {
                    if (p instanceof com.redhat.ceylon.compiler.typechecker.model.Class) {
                        sb.add(i, p.isAnonymous() ? MetamodelGenerator.KEY_OBJECTS : MetamodelGenerator.KEY_CLASSES);
                    } else if (p instanceof com.redhat.ceylon.compiler.typechecker.model.Interface) {
                        sb.add(i, MetamodelGenerator.KEY_INTERFACES);
                    } else if (p instanceof Method) {
                        sb.add(i, MetamodelGenerator.KEY_METHODS);
                    } else if (p instanceof TypeAlias || p instanceof Setter) {
                        sb.add(i, MetamodelGenerator.KEY_ATTRIBUTES);
                    } else if (p instanceof Constructor) {
                        sb.add(i, MetamodelGenerator.KEY_CONSTRUCTORS);
                    } else { //It's a value
                        TypeDeclaration td=((TypedDeclaration)p).getTypeDeclaration();
                        sb.add(i, (td!=null&&td.isAnonymous())? MetamodelGenerator.KEY_OBJECTS
                                : MetamodelGenerator.KEY_ATTRIBUTES);
                    }
                }
                p = Util.getContainingDeclaration(p);
            }
        }
        return sb;
    }

    static void outputModelPath(final Declaration d, GenerateJsVisitor gen) {
        List<String> parts = generateModelPath(d);
        gen.out("[");
        boolean first = true;
        for (String p : parts) {
            if (p.startsWith("anon$") || p.startsWith("anonymous#"))continue;
            if (first)first=false;else gen.out(",");
            gen.out("'", p, "'");
        }
        gen.out("]");
    }

    static void encodeForRuntime(final Node that, final Declaration d, final GenerateJsVisitor gen,
            final RuntimeMetamodelAnnotationGenerator annGen) {
        gen.out("function(){return{mod:$CCMM$");
        List<TypeParameter> tparms = d instanceof Generic ? ((Generic)d).getTypeParameters() : null;
        List<ProducedType> satisfies = null;
        List<ProducedType> caseTypes = null;
        if (d instanceof com.redhat.ceylon.compiler.typechecker.model.Class) {
            com.redhat.ceylon.compiler.typechecker.model.Class _cd = (com.redhat.ceylon.compiler.typechecker.model.Class)d;
            if (_cd.getExtendedType() != null) {
                gen.out(",'super':");
                metamodelTypeNameOrList(that, d.getUnit().getPackage(), _cd.getExtendedType(), gen);
            }
            //Parameter types
            if (_cd.getParameterList()!=null) {
                gen.out(",", MetamodelGenerator.KEY_PARAMS, ":");
                encodeParameterListForRuntime(that, _cd.getParameterList(), gen);
            }
            satisfies = _cd.getSatisfiedTypes();
            caseTypes = _cd.getCaseTypes();

        } else if (d instanceof com.redhat.ceylon.compiler.typechecker.model.Interface) {

            satisfies = ((com.redhat.ceylon.compiler.typechecker.model.Interface) d).getSatisfiedTypes();
            caseTypes = ((com.redhat.ceylon.compiler.typechecker.model.Interface) d).getCaseTypes();

        } else if (d instanceof MethodOrValue) {

            gen.out(",", MetamodelGenerator.KEY_TYPE, ":");
            //This needs a new setting to resolve types but not type parameters
            metamodelTypeNameOrList(that, d.getUnit().getPackage(), ((MethodOrValue)d).getType(), gen);
            if (d instanceof Method) {
                gen.out(",", MetamodelGenerator.KEY_PARAMS, ":");
                //Parameter types of the first parameter list
                encodeParameterListForRuntime(that, ((Method)d).getParameterLists().get(0), gen);
                tparms = ((Method) d).getTypeParameters();
            }

        } else if (d instanceof Constructor) {
            gen.out(",", MetamodelGenerator.KEY_PARAMS, ":");
            encodeParameterListForRuntime(that, ((Constructor)d).getParameterLists().get(0), gen);
        }
        if (!d.isToplevel()) {
            //Find the first container that is a Declaration
            Declaration _cont = Util.getContainingDeclaration(d);
            gen.out(",$cont:");
            boolean generateName = true;
            if (_cont.getName().startsWith("anonymous#")) {
                //Anon functions don't have metamodel so go up until we find a non-anon container
                Declaration _supercont = Util.getContainingDeclaration(_cont);
                while (_supercont != null && _supercont.getName().startsWith("anonymous#")) {
                    _supercont = Util.getContainingDeclaration(_supercont);
                }
                if (_supercont == null) {
                    //If the container is a package, add it because this isn't really toplevel
                    generateName = false;
                    gen.out("0");
                } else {
                    _cont = _supercont;
                }
            }
            if (generateName) {
                if (_cont instanceof Value) {
                    if (gen.defineAsProperty(_cont)) {
                        gen.qualify(that, _cont);
                        gen.out("$prop$");
                    }
                    gen.out(gen.getNames().getter(_cont));
                } else if (_cont instanceof Setter) {
                    gen.out("{setter:");
                    if (gen.defineAsProperty(_cont)) {
                        gen.qualify(that, _cont);
                        gen.out("$prop$", gen.getNames().getter(((Setter) _cont).getGetter()), ".set");
                    } else {
                        gen.out(gen.getNames().setter(((Setter) _cont).getGetter()));
                    }
                    gen.out("}");
                } else {
                    boolean inProto = gen.opts.isOptimize()
                            && (_cont.getContainer() instanceof TypeDeclaration);
                    final String path = gen.qualifiedPath(that, _cont, inProto);
                    if (path != null && !path.isEmpty()) {
                        gen.out(path, ".");
                    }
                    gen.out(gen.getNames().name(_cont));
                }
            }
        }
        if (tparms != null && !tparms.isEmpty()) {
            gen.out(",", MetamodelGenerator.KEY_TYPE_PARAMS, ":{");
            encodeTypeParametersForRuntime(that, d, tparms, true, gen);
            gen.out("}");
        }
        if (satisfies != null && !satisfies.isEmpty()) {
            gen.out(",", MetamodelGenerator.KEY_SATISFIES, ":[");
            boolean first = true;
            for (ProducedType st : satisfies) {
                if (!first)gen.out(",");
                first=false;
                metamodelTypeNameOrList(that, d.getUnit().getPackage(), st, gen);
            }
            gen.out("]");
        }
        if (caseTypes != null && !caseTypes.isEmpty()) {
            gen.out(",of:[");
            boolean first = true;
            for (ProducedType st : caseTypes) {
                if (!first)gen.out(",");
                first=false;
                if (st.getDeclaration().isAnonymous()) {
                    gen.out("$prop$", gen.getNames().getter(st.getDeclaration()));
                } else {
                    metamodelTypeNameOrList(that, d.getUnit().getPackage(), st, gen);
                }
            }
            gen.out("]");
        }
        if (annGen != null) {
            annGen.generateAnnotations();
        }
        //Path to its model
        gen.out(",d:");
        outputModelPath(d, gen);
        gen.out("};}");
    }

    static boolean encodeTypeParametersForRuntime(final Node node, final Declaration d,
            final List<TypeParameter> tparms, boolean first, final GenerateJsVisitor gen) {
        for(TypeParameter tp : tparms) {
            boolean comma = false;
            if (!first)gen.out(",");
            first=false;
            gen.out(tp.getName(), "$", tp.getDeclaration().getName(), ":{");
            if (tp.isCovariant()) {
                gen.out(MetamodelGenerator.KEY_DS_VARIANCE, ":'out'");
                comma = true;
            } else if (tp.isContravariant()) {
                gen.out(MetamodelGenerator.KEY_DS_VARIANCE, ":'in'");
                comma = true;
            }
            List<ProducedType> typelist = tp.getSatisfiedTypes();
            if (typelist != null && !typelist.isEmpty()) {
                if (comma)gen.out(",");
                gen.out(MetamodelGenerator.KEY_SATISFIES, ":[");
                boolean first2 = true;
                for (ProducedType st : typelist) {
                    if (!first2)gen.out(",");
                    first2=false;
                    metamodelTypeNameOrList(node, d.getUnit().getPackage(), st, gen);
                }
                gen.out("]");
                comma = true;
            }
            typelist = tp.getCaseTypes();
            if (typelist != null && !typelist.isEmpty()) {
                if (comma)gen.out(",");
                gen.out("of:[");
                boolean first3 = true;
                for (ProducedType st : typelist) {
                    if (!first3)gen.out(",");
                    first3=false;
                    metamodelTypeNameOrList(node, d.getUnit().getPackage(), st, gen);
                }
                gen.out("]");
                comma = true;
            }
            if (tp.getDefaultTypeArgument() != null) {
                if (comma)gen.out(",");
                gen.out("def:");
                metamodelTypeNameOrList(node, d.getUnit().getPackage(), tp.getDefaultTypeArgument(), gen);
            }
            gen.out("}");
        }
        return first;
    }

    /** Prints out an object with a type constructor under the property "t" and its type arguments under
     * the property "a", or a union/intersection type with "u" or "i" under property "t" and the list
     * of types that compose it in an array under the property "l", or a type parameter as a reference to
     * already existing params. */
    static void metamodelTypeNameOrList(final Node node,
            final com.redhat.ceylon.compiler.typechecker.model.Package pkg,
            ProducedType pt, GenerateJsVisitor gen) {
        if (pt == null) {
            //In dynamic blocks we sometimes get a null producedType
            pt = pkg.getUnit().getAnythingDeclaration().getType();
        }
        if (!outputMetamodelTypeList(node, pkg, pt, gen)) {
            TypeDeclaration type = pt.getDeclaration();
            if (type instanceof TypeParameter) {
                gen.out("'", type.getNameAsString(), "$", ((TypeParameter)type).getDeclaration().getName(), "'");
            } else if (type instanceof TypeAlias) {
                outputQualifiedTypename(node, gen.isImported(pkg, type), pt, gen, false);
            } else {
                gen.out("{t:");
                outputQualifiedTypename(node, gen.isImported(pkg, type), pt, gen, false);
                //Type Parameters
                if (!pt.getTypeArgumentList().isEmpty()) {
                    gen.out(",a:{");
                    boolean first = true;
                    for (Map.Entry<TypeParameter, ProducedType> e : pt.getTypeArguments().entrySet()) {
                        if (first) first=false; else gen.out(",");
                        gen.out(e.getKey().getNameAsString(), "$", e.getKey().getDeclaration().getName(), ":");
                        metamodelTypeNameOrList(node, pkg, e.getValue(), gen);
                    }
                    gen.out("}");
                }
                gen.out("}");
            }
        }
    }

    /** Appends an object with the type's type and list of union/intersection types; works only with union,
     * intersection and tuple types.
     * @return true if output was generated, false otherwise (it was a regular type) */
    static boolean outputMetamodelTypeList(final Node node,
            final com.redhat.ceylon.compiler.typechecker.model.Package pkg,
            ProducedType pt, GenerateJsVisitor gen) {
        TypeDeclaration type = pt.getDeclaration();
        final List<ProducedType> subs;
        if (type instanceof IntersectionType) {
            gen.out("{t:'i");
            subs = type.getSatisfiedTypes();
        } else if (type instanceof UnionType) {
            //It still could be a Tuple with first optional type
            List<TypeDeclaration> cts = type.getCaseTypeDeclarations();
            if (cts.size()==2 && cts.contains(type.getUnit().getEmptyDeclaration())
                    && cts.contains(type.getUnit().getTupleDeclaration())) {
                //yup...
                gen.out("{t:'T',l:");
                encodeTupleAsParameterListForRuntime(node, pt,false,gen);
                gen.out("}");
                return true;
            }
            gen.out("{t:'u");
            subs = type.getCaseTypes();
        } else if (type.getQualifiedNameString().equals("ceylon.language::Tuple")) {
            gen.out("{t:'T',l:");
            encodeTupleAsParameterListForRuntime(node, pt,false, gen);
            gen.out("}");
            return true;
        } else {
            return false;
        }
        gen.out("',l:[");
        boolean first = true;
        for (ProducedType t : subs) {
            if (!first) gen.out(",");
            metamodelTypeNameOrList(node, pkg, t, gen);
            first = false;
        }
        gen.out("]}");
        return true;
    }

    static String pathToModelDoc(final Declaration d) {
        if (d == null)return null;
        final StringBuilder sb = new StringBuilder();
        for (String p : generateModelPath(d)) {
            sb.append(sb.length() == 0 ? '\'' : ':').append(p);
        }
        sb.append('\'');
        return sb.toString();
    }

    /** Outputs a function that returns the specified annotations, so that they can be loaded lazily.
     * @param annotations The annotations to be output.
     * @param d The declaration to which the annotations belong.
     * @param gen The generator to use for output. */
    static void outputAnnotationsFunction(final Tree.AnnotationList annotations, final Declaration d,
            final GenerateJsVisitor gen) {
        List<Tree.Annotation> anns = annotations == null ? null : annotations.getAnnotations();
        if (d != null) {
            int mask = MetamodelGenerator.encodeAnnotations(d, null);
            if (mask > 0) {
                gen.out(",", MetamodelGenerator.KEY_PACKED_ANNS, ":", Integer.toString(mask));
            }
            if (annotations == null || (anns.isEmpty() && annotations.getAnonymousAnnotation() == null)) {
                return;
            }
            anns = new ArrayList<>(annotations.getAnnotations().size());
            anns.addAll(annotations.getAnnotations());
            for (Iterator<Tree.Annotation> iter = anns.iterator(); iter.hasNext();) {
                final String qn = ((Tree.StaticMemberOrTypeExpression)iter.next().getPrimary()).getDeclaration().getQualifiedNameString();
                if (qn.startsWith("ceylon.language::") && MetamodelGenerator.annotationBits.contains(qn.substring(17))) {
                    iter.remove();
                }
            }
            if (anns.isEmpty() && annotations.getAnonymousAnnotation() == null) {
                return;
            }
            gen.out(",", MetamodelGenerator.KEY_ANNOTATIONS, ":");
        }
        if (annotations == null || (anns.isEmpty() && annotations.getAnonymousAnnotation()==null)) {
            gen.out("[]");
        } else {
            gen.out("function(){return[");
            boolean first = true;
            //Leave the annotation but remove the doc from runtime for brevity
            if (annotations.getAnonymousAnnotation() != null) {
                first = false;
                final Tree.StringLiteral lit = annotations.getAnonymousAnnotation().getStringLiteral();
                final String ptmd = pathToModelDoc(d);
                if (ptmd != null && ptmd.length() < lit.getText().length()) {
                    gen.out(gen.getClAlias(), "doc$($CCMM$,", ptmd);
                } else {
                    gen.out(gen.getClAlias(), "doc(");
                    lit.visit(gen);
                }
                gen.out(")");
            }
            for (Tree.Annotation a : anns) {
                if (first) first=false; else gen.out(",");
                gen.getInvoker().generateInvocation(a);
            }
            gen.out("];}");
        }
    }

    /** Abstraction for a callback that generates the runtime annotations list as part of the metamodel. */
    static interface RuntimeMetamodelAnnotationGenerator {
        public void generateAnnotations();
    }

    static class ModelAnnotationGenerator implements RuntimeMetamodelAnnotationGenerator {
        private final GenerateJsVisitor gen;
        private final Declaration d;
        private final Node node;
        ModelAnnotationGenerator(GenerateJsVisitor generator, Declaration decl, Node n) {
            gen = generator;
            d = decl;
            node = n;
        }
        @Override public void generateAnnotations() {
            List<Annotation> anns = d.getAnnotations();
            final int bits = MetamodelGenerator.encodeAnnotations(d, null);
            if (bits > 0) {
                gen.out(",", MetamodelGenerator.KEY_PACKED_ANNS, ":", Integer.toString(bits));
                //Remove these annotations from the list
                anns = new ArrayList<Annotation>(d.getAnnotations().size());
                anns.addAll(d.getAnnotations());
                for (Iterator<Annotation> iter = anns.iterator(); iter.hasNext();) {
                    final Annotation a = iter.next();
                    final Declaration ad = d.getUnit().getPackage().getMemberOrParameter(d.getUnit(), a.getName(), null, false);
                    final String qn = ad.getQualifiedNameString();
                    if (qn.startsWith("ceylon.language::") && MetamodelGenerator.annotationBits.contains(qn.substring(17))) {
                        iter.remove();
                    }
                }
                if (anns.isEmpty()) {
                    return;
                }
            }
            gen.out(",", MetamodelGenerator.KEY_ANNOTATIONS, ":function(){return[");
            boolean first = true;
            for (Annotation a : anns) {
                Declaration ad = d.getUnit().getPackage().getMemberOrParameter(d.getUnit(), a.getName(), null, false);
                if (ad instanceof Method) {
                    if (first) first=false; else gen.out(",");
                    final boolean isDoc = "ceylon.language::doc".equals(ad.getQualifiedNameString());
                    if (!isDoc) {
                        gen.qualify(node, ad);
                        gen.out(gen.getNames().name(ad), "(");
                    }
                    if (a.getPositionalArguments() == null) {
                        for (Parameter p : ((Method)ad).getParameterLists().get(0).getParameters()) {
                            String v = a.getNamedArguments().get(p.getName());
                            gen.out(v == null ? "undefined" : v);
                        }
                    } else {
                        if (isDoc) {
                            //Use ref if it's too long
                            final String ref = pathToModelDoc(d);
                            final String doc = a.getPositionalArguments().get(0);
                            if (ref != null && ref.length() < doc.length()) {
                                gen.out(gen.getClAlias(), "doc$($CCMM$,", ref);
                            } else {
                                gen.out(gen.getClAlias(), "doc(\"", gen.escapeStringLiteral(doc), "\"");
                            }
                        } else {
                            boolean farg = true;
                            for (String s : a.getPositionalArguments()) {
                                if (farg)farg=false; else gen.out(",");
                                gen.out("\"", gen.escapeStringLiteral(s), "\"");
                            }
                        }
                    }
                    gen.out(")");
                } else {
                    gen.out("/*MISSING DECLARATION FOR ANNOTATION ", a.getName(), "*/");
                }
            }
            gen.out("];}");
        }
    }

    /** Generates the right type arguments for operators that are sugar for method calls.
     * @param left The left term of the operator
     * @param right The right term of the operator
     * @param methodName The name of the method that is to be invoked
     * @param rightTpName The name of the type argument on the right term
     * @param leftTpName The name of the type parameter on the method
     * @return A map with the type parameter of the method as key
     * and the produced type belonging to the type argument of the term on the right. */
    static Map<TypeParameter, ProducedType> mapTypeArgument(final Tree.BinaryOperatorExpression expr,
            final String methodName, final String rightTpName, final String leftTpName) {
        Method md = (Method)expr.getLeftTerm().getTypeModel().getDeclaration().getMember(methodName, null, false);
        if (md == null) {
            expr.addUnexpectedError("Left term of intersection operator should have method named " + methodName);
            return null;
        }
        Map<TypeParameter, ProducedType> targs = expr.getRightTerm().getTypeModel().getTypeArguments();
        ProducedType otherType = null;
        for (TypeParameter tp : targs.keySet()) {
            if (tp.getName().equals(rightTpName)) {
                otherType = targs.get(tp);
                break;
            }
        }
        if (otherType == null) {
            expr.addUnexpectedError("Right term of intersection operator should have type parameter named " + rightTpName);
            return null;
        }
        targs = new HashMap<>();
        TypeParameter mtp = null;
        for (TypeParameter tp : md.getTypeParameters()) {
            if (tp.getName().equals(leftTpName)) {
                mtp = tp;
                break;
            }
        }
        if (mtp == null) {
            expr.addUnexpectedError("Left term of intersection should have type parameter named " + leftTpName);
        }
        targs.put(mtp, otherType);
        return targs;
    }

    /** Returns the qualified name of a declaration, skipping any containing methods. */
    public static String qualifiedNameSkippingMethods(Declaration d) {
        final StringBuilder p = new StringBuilder(d.getName());
        Scope s = d.getContainer();
        while (s != null) {
            if (s instanceof com.redhat.ceylon.compiler.typechecker.model.Package) {
                final String pkname = ((com.redhat.ceylon.compiler.typechecker.model.Package)s).getNameAsString();
                if (!pkname.isEmpty()) {
                    p.insert(0, "::");
                    p.insert(0, pkname);
                }
            } else if (s instanceof TypeDeclaration) {
                p.insert(0, '.');
                p.insert(0, ((TypeDeclaration)s).getName());
            }
            s = s.getContainer();
        }
        return p.toString();
    }

    public static String modelName(Declaration d) {
        String dname = d.getName();
        if (dname.startsWith("anonymous#")) {
            dname = "anon$" + dname.substring(10);
        }
        if (d.isToplevel() || d.isShared()) {
            return dname;
        }
        if (d instanceof Setter) {
            d = ((Setter)d).getGetter();
        }
        return dname+"$"+Long.toString(Math.abs((long)d.hashCode()), 36);
    }

}
