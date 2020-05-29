package br.usp.each.saeg.jaguar.core.output;

import br.usp.each.saeg.badua.core.analysis.ClassCoverage;
import br.usp.each.saeg.badua.core.analysis.SourceLineDefUseChain;
import br.usp.each.saeg.jaguar.core.analysis.DuaCoverageBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * @author Mario Concilio
 */
public class Matrix extends CoverageFile {

    private boolean currentTestFailed;
    private BufferedWriter writer;

    public Matrix(File path) {
        super(path, "matrix");
    }

    public void isTestFailed(boolean currentTestFailed) {
        this.currentTestFailed = currentTestFailed;
    }

    public void addClass(ClassCoverage clazz) throws IOException {
        String fileName = fileNameForClass(clazz.getName());
        this.writer = createWriter(fileName);
    }

    public void addDua(SourceLineDefUseChain dua) throws IOException {
        if (dua.covered) {
            this.writer.append("1 ");
        }
        else {
            this.writer.append("0 ");
        }
    }

    public void next() throws IOException {
        this.writer.append(currentTestFailed ? "-" : "+");
        this.writer.newLine();
        this.writer.close();
    }

    public void remainingClassesCoverage(Set<String> allClasses, DuaCoverageBuilder coverageVisitor) {
        allClasses.stream()
                .filter(className -> !coverageVisitor.getClassNames().contains(className))
                .map(this::fileNameForClass)
                .forEach(this::writeEmptyLines);
    }

    private void writeEmptyLines(String fileName) {
        try {
            final BufferedWriter writer = createWriter(fileName);
            writer.append("=0 ");
            writer.append(currentTestFailed ? "-" : "+");
            writer.newLine();
            writer.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
