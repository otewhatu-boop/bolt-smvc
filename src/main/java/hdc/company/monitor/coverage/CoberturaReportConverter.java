package hdc.company.monitor.coverage;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Locale;

public class CoberturaReportConverter {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: CoberturaReportConverter <jacoco.xml> <coverage.xml>");
            System.exit(1);
        }
        Path jacocoFile = Path.of(args[0]);
        Path coberturaFile = Path.of(args[1]);

        if (!Files.exists(jacocoFile)) {
            System.out.println("Missing JaCoCo report: " + jacocoFile + ". Skipping Cobertura conversion.");
            return;
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> new org.xml.sax.InputSource(new java.io.StringReader("")));
        Document sourceDoc = builder.parse(Files.newInputStream(jacocoFile));
        Document targetDoc = builder.newDocument();

        Element coverage = targetDoc.createElement("coverage");
        targetDoc.appendChild(coverage);

        long linesCovered = 0;
        long linesValid = 0;
        long branchesCovered = 0;
        long branchesValid = 0;

        Element sources = targetDoc.createElement("sources");
        Element source = targetDoc.createElement("source");
        source.setTextContent("src/main/java");
        sources.appendChild(source);
        coverage.appendChild(sources);

        Element packagesElement = targetDoc.createElement("packages");
        coverage.appendChild(packagesElement);

        NodeList packageNodes = sourceDoc.getElementsByTagName("package");
        for (int p = 0; p < packageNodes.getLength(); p++) {
            Element packageNode = (Element) packageNodes.item(p);
            String packageName = packageNode.getAttribute("name");
            Element packageElement = targetDoc.createElement("package");
            packageElement.setAttribute("name", packageName);

            long packageLinesCovered = 0;
            long packageLinesValid = 0;
            long packageBranchesCovered = 0;
            long packageBranchesValid = 0;

            Element classesElement = targetDoc.createElement("classes");
            NodeList sourceFiles = packageNode.getElementsByTagName("sourcefile");
            for (int s = 0; s < sourceFiles.getLength(); s++) {
                Element sourceFile = (Element) sourceFiles.item(s);
                String sourceFileName = sourceFile.getAttribute("name");
                String className = packageName.replace('/', '.');
                if (!className.isEmpty()) {
                    className += "." + sourceFileName.replace(".java", "");
                } else {
                    className = sourceFileName.replace(".java", "");
                }

                Element classElement = targetDoc.createElement("class");
                classElement.setAttribute("name", className);
                classElement.setAttribute("filename", packageName.isEmpty() ? sourceFileName : packageName + "/" + sourceFileName);

                long classLinesCovered = 0;
                long classLinesValid = 0;
                long classBranchesCovered = 0;
                long classBranchesValid = 0;

                Element linesElement = targetDoc.createElement("lines");
                NodeList lineNodes = sourceFile.getElementsByTagName("line");
                for (int l = 0; l < lineNodes.getLength(); l++) {
                    Element lineNode = (Element) lineNodes.item(l);
                    int lineNumber = Integer.parseInt(lineNode.getAttribute("nr"));
                    int coveredInstructions = Integer.parseInt(lineNode.getAttribute("ci"));
                    int missedBranches = Integer.parseInt(lineNode.getAttribute("mb"));
                    int coveredBranches = Integer.parseInt(lineNode.getAttribute("cb"));

                    boolean branch = missedBranches + coveredBranches > 0;
                    int hits = coveredInstructions > 0 ? 1 : 0;

                    Element lineElement = targetDoc.createElement("line");
                    lineElement.setAttribute("number", String.valueOf(lineNumber));
                    lineElement.setAttribute("hits", String.valueOf(hits));
                    lineElement.setAttribute("branch", Boolean.toString(branch));
                    if (branch) {
                        lineElement.setAttribute("condition-coverage", String.format(Locale.US, "%d%% (%d/%d)",
                                coveredBranches * 100 / Math.max(1, coveredBranches + missedBranches),
                                coveredBranches,
                                coveredBranches + missedBranches));
                    }

                    linesElement.appendChild(lineElement);

                    packageLinesValid++;
                    classLinesValid++;
                    if (hits > 0) {
                        packageLinesCovered++;
                        classLinesCovered++;
                    }
                    if (branch) {
                        packageBranchesValid += missedBranches + coveredBranches;
                        classBranchesValid += missedBranches + coveredBranches;
                        packageBranchesCovered += coveredBranches;
                        classBranchesCovered += coveredBranches;
                    }
                }

                classElement.setAttribute("line-rate", formatRate(classLinesCovered, classLinesValid));
                classElement.setAttribute("branch-rate", formatRate(classBranchesCovered, classBranchesValid));
                classElement.appendChild(linesElement);
                classesElement.appendChild(classElement);

                packageLinesCovered += classLinesCovered;
                packageLinesValid += classLinesValid;
                packageBranchesCovered += classBranchesCovered;
                packageBranchesValid += classBranchesValid;
            }

            packageElement.setAttribute("line-rate", formatRate(packageLinesCovered, packageLinesValid));
            packageElement.setAttribute("branch-rate", formatRate(packageBranchesCovered, packageBranchesValid));
            packageElement.appendChild(classesElement);
            packagesElement.appendChild(packageElement);

            linesCovered += packageLinesCovered;
            linesValid += packageLinesValid;
            branchesCovered += packageBranchesCovered;
            branchesValid += packageBranchesValid;
        }

        coverage.setAttribute("line-rate", formatRate(linesCovered, linesValid));
        coverage.setAttribute("branch-rate", formatRate(branchesCovered, branchesValid));
        coverage.setAttribute("lines-covered", String.valueOf(linesCovered));
        coverage.setAttribute("lines-valid", String.valueOf(linesValid));
        coverage.setAttribute("branches-covered", String.valueOf(branchesCovered));
        coverage.setAttribute("branches-valid", String.valueOf(branchesValid));
        coverage.setAttribute("complexity", "0");
        coverage.setAttribute("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        coverage.setAttribute("version", "JaCoCo 0.8.14");

        Files.createDirectories(coberturaFile.getParent());
        writeDocument(targetDoc, coberturaFile);
    }

    private static String formatRate(long covered, long valid) {
        if (valid == 0) {
            return "0";
        }
        return new DecimalFormat("0.00").format((double) covered / valid);
    }

    private static void writeDocument(Document document, Path outputFile) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        try (var writer = Files.newBufferedWriter(outputFile)) {
            transformer.transform(new DOMSource(document), new StreamResult(writer));
        }
    }
}
