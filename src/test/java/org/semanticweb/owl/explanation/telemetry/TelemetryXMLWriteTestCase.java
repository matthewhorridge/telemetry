package org.semanticweb.owl.explanation.telemetry;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.rdf.rdfxml.renderer.XMLWriterNamespaceManager;


/**
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics Research Group, Date: 04/06/2014
 */
public class TelemetryXMLWriteTestCase {

    private StringWriter baseWriter;
    private XMLWriterNamespaceManager nsm;
    private TelemetryXMLWriter writer;

    @Before
    public void setUp() throws Exception {
        baseWriter = new StringWriter();
        nsm = new XMLWriterNamespaceManager("http://base.com/stuff/");
        writer = new TelemetryXMLWriter(baseWriter, nsm, "http://base.com/stuff");
    }

    @Test
    public void shouldWriteElement() throws IOException{
        writer.writeStartElement(IRI.create("experiment"));
        writer.writeEndElement();
        assertThat(baseWriter.toString().trim(), is("<experiment/>"));
    }

    @Test
    public void shouldWriteAttribute() throws IOException {
        writer.writeStartElement(IRI.create("experiment"));
        writer.writeAttribute("name", "ExperimentName");
        writer.writeEndElement();
        assertThat(baseWriter.toString().trim(), is("<experiment name=\"ExperimentName\"/>"));
    }
}
