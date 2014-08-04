package com.redhat.ceylon.compiler.js;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.redhat.ceylon.cmr.api.ArtifactContext;
import com.redhat.ceylon.cmr.ceylon.CeylonUtils;
import com.redhat.ceylon.cmr.impl.ShaSigner;
import com.redhat.ceylon.common.Constants;
import com.redhat.ceylon.compiler.Options;
import com.redhat.ceylon.compiler.loader.ModelEncoder;
import com.redhat.ceylon.compiler.typechecker.TypeChecker;
import com.redhat.ceylon.compiler.typechecker.TypeCheckerBuilder;
import com.redhat.ceylon.compiler.typechecker.context.PhasedUnit;
import com.redhat.ceylon.compiler.typechecker.tree.Message;

/** A simple program that takes the main JS module file and replaces #include markers with the contents of other files.
 * 
 * @author Enrique Zamudio
 */
public class Stitcher {

    private static TypeCheckerBuilder langmodtc;
    public static final File clSrcDir = new File("../ceylon.language/src/ceylon/language/");
    public static final File LANGMOD_JS_SRC = new File("../ceylon.language/runtime-js");
    public static final File LANGMOD_JS_SRC2 = new File("../ceylon.language/runtime-js/ceylon/language");

    private static void compileLanguageModule(final String line, Writer writer)
            throws IOException {
        File tmpdir = File.createTempFile("ceylonjs", "clsrc");
        tmpdir.delete();
        tmpdir = new File(tmpdir.getAbsolutePath());
        tmpdir.mkdir();
        tmpdir.deleteOnExit();
        final File tmpout = new File(tmpdir, Constants.DEFAULT_MODULE_DIR);
        tmpout.mkdir();
        tmpout.deleteOnExit();
        final Options opts = new Options().addRepo("build/runtime").comment(false).optimize(true)
                .outRepo(tmpout.getAbsolutePath()).modulify(false).minify(true);

        //Typecheck the whole language module
        if (langmodtc == null) {
            langmodtc = new TypeCheckerBuilder().addSrcDirectory(clSrcDir.getParentFile().getParentFile())
                    .addSrcDirectory(LANGMOD_JS_SRC)
                    .encoding("UTF-8");
            langmodtc.setRepositoryManager(CeylonUtils.repoManager().systemRepo(opts.getSystemRepo())
                    .userRepos(opts.getRepos()).outRepo(opts.getOutRepo()).buildManager());
        }
        final File mod2 = new File(LANGMOD_JS_SRC2, "module.ceylon");
        if (!mod2.exists()) {
            try (FileWriter w2 = new FileWriter(mod2);
                    FileReader r2 = new FileReader(new File(clSrcDir, "module.ceylon"))) {
                char[] c = new char[512];
                int r = r2.read(c);
                while (r != -1) {
                    w2.write(c, 0, r);
                    r = r2.read(c);
                }
                mod2.deleteOnExit();
            } finally {
            }
        }
        final TypeChecker tc = langmodtc.getTypeChecker();
        tc.process();
        if (tc.getErrors() > 0) {
            System.exit(1);
        }

        //Compile these files
        final List<File> includes = new ArrayList<File>();
        for (String filename : line.split(",")) {
            final boolean isJsSrc = filename.trim().endsWith(".js");
            final File src = new File(isJsSrc ? LANGMOD_JS_SRC : clSrcDir,
                    isJsSrc ? filename.trim() :
                    String.format("%s.ceylon", filename.trim()));
            if (src.exists() && src.isFile() && src.canRead()) {
                includes.add(src);
            } else {
                final File src2 = new File(LANGMOD_JS_SRC2, String.format("%s.ceylon", filename.trim()));
                if (src2.exists() && src2.isFile() && src2.canRead()) {
                    includes.add(src2);
                } else {
                    throw new IllegalArgumentException("Invalid Ceylon language module source " + src + " or " + src2);
                }
            }
        }
        //Compile only the files specified in the line
        //Set this before typechecking to share some decls that otherwise would be private
        JsCompiler.compilingLanguageModule=true;
        JsCompiler jsc = new JsCompiler(tc, opts).stopOnErrors(false);
        jsc.setSourceFiles(includes);
        jsc.generate();
        JsCompiler.compilingLanguageModule=false;
        File compsrc = new File(tmpout, "delete/me/delete-me.js");
        if (compsrc.exists() && compsrc.isFile() && compsrc.canRead()) {
            try (BufferedReader jsr = new BufferedReader(new FileReader(compsrc))) {
                String jsline = null;
                while ((jsline = jsr.readLine()) != null) {
                    if (!jsline.contains("=require('")) {
                        writer.write(jsline);
                        writer.write("\n");
                    }
                }
            } finally {
                compsrc.delete();
            }
        } else {
            System.out.println("Can't find generated js for language module in " + compsrc.getAbsolutePath());
            System.exit(1);
        }
    }

