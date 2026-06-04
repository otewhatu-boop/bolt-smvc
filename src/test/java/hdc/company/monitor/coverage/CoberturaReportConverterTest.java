package hdc.company.monitor.coverage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CoberturaReportConverterTest {

    @Test
    void shouldConvertJacocoXmlToCoberturaXml(@TempDir Path tempDir) throws Exception {
        Path jacocoXml = tempDir.resolve("jacoco.xml");
        Path coberturaXml = tempDir.resolve("coverage.xml");

        String xml = "<?xml version='1.0' encoding='UTF-8'?><report name='JaCoCo Coverage Report'><package name='hdc/company/monitor/util'><sourcefile name='FaviconGenerator.java'><line nr='1' mi='0' ci='1' mb='0' cb='0'/></sourcefile></package></report>";
        Files.writeString(jacocoXml, xml);

        CoberturaReportConverter.main(new String[]{jacocoXml.toString(), coberturaXml.toString()});

        assertTrue(Files.exists(coberturaXml), "Cobertura XML output should exist");
        Document document = parseXml(coberturaXml);

        NodeList packageNodes = document.getElementsByTagName("package");
        assertEquals(1, packageNodes.getLength(), "There should be exactly one package element");

        Element packageElement = (Element) packageNodes.item(0);
        assertEquals("hdc/company/monitor/util", packageElement.getAttribute("name"), "Output should preserve package element");
    }

    @Test
    void shouldHandleMissingJacocoFile(@TempDir Path tempDir) throws Exception {
        Path missingJacoco = tempDir.resolve("non-existent.xml");
        Path coberturaXml = tempDir.resolve("coverage.xml");

        // Should not throw exception or exit with error
        CoberturaReportConverter.main(new String[]{missingJacoco.toString(), coberturaXml.toString()});

        assertFalse(Files.exists(coberturaXml), "Cobertura XML should not be created for missing input");
    }

    @Test
    void shouldReportBranchCoverageAndLineRates(@TempDir Path tempDir) throws Exception {
        Path jacocoXml = tempDir.resolve("jacoco.xml");
        Path coberturaXml = tempDir.resolve("coverage.xml");

        String xml = "<?xml version='1.0' encoding='UTF-8'?><report name='JaCoCo Coverage Report'>"
                + "<package name='hdc/company/monitor/util'>"
                + "<sourcefile name='FaviconGenerator.java'>"
                + "<line nr='1' mi='0' ci='0' mb='1' cb='0'/>"
                + "<line nr='2' mi='0' ci='1' mb='1' cb='1'/>"
                + "</sourcefile>"
                + "</package>"
                + "</report>";
        Files.writeString(jacocoXml, xml);

        CoberturaReportConverter.main(new String[]{jacocoXml.toString(), coberturaXml.toString()});

        Document document = parseXml(coberturaXml);
        Element coverage = (Element) document.getElementsByTagName("coverage").item(0);

        assertEquals("0.50", coverage.getAttribute("line-rate"), "Line rate should reflect covered lines");
        assertEquals("0.33", coverage.getAttribute("branch-rate"), "Branch rate should reflect covered branches");

        NodeList lineNodes = document.getElementsByTagName("line");
        assertEquals(2, lineNodes.getLength(), "There should be two line elements");

        Element branchLine = (Element) lineNodes.item(1);
        assertEquals("true", branchLine.getAttribute("branch"), "Line with branch data should be marked as branch");
        assertEquals("50% (1/2)", branchLine.getAttribute("condition-coverage"), "Branch condition coverage should be reported");
    }

    private Document parseXml(Path path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory.newDocumentBuilder().parse(Files.newInputStream(path));
    }
}
