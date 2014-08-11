package com.redhat.ceylon.compiler.loader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.redhat.ceylon.cmr.api.ArtifactContext;
import com.redhat.ceylon.cmr.api.ArtifactResult;
import com.redhat.ceylon.cmr.impl.JSUtils;
import com.redhat.ceylon.common.Constants;
import com.redhat.ceylon.common.Versions;
import com.redhat.ceylon.common.config.CeylonConfig;
import com.redhat.ceylon.common.config.DefaultToolOptions;
import com.redhat.ceylon.compiler.js.CeylonRunJsException;
import com.redhat.ceylon.compiler.js.CompilerErrorException;
import com.redhat.ceylon.compiler.typechecker.analyzer.ModuleManager;
import com.redhat.ceylon.compiler.typechecker.context.Context;
import com.redhat.ceylon.compiler.typechecker.context.PhasedUnits;
import com.redhat.ceylon.compiler.typechecker.model.Module;
import com.redhat.ceylon.compiler.typechecker.model.ModuleImport;
import com.redhat.ceylon.compiler.typechecker.model.Unit;

/** A ModuleManager that loads modules from js files.
 * 
 * @author Enrique Zamudio
 */
public class JsModuleManager extends ModuleManager {

    /** Tells whether the language module has been loaded yet. */
    private boolean clLoaded;
	private String encoding;
    private static final String BIN_VERSION = Versions.JS_BINARY_MAJOR_VERSION + "." + Versions.JS_BINARY_MINOR_VERSION;

    public JsModuleManager(Context context, String encoding) {
        super(context);
        this.encoding = encoding;
    }

    @Override
    public void resolveModule(final ArtifactResult artifact, final Module module,
            final ModuleImport moduleImport, LinkedList<Module> dependencyTree,
            List<PhasedUnits> phasedUnitsOfDependencies, boolean forCompiledModule) {
        if (!clLoaded) {
            clLoaded = true;
            //If we haven't loaded the language module yet, we need to load it first
            if (!(Module.LANGUAGE_MODULE_NAME.equals(artifact.name())
                    && artifact.artifact().getName().endsWith(ArtifactContext.JS_MODEL))) {
                if (JsModuleManagerFactory.isVerbose()) {
                    System.out.println("Loading JS language module before any other modules");
                }
                ArtifactContext ac = new ArtifactContext(Module.LANGUAGE_MODULE_NAME,
                        module.getLanguageModule().getVersion(), ArtifactContext.JS_MODEL);
                ac.setFetchSingleArtifact(true);
                ac.setThrowErrorIfMissing(true);
                ArtifactResult lmar = getContext().getRepositoryManager().getArtifactResult(ac);
                resolveModule(lmar, module.getLanguageModule(), null, dependencyTree,
                        phasedUnitsOfDependencies, forCompiledModule);
            }
            //Then we continue loading whatever they asked for first.
        }
        //Create a similar artifact but with -model.js extension
        File js = artifact.artifact();
        if (js.getName().endsWith(ArtifactContext.JS) && !js.getName().endsWith(ArtifactContext.JS_MODEL)) {
            ArtifactContext ac = new ArtifactContext(artifact.name(),
                    artifact.version(), ArtifactContext.JS_MODEL);
            ac.setFetchSingleArtifact(true);
            ac.setThrowErrorIfMissing(true);
            ArtifactResult lmar = getContext().getRepositoryManager().getArtifactResult(ac);
            js = lmar.artifact();
        }
        if (module instanceof JsonModule) {
            if (((JsonModule)module).getModel() != null) {
                return;
            }
            if (js.exists() && js.isFile() && js.canRead() && js.getName().endsWith(ArtifactContext.JS_MODEL)) {
                if (JsModuleManagerFactory.isVerbose()) {
                    System.out.println("Loading model from " + js);
                }
                Map<String,Object> model = loadJsonModel(js);
                if (model == null) {
                    if (JsModuleManagerFactory.isVerbose()) {
                        System.out.println("Model not found in " + js);
                    }
                } else {
                    loadModuleFromMap(artifact, module, moduleImport, dependencyTree, phasedUnitsOfDependencies,
                            forCompiledModule, model);
                    return;
                }
            }
        }
        super.resolveModule(artifact, module, moduleImport, dependencyTree,
                phasedUnitsOfDependencies, forCompiledModule);
    }

    @Override
    public Iterable<String> getSearchedArtifactExtensions() {
        return Arrays.asList("js");
    }