    private static void encodeModel(final File moduleFile) throws IOException {
        final String name = moduleFile.getName();
        final File file = new File(moduleFile.getParentFile(),
                name.substring(0,name.length()-3)+ArtifactContext.JS_MODEL);
        System.out.println("Generating language module compile-time model in JSON...");
        TypeCheckerBuilder tcb = new TypeCheckerBuilder().usageWarnings(false);
        tcb.addSrcDirectory(clSrcDir.getParentFile().getParentFile());
        TypeChecker tc = tcb.getTypeChecker();
        tc.process();
        MetamodelVisitor mmg = null;
        for (PhasedUnit pu : tc.getPhasedUnits().getPhasedUnits()) {
            if (!pu.getCompilationUnit().getErrors().isEmpty()) {
                System.out.println("whoa, errors in the language module "
                        + pu.getCompilationUnit().getLocation());
                for (Message err : pu.getCompilationUnit().getErrors()) {
                    System.out.println(err.getMessage());
                }
            }
            if (mmg == null) {
                mmg = new MetamodelVisitor(pu.getPackage().getModule());
            }
            pu.getCompilationUnit().visit(mmg);
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("exports.$CCMM$=");
            ModelEncoder.encodeModel(mmg.getModel(), writer);
            writer.write(";\n");
            compileLanguageModule("MODEL.js", writer);
        } finally {
        }
    }

    private static void stitch(File infile, Writer writer, String version) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(infile), "UTF-8"));
        try {
            String line = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0) {
                    if (line.equals("//#METAMODEL")) {
                        writer.write("var _CTM$;function $CCMM$(){if (_CTM$===undefined)_CTM$=require('");
                        writer.write("ceylon/language/");
                        writer.write(version);
                        writer.write("/ceylon.language-");
                        writer.write(version);
                        writer.write(ArtifactContext.JS_MODEL);
                        writer.write("').$CCMM$;return _CTM$;}\n");
                        writer.write("ex$.$CCMM$=$CCMM$;");
                    } else if (line.startsWith("//#COMPILE ")) {
                        final String sourceFiles = line.substring(11);
                        System.out.println("Compiling language module sources: " + sourceFiles);
                        compileLanguageModule(sourceFiles, writer);
                    } else if (!line.endsWith("//IGNORE")) {
                        writer.write(line);
                        writer.write("\n");
                    }
                }
            }
        } finally {
            if (reader != null) reader.close();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("This program requires 3 arguments to run:");
            System.err.println("1. The path to the main JS file");
            System.err.println("2. The path of the resulting JS file");
            System.exit(1);
            return;
        }
        File infile = new File(args[0]);
        if (infile.exists() && infile.isFile() && infile.canRead()) {
            File outfile = new File(args[1]);
            if (!outfile.getParentFile().exists()) {
                outfile.getParentFile().mkdirs();
            }
            encodeModel(outfile);
            final int p0 = args[1].indexOf(".language-");
            final String version = args[1].substring(p0+10,args[1].length()-3);
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outfile), "UTF-8")) {
                stitch(infile, writer, version);
            } finally {
                ShaSigner.sign(outfile, new JsJULLogger(), true);
            }
        } else {
            System.err.println("Input file is invalid: " + infile);
            System.exit(2);
        }
    }
}
