package com.redhat.ceylon.compiler.js;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.redhat.ceylon.cmr.api.RepositoryManager;
import com.redhat.ceylon.cmr.ceylon.CeylonUtils;
import com.redhat.ceylon.cmr.impl.JULLogger;
import com.redhat.ceylon.compiler.Options;
import com.redhat.ceylon.compiler.loader.JsModuleManagerFactory;
import com.redhat.ceylon.compiler.typechecker.TypeChecker;
import com.redhat.ceylon.compiler.typechecker.TypeCheckerBuilder;
import com.redhat.ceylon.compiler.typechecker.io.VirtualFile;

/**
 * Entry point for the type checker
 * Pass the source directory as parameter. The source directory is relative to
 * the startup directory.
 *
 * @author Gavin King <gavin@hibernate.org>
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Enrique Zamudio
 */
public class Main {

    /** Print a help message with the available options. */
    private static void help(boolean all) {
        System.err.println("Usage ceylonc-js <options> <source files> <module names>");
        if (all) {
            System.err.println();
            System.err.println("where possible options include:");
            System.err.println("  -rep <url>         Module repository (default: ./modules).");
            System.err.println("                     Can be specified multiple times.");
            System.err.println("  -user <value>      User name for output repository (HTTP only)");
            System.err.println("  -pass <value>      Password for output repository (HTTP only)");
            System.err.println("  -src <directory>   Path to source files (default: ./source)");
            System.err.println("                     Can be specified multiple times; you can also");
            System.err.println("                     specify several paths separated by '" + File.pathSeparator + "'");
            System.err.println("  -out <url>         Output module repository (default: ./modules)");
            System.err.println("  -version           Version information");
            System.err.println("  -help              Print a synopsis of standard options");
            System.err.println();
            System.err.println("Javascript code generation options:");
            System.err.println("  -optimize    Create prototype-style JS code");
            System.err.println("  -nomodule    Do NOT wrap generated code as CommonJS module");
            System.err.println("  -noindent    Do NOT indent code");
            System.err.println("  -nocomments  Do not generate any comments");
            System.err.println("  -compact     Same as -noindent -nocomments");
            System.err.println("  -verbose     Print messages while compiling");
            System.err.println("  -profile     Time the compilation phases (results are printed to STDERR)");
            System.err.println();
            System.err.println("If no files are specified or '--' is used, STDIN is read.");
        } else {
            System.out.println("use -help for a list of possible options");
        }
    }