    @Override
    protected Module createModule(List<String> moduleName, String version) {
        final Module module = new JsonModule();
        module.setName(moduleName);
        module.setVersion(version);
        Unit u = new Unit();
        u.setFilename(Constants.MODULE_DESCRIPTOR);
        u.setFullPath(moduleName+"/"+version);
        module.setUnit(u);
        JsonModule dep = (JsonModule)findLoadedModule(Module.LANGUAGE_MODULE_NAME, null);
        //This can only happen during initCoreModules()
        if (!(module.getNameAsString().equals(Module.DEFAULT_MODULE_NAME) || module.getNameAsString().equals(Module.LANGUAGE_MODULE_NAME)) && dep == null) {
            //Load the language module if we're not inside initCoreModules()
            dep = (JsonModule)getContext().getModules().getLanguageModule();
            //Add language module as a dependency
            //This will cause the dependency to be loaded later
            ModuleImport imp = new ModuleImport(dep, false, false);
            module.addImport(imp);
            module.setLanguageModule(dep);
            //Fix 280 part 1
            getContext().getModules().getDefaultModule().addImport(new ModuleImport(module, false, false));
        }
        return module;
    }

    @Override
    protected com.redhat.ceylon.compiler.typechecker.model.Package createPackage(String pkgName, Module module) {
        if (module!=null && module == getContext().getModules().getDefaultModule())
        try {
            //Fix 280 part 2
            return module.getPackage(pkgName);
        } catch (CompilerErrorException ex) {
            //nothing, package will be created
        }
        final JsonPackage pkg = new JsonPackage(pkgName);
        List<String> name = pkgName.isEmpty() ? Collections.<String>emptyList() : splitModuleName(pkgName); 
        pkg.setName(name);
        if (module != null) {
            module.getPackages().add(pkg);
            pkg.setModule(module);
        }
        return pkg;
    }

    protected void loadModuleFromMap(ArtifactResult artifact, Module module,
            ModuleImport moduleImport, LinkedList<Module> dependencyTree,
            List<PhasedUnits> phasedUnitsOfDependencies, boolean forCompiledModule,
            Map<String, Object> model) {
        @SuppressWarnings("unchecked")
        List<Object> deps = (List<Object>)model.get("$mod-deps");
        if (deps != null) {
            for (Object dep : deps) {
                final String s;
                boolean optional = false;
                boolean export = false;
                if (dep instanceof Map) {
                    @SuppressWarnings("unchecked")
                    final Map<String,Object> depmap = (Map<String,Object>)dep;
                    s = (String)depmap.get("path");
                    optional = depmap.containsKey("opt");
                    export = depmap.containsKey("exp");
                } else {
                    s = (String)dep;
                }
                int p = s.indexOf('/');
                String depname = null;
                String depv = null;
                if (p > 0) {
                    depname = s.substring(0,p);
                    depv = s.substring(p+1);
                    if (depv.isEmpty()) depv = null;
                } else {
                    depname = s;
                }
                //This will cause the dependency to be loaded later
                JsonModule mod = (JsonModule)getOrCreateModule(splitModuleName(depname), depv);
                ModuleImport imp = new ModuleImport(mod, optional, export);
                module.addImport(imp);
            }
            model.remove("$mod-deps");
        }
        ((JsonModule)module).setModel(model);
        for (ModuleImport imp : module.getImports()) {
            if (!imp.getModule().getNameAsString().equals(Module.LANGUAGE_MODULE_NAME)) {
                ArtifactContext ac = new ArtifactContext(imp.getModule().getNameAsString(),
                        imp.getModule().getVersion(), ArtifactContext.JS_MODEL);
                artifact = getContext().getRepositoryManager().getArtifactResult(ac);
                if (artifact != null) {
                    resolveModule(artifact, imp.getModule(), imp, dependencyTree,
                            phasedUnitsOfDependencies, forCompiledModule & imp.isExport());
                }
            }
        }
        ((JsonModule)module).loadDeclarations();
        return;
    }

    /** Read the metamodel declaration from a js file, check it's the right version and return the model as a Map. */
    public static Map<String,Object> loadJsonModel(File jsFile) {
        try {
            Map<String,Object> model = JSUtils.readJsonModel(jsFile);
            if (model == null) {
                throw new CompilerErrorException("Can't find metamodel definition in " + jsFile.getAbsolutePath());
            }
            if (!model.containsKey("$mod-bin")) {
                throw new CeylonRunJsException("The JavaScript module " + jsFile +
                        " is not compatible with the current version of ceylon-js");
            } else if (!model.get("$mod-bin").toString().equals(BIN_VERSION)) {
                throw new CompilerErrorException(String.format("This Ceylon-JS module has binary version %s is incompatible with the compiler version %s",
                        model.get("$mod-bin"), BIN_VERSION));
            }
            return model;
        } catch (IOException ex) {
            throw new CompilerErrorException("Error loading model from " + jsFile);
        }
    }

    @Override
    protected PhasedUnits createPhasedUnits() {
    	PhasedUnits units = super.createPhasedUnits();
        String fileEncoding = encoding;
        if (fileEncoding == null) {
            fileEncoding = CeylonConfig.get(DefaultToolOptions.DEFAULTS_ENCODING);
        }
        if (fileEncoding != null) {
    		units.setEncoding(fileEncoding);
        }
    	return units;
    }

}
