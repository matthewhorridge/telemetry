package org.semanticweb.owl.explanation.telemetry;


/**
 * Author: Matthew Horridge<br>
 * The University of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 05/02/2011
 */
public interface TelemetryObjectXMLWriter {

    void startElement(String name);

    void writeAttribute(String attributeName, String value);

    void writeTextContent(String textContent);

    void writeCDataSection(String cdataContent);

    void endElement();
}
