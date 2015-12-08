package org.semanticweb.owl.explanation.telemetry;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.owlxml.renderer.OWLXMLObjectRenderer;
import org.semanticweb.owlapi.owlxml.renderer.OWLXMLWriter;
import org.semanticweb.owlapi.rdf.rdfxml.renderer.XMLWriterNamespaceManager;

/**
 * Author: Matthew Horridge<br>
 * The University of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 31/01/2011
 */
public class XMLTelemetryReceiver implements TelemetryReceiver {

    private TelemetryXMLWriter xmlWriter;

    private Stack<TelemetryInfo> telemetryNodeStack = new Stack<TelemetryInfo>();

    private Stack<Boolean> ignoreNodeStack = new Stack<Boolean>();

    private Set<String> ignoredNodeNames = new HashSet<String>();

    private int depth = 0;

    private Writer baseWriter;

    public XMLTelemetryReceiver() {
        this(getNextFile("telemetry-", ".xml"));
    }

    private static File getNextFile(String prefix, String suffix) {
        File outputFile;
        for (int i = 0; ; i++) {
            outputFile = new File(prefix + i + suffix);
            if (!outputFile.exists()) {
                break;
            }
        }
        return outputFile;
    }

    public XMLTelemetryReceiver(File outputFile) {
        this(getWriterForFile(outputFile));
    }

