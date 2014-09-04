package com.redhat.ceylon.compiler.js;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.redhat.ceylon.compiler.loader.ModelEncoder;
import com.redhat.ceylon.compiler.typechecker.model.Module;

/** A container for things we need to keep per-module. */
public class JsOutput {
    private File outfile;
    private File modfile;
    private Writer writer;
    private final Module module;
    private final Set<File> s = new HashSet<File>();
    final Map<String,String> requires = new HashMap<String,String>();
    final MetamodelVisitor mmg;
    final String encoding;

    protected JsOutput(Module m, String encoding) throws IOException {
        this.encoding = encoding == null ? "UTF-8" : encoding;
        module = m;
        mmg = new MetamodelVisitor(m);
    }
    protected Writer getWriter() throws IOException {
        if (writer == null) {
            outfile = File.createTempFile("jsout", ".tmp");
            writer = new OutputStreamWriter(new FileOutputStream(outfile), encoding);
        }
        return writer;
    }
    protected File close() throws IOException {
        if (writer != null) {
            writer.close();
        }
        return outfile;
    }
    File getModelFile() {
        return modfile;
    }

    void addSource(File src) {
        s.add(src);
    }
    Set<File> getSources() { return s; }

    public void encodeModel() throws IOException {
        if (modfile == null) {
            modfile = File.createTempFile("jsmod", ".tmp");
            try (OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(modfile), encoding)) {
                JsCompiler.beginWrapper(fw);
                fw.write("ex$.$CCMM$=");
                ModelEncoder.encodeModel(mmg.getModel(), fw);
                fw.write(";\n");
                JsCompiler.endWrapper(fw);
            } finally {
            }
            writer.write("\nvar _CTM$;function $CCMM$(){if (_CTM$===undefined)_CTM$=require('");
            writer.write(GenerateJsVisitor.scriptPath(module));
            writer.write("-model");
            writer.write("').$CCMM$;return _CTM$;}\n");
            writer.write("ex$.$CCMM$=$CCMM$;\n");
        }
    }

    public void outputFile(File f) {
        try(BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line = null;
            while ((line = r.readLine()) != null) {
                final String c = line.trim();
                if (!c.isEmpty()) {
                    getWriter().write(c);
                    getWriter().write('\n');
                }
            }
        } catch(IOException ex) {
            throw new CompilerErrorException("Reading from " + f);
        }
    }

}