    /**
     * Files that are not under a proper module structure are placed under a <nomodule> module.
     */
    public static void main(String[] _args) throws Exception {
        long t0, t1, t2, t3, t4;
        List<String> args = new ArrayList<String>(Arrays.asList(_args));
        final Options opts = Options.parse(args);
        if (opts.isVersion()) {
            System.err.println("Version: ceylonc-js 0.4 'Ratatouille'");
            return;
        }
        if (opts.isHelp()) {
            help(true);
            return;
        }
        if (args.size() == 0) {
            help(false);
            return;
        }

        final TypeChecker typeChecker;
        if (opts.isVerbose()) {
            System.out.printf("Using repositories: %s%n", opts.getRepos());
        }
        final RepositoryManager repoman = CeylonUtils.makeRepositoryManager(
                opts.getRepos(), opts.getOutDir(), new JULLogger());
        final Set<String> onlyFiles = new HashSet<String>();
        if (opts.isStdin()) {
            VirtualFile src = new VirtualFile() {
                @Override
                public boolean isFolder() {
                    return false;
                }
                @Override
                public String getName() {
                    return "SCRIPT.ceylon";
                }
                @Override
                public String getPath() {
                    return getName();
                }
                @Override
                public InputStream getInputStream() {
                    return System.in;
                }
                @Override
                public List<VirtualFile> getChildren() {
                    return new ArrayList<VirtualFile>(0);
                }
                @Override
                public int hashCode() {
                    return getPath().hashCode();
                }
                @Override
                public boolean equals(Object obj) {
                    if (obj instanceof VirtualFile) {
                        return ((VirtualFile) obj).getPath().equals(getPath());
                    }
                    else {
                        return super.equals(obj);
                    }
                }
            };
            t0 = System.nanoTime();
            TypeCheckerBuilder tcb = new TypeCheckerBuilder()
                .verbose(opts.isVerbose())
                .addSrcDirectory(src);
            tcb.setRepositoryManager(repoman);
            typeChecker = tcb.getTypeChecker();
            t1=System.nanoTime();
        } else {
            t0=System.nanoTime();
            TypeCheckerBuilder tcb = new TypeCheckerBuilder()
                .verbose(opts.isVerbose());
            tcb.setRepositoryManager(repoman);
            final List<File> roots = new ArrayList<File>(opts.getSrcDirs().size());
            for (String _srcdir : opts.getSrcDirs()) {
                roots.add(new File(_srcdir));
            }
            final Set<String> modfilters = new HashSet<String>();
            boolean stop = false;
            for (String filedir : args) {
                File f = new File(filedir);
                boolean once=false;
                if (f.exists() && f.isFile()) {
                    for (File root : roots) {
                        if (f.getAbsolutePath().startsWith(root.getAbsolutePath())) {
                            if (opts.isVerbose()) {
                                System.out.printf("Adding %s to compilation set%n", filedir);
                            }
                            onlyFiles.add(filedir);
                            once=true;
                            break;
                        }
                    }
                    if (!once) {
                        System.err.printf("%s is not in any source path: %n", f.getAbsolutePath());
                        stop=true;
                        f=null;
                    }
                } else if ("default".equals(filedir)) {
                    //Default module: load every file in the source directories recursively,
                    //except any file that exists in directories and subdirectories where we find a module.ceylon file
                    //Typechecker takes care of all that if we add default to module filters
                    if (opts.isVerbose()) {
                        System.out.println("Adding default module filter");
                    }
                    modfilters.add("default");
                    f = null;
                } else {
                    //Parse, may be a module name
                    String[] modpath = filedir.split("\\.");
                    f = null;
                    for (File root : roots) {
                        File _f = root;
                        for (String pe : modpath) {
                            _f = new File(_f, pe);
                            if (!(_f.exists() && _f.isDirectory())) {
                                System.err.printf("ceylonc-js: Could not find source files for module: %s%n", filedir);
                                _f=null;
                                break;
                            }
                        }
                        if (_f != null) {
                            f = _f;
                        }
                    }
                    if (f == null) {
                        System.err.printf("ceylonc-js: file not found: %s%n", filedir);
                        stop=true;
                    } else {
                        if (opts.isVerbose()) {
                            System.out.println("Adding to module filters: " + filedir);
                        }
                        for (File e : f.listFiles()) {
                            String n = e.getName().toLowerCase();
                            if (e.isFile() && n.endsWith(".ceylon") && !n.equals("module.ceylon")) {
                                if (opts.isVerbose()) {
                                    System.out.println("Adding to compilation set: " + e.getPath());
                                }
                                onlyFiles.add(e.getPath());
                            }
                        }
                        modfilters.add(filedir);
                        f = null;
                    }
                }
                if (f != null) {
                    if ("module.ceylon".equals(f.getName().toLowerCase())) {
                        String _f = f.getParentFile().getAbsolutePath();
                        for (File root : roots) {
                            if (root.getAbsolutePath().startsWith(_f)) {
                                _f = _f.substring(root.getAbsolutePath().length()+1).replace(File.separator, ".");
                                modfilters.add(_f);
                                if (opts.isVerbose()) {
                                    System.out.println("Adding to module filters: " + _f);
                                }
                            }
                        }
                    } else {
                        for (File root : roots) {
                            File middir = f.getParentFile();
                            while (middir != null && !middir.getAbsolutePath().equals(root.getAbsolutePath())) {
                                if (new File(middir, "module.ceylon").exists()) {
                                    String _f = middir.getAbsolutePath().substring(root.getAbsolutePath().length()+1).replace(File.separator, ".");
                                    modfilters.add(_f);
                                    if (opts.isVerbose()) {
                                        System.out.println("Adding to module filters: " + _f);
                                    }
                                }
                                middir = middir.getParentFile();
                            }
                        }
                    }
                }
            }
            if (stop) {
                help(false);
                return;
            }
            for (File root : roots) {
                tcb.addSrcDirectory(root);
            }
            if (!modfilters.isEmpty()) {
                ArrayList<String> _modfilters = new ArrayList<String>();
                _modfilters.addAll(modfilters);
                tcb.setModuleFilters(_modfilters);
            }
            tcb.statistics(opts.isProfile());
            //tcb.moduleManagerFactory(new JsModuleManagerFactory());
            typeChecker = tcb.getTypeChecker();
            t1=System.nanoTime();
        }
        //getting the type checker does process all types in the source directory
        typeChecker.process();
        t2=System.nanoTime();
        JsCompiler jsc = new JsCompiler(typeChecker, opts);
        if (!onlyFiles.isEmpty()) { jsc.setFiles(onlyFiles); }
        t3=System.nanoTime();
        if (!jsc.generate()) {
            jsc.printErrors(System.out);
        }
        t4=System.nanoTime();
        if (opts.isProfile() || opts.isVerbose()) {
            System.err.println("PROFILING INFORMATION");
            System.err.printf("TypeChecker creation:   %6d nanos%n", t1-t0);
            System.err.printf("TypeChecker processing: %6d nanos%n", t2-t1);
            System.err.printf("JS compiler creation:   %6d nanos%n", t3-t2);
            System.err.printf("JS compilation:         %6d nanos%n", t4-t3);
        }
        System.out.println("Compilation finished.");
    }
}