    private static BufferedWriter getWriterForFile(File outputFile) {
        try {
            return new BufferedWriter(new FileWriter(outputFile), 10 * 1024 * 1024);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public XMLTelemetryReceiver(Writer writer) {
        XMLWriterNamespaceManager nsm = new XMLWriterNamespaceManager("");
        baseWriter = writer;
        xmlWriter = new TelemetryXMLWriter(baseWriter, nsm, "");
        xmlWriter.startDocument(IRI.create("experiments"));
        depth++;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    xmlWriter.endDocument();
                    baseWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void addIgnoreName(String name) {
        ignoredNodeNames.add(name);
    }

    public void closeOpenTransmissions() {
        while (!telemetryNodeStack.isEmpty()) {
            TelemetryInfo info = telemetryNodeStack.peek();
            endTransmission(info);
        }
    }

    @Override
    public void beginTransmission(TelemetryInfo info) {
        List<TelemetryTimer> timers = pauseRunningTimers();
        try {
            telemetryNodeStack.push(info);
            boolean ignore = false;
            if (!ignoreNodeStack.isEmpty()) {
                ignore = ignoreNodeStack.peek();
            }
            if (!ignore) {
                ignore = ignoredNodeNames.contains(info.getName());
            }
            ignoreNodeStack.push(ignore);
            if (!ignore) {
                xmlWriter.writeStartElement(IRI.create(info.getName()));
            }
            depth++;
        } finally {
            unpauseTimers(timers);
        }
    }

    @Override
    public void recordMeasurement(TelemetryInfo info, String propertyName, String value) {
        if (propertyName != null && value != null && !isIgnoredTransmission()) {
            List<TelemetryTimer> timers = pauseRunningTimers();
            try {
                xmlWriter.writeStartElement(IRI.create("measurement"));
                xmlWriter.writeAttribute("name", propertyName);
                xmlWriter.writeAttribute("value", value);
                xmlWriter.writeEndElement();
            } finally {
                unpauseTimers(timers);
            }
        }
    }

    private boolean isIgnoredTransmission() {
        return !ignoreNodeStack.isEmpty() && ignoreNodeStack.peek();
    }

    @Override
    public void recordException(TelemetryInfo info, Throwable exception) {
        List<TelemetryTimer> pausedTimers = pauseRunningTimers();
        try {
            xmlWriter.writeStartElement(IRI.create("exception"));

            xmlWriter.writeStartElement(IRI.create("class"));
            xmlWriter.writeTextContent(exception.getClass().getName());
            xmlWriter.writeEndElement();

            xmlWriter.writeStartElement(IRI.create("message"));
            xmlWriter.writeTextContent(exception.getMessage());
            xmlWriter.writeEndElement();

            xmlWriter.writeStartElement(IRI.create("stacktrace"));
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            pw.flush();
            xmlWriter.writeTextContent(sw.getBuffer().toString());
            xmlWriter.writeEndElement();

            xmlWriter.writeEndElement();
        } finally {
            unpauseTimers(pausedTimers);
        }
    }

    @Override
    public void recordObject(TelemetryInfo info, String namePrefix, String nameSuffix, Object object) {
        if (!isIgnoredTransmission()) {
            List<TelemetryTimer> timers = pauseRunningTimers();
            serialiseObject(info, namePrefix, object);
            unpauseTimers(timers);
        }
    }

    private void serialiseObject(TelemetryInfo info, String namePrefix, Object object) {
        if (!isIgnoredTransmission()) {
            try {
                boolean writeAsXML = false;
                xmlWriter.writeStartElement(IRI.create("object"));
                xmlWriter.writeAttribute("name", namePrefix);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                boolean wrapInCDataSection = true;
                if (object instanceof TelemetryObject) {
                    TelemetryObject telemetryObject = (TelemetryObject) object;
                    telemetryObject.serialise(bos);
                    if (!telemetryObject.isSerialisedAsXML()) {
                        wrapInCDataSection = false;
                    }
                    else {
                        wrapInCDataSection = false;
                        writeAsXML = true;
                    }
                }
                else if (object instanceof OWLAxiom) {
                    OWLAxiom ax = (OWLAxiom) object;
                    OutputStreamWriter osw = new OutputStreamWriter(bos);
                    PrintWriter printWriter = new PrintWriter(osw);
                    OWLXMLWriter writer = new OWLXMLWriter(printWriter, null);
                    OWLXMLObjectRenderer renderer = new OWLXMLObjectRenderer(writer);
                    ax.accept(renderer);
                    osw.flush();
                    wrapInCDataSection = false;
                    writeAsXML = true;

                }
                else {
                    OutputStreamWriter writer = new OutputStreamWriter(bos);
                    String string = object.toString();
                    writer.write(string);
                    writer.flush();
                }
                if (wrapInCDataSection) {
                    xmlWriter.writeCData(bos.toString());
                }
                else if (writeAsXML) {
                    xmlWriter.writeXMLContent(bos.toString());
                }
                else {
                    xmlWriter.writeTextContent(bos.toString());
                }
                xmlWriter.writeEndElement();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void recordTiming(TelemetryInfo info, String name, TelemetryTimer telemetryTimer) {
        if (!isIgnoredTransmission()) {
            recordMeasurement(info, name, Long.toString(telemetryTimer.getEllapsedTime()));
        }
    }

    @Override
    public void endTransmission(TelemetryInfo info) {
        List<TelemetryTimer> timers = pauseRunningTimers();
        try {
            if (!isIgnoredTransmission()) {
                xmlWriter.writeEndElement();
            }
            if (!telemetryNodeStack.isEmpty()) {
                telemetryNodeStack.pop();
            }
            if (!ignoreNodeStack.isEmpty()) {
                ignoreNodeStack.pop();
            }
            depth--;
            if (depth == 0) {
                xmlWriter.endDocument();
            }
        } finally {
            unpauseTimers(timers);
        }
    }

    private void unpauseTimers(List<TelemetryTimer> paused) {
        for (TelemetryTimer timer : paused) {
            timer.start();
        }
    }

    private List<TelemetryTimer> pauseRunningTimers() {
        List<TelemetryTimer> paused = new ArrayList<TelemetryTimer>();
        for (TelemetryInfo i : telemetryNodeStack) {
            for (TelemetryTimer timer : i.getTimers()) {
                if (timer != null) {
                    if (timer.isRunning()) {
                        timer.stop();
                        paused.add(timer);
                    }
                }
            }
        }
        return paused;
    }


}
