package br.usp.each.saeg.jaguar.core.output;

import br.usp.each.saeg.badua.core.analysis.ClassCoverage;
import br.usp.each.saeg.badua.core.analysis.MethodCoverage;
import br.usp.each.saeg.badua.core.analysis.SourceLineDefUseChain;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Mario Concilio
 */
public class Spectra extends CoverageFile {

    private Set<String> classes = new HashSet<>();
    private BufferedWriter writer;

    public Spectra(File path) {
        super(path, "spectra");
    }

    public void addClass(ClassCoverage clazz) throws IOException {
        if (this.classes.contains(clazz.getName())) {
            this.writer = null;
        }
        else {
            String fileName = fileNameForClass(clazz.getName());
            this.writer = createWriter(fileName);
            this.classes.add(clazz.getName());
        }
    }

    public void addDua(ClassCoverage clazz, MethodCoverage method, SourceLineDefUseChain dua) throws IOException {
        if (this.writer != null) {
            this.writer.append(classFullName(clazz.getName()))
                    .append("#")
                    .append(method.getName())
                    .append(":")
                    .append(duaToString(dua));

            this.writer.newLine();
        }
    }

    public void next() throws IOException {
        if (this.writer != null) {
            this.writer.close();
        }
    }

    private StringBuilder duaToString(SourceLineDefUseChain dua) {
        final StringBuilder str = new StringBuilder();
        str.append("(")
                .append(dua.def)
                .append(",");

        if (isPUseDua(dua)) {
            str.append("(")
                    .append(dua.use)
                    .append(",")
                    .append(dua.target)
                    .append("),");
        }
        else {
            str.append(dua.use)
                    .append(",");
        }

        str.append(" ")
                .append(dua.var)
                .append(")");

        return str;
    }

    private boolean isPUseDua(SourceLineDefUseChain dua) {
        return dua.target > 0;
    }

}
