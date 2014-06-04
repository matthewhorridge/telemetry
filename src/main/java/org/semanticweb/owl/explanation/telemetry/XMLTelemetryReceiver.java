package org.semanticweb.owl.explanation.telemetry;

import org.coode.owlapi.owlxml.renderer.OWLXMLObjectRenderer;
import org.coode.owlapi.owlxml.renderer.OWLXMLWriter;
import org.coode.xml.XMLWriterNamespaceManager;
import org.semanticweb.owlapi.model.OWLAxiom;

import java.io.*;
import java.util.*;

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
        try {
            XMLWriterNamespaceManager nsm = new XMLWriterNamespaceManager("");
            this.baseWriter = writer;
            xmlWriter = new TelemetryXMLWriter(baseWriter, nsm, "");
            xmlWriter.startDocument("experiments");
            depth++;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    xmlWriter.endDocument();
                    baseWriter.close();
                }
                catch (IOException e) {
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

    public void beginTransmission(TelemetryInfo info) {
        List<TelemetryTimer> timers = pauseRunningTimers();
        try {
            telemetryNodeStack.push(info);
            boolean ignore = false;
            if(!ignoreNodeStack.isEmpty()) {
                ignore = ignoreNodeStack.peek();
            }
            if(!ignore) {
                ignore = ignoredNodeNames.contains(info.getName());
            }
            ignoreNodeStack.push(ignore);
            if (!ignore) {
                xmlWriter.writeStartElement(info.getName());
            }
            depth++;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            unpauseTimers(timers);
        }
    }

    public void recordMeasurement(TelemetryInfo info, String propertyName, String value) {
        if (propertyName != null && value != null && !isIgnoredTransmission()) {
            List<TelemetryTimer> timers = pauseRunningTimers();
            try {
                xmlWriter.writeStartElement("measurement");
                xmlWriter.writeAttribute("name", propertyName);
                xmlWriter.writeAttribute("value", value);
                xmlWriter.writeEndElement();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                unpauseTimers(timers);
            }
        }
    }

    private boolean isIgnoredTransmission() {
        return !ignoreNodeStack.isEmpty() && ignoreNodeStack.peek();
    }

    public void recordException(TelemetryInfo info, Throwable exception) {
        List<TelemetryTimer> pausedTimers = pauseRunningTimers();
        try {
            xmlWriter.writeStartElement("exception");

            xmlWriter.writeStartElement("class");
            xmlWriter.writeTextContent(exception.getClass().getName());
            xmlWriter.writeEndElement();

            xmlWriter.writeStartElement("message");
            xmlWriter.writeTextContent(exception.getMessage());
            xmlWriter.writeEndElement();

            xmlWriter.writeStartElement("stacktrace");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            pw.flush();
            xmlWriter.writeTextContent(sw.getBuffer().toString());
            xmlWriter.writeEndElement();

            xmlWriter.writeEndElement();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            unpauseTimers(pausedTimers);
        }
    }

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
                xmlWriter.writeStartElement("object");
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
                else if(object instanceof OWLAxiom) {
                    OWLAxiom ax = (OWLAxiom) object;
                    OutputStreamWriter osw = new OutputStreamWriter(bos);
                    OWLXMLWriter writer = new OWLXMLWriter(osw, null);
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
                else if(writeAsXML) {
                    xmlWriter.writeXMLContent(bos.toString());
                }
                else {
                    xmlWriter.writeTextContent(bos.toString());
                }
                xmlWriter.writeEndElement();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void recordTiming(TelemetryInfo info, String name, TelemetryTimer telemetryTimer) {
        if (!isIgnoredTransmission()) {
            recordMeasurement(info, name, Long.toString(telemetryTimer.getEllapsedTime()));
        }
    }

    public void endTransmission(TelemetryInfo info) {
        List<TelemetryTimer> timers = pauseRunningTimers();
        try {
            if (!isIgnoredTransmission()) {
                xmlWriter.writeEndElement();
            }
            if (!telemetryNodeStack.isEmpty()) {
                telemetryNodeStack.pop();
            }
            if(!ignoreNodeStack.isEmpty()) {
                ignoreNodeStack.pop();
            }
            depth--;
            if (depth == 0) {
                xmlWriter.endDocument();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
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
