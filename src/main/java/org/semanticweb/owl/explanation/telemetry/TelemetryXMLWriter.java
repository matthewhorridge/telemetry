package org.semanticweb.owl.explanation.telemetry;

import org.coode.string.EscapeUtils;
import org.coode.xml.XMLWriter;
import org.coode.xml.XMLWriterNamespaceManager;
import org.coode.xml.XMLWriterPreferences;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.*;

/**
 * Author: Matthew Horridge<br>
 * The University of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 31/01/2011
 */
public class TelemetryXMLWriter implements XMLWriter {

    private Stack<XMLElement> elementStack;

    protected Writer writer;

    private String encoding = "";

    private String xmlBase;

    private URI xmlBaseURI;

    private XMLWriterNamespaceManager xmlWriterNamespaceManager;

    private Map<String, String> entities;

    private static final int TEXT_CONTENT_WRAP_LIMIT = Integer.MAX_VALUE;

    private boolean preambleWritten;

    private static final String PERCENT_ENTITY = "&#37;";

    public TelemetryXMLWriter(Writer writer, XMLWriterNamespaceManager nsm, String xmlBase) {
        this.writer = writer;
        this.xmlWriterNamespaceManager = nsm;
        this.xmlBase = xmlBase;
        this.xmlBaseURI = URI.create(xmlBase);
        // no need to set it to UTF-8: it's supposed to be the default encoding for XML.
        //Must be set correctly for the Writer anyway, or bugs will ensue.
        //this.encoding = "UTF-8";
        elementStack = new Stack<XMLElement>();
        setupEntities();
    }


    private void setupEntities() {
        List<String> namespaces = new ArrayList<String>(xmlWriterNamespaceManager.getNamespaces());
        Collections.sort(namespaces, new StringLengthOnlyComparator());
        entities = new LinkedHashMap<String, String>();
        for (String curNamespace : namespaces) {
            String curPrefix = "";
            if (xmlWriterNamespaceManager.getDefaultNamespace().equals(curNamespace)) {
                curPrefix = xmlWriterNamespaceManager.getDefaultPrefix();
            }
            else {
                curPrefix = xmlWriterNamespaceManager.getPrefixForNamespace(curNamespace);

            }
            if (curPrefix.length() > 0) {
                entities.put(curNamespace, "&" + curPrefix + ";");
            }
        }
    }


    private String swapForEntity(String value) {
        for (String curEntity : entities.keySet()) {
            String entityVal = entities.get(curEntity);
            if (value.length() > curEntity.length()) {
                String repVal = value.replace(curEntity, entityVal);
                if (repVal.length() < value.length()) {
                    return repVal;
                }
            }
        }
        return value;
    }


    public String getDefaultNamespace() {
        return xmlWriterNamespaceManager.getDefaultNamespace();
    }


    public String getXMLBase() {
        return xmlBase;
    }

    public URI getXMLBaseAsURI() {
        return xmlBaseURI;
    }

    public XMLWriterNamespaceManager getNamespacePrefixes() {
        return xmlWriterNamespaceManager;
    }


    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    private boolean isValidQName(String name) {
        if(name == null) {
            return false;
        }
        int colonIndex = name.indexOf(":");
        boolean valid = false;
        if (colonIndex == -1) {
            valid = OWL2Datatype.XSD_NCNAME.getPattern().matcher(name).matches();
        }
        else {
            valid = OWL2Datatype.XSD_NCNAME.getPattern().matcher(name.substring(0, colonIndex - 1)).matches();
            if (valid) {
                valid = OWL2Datatype.XSD_NAME.getPattern().matcher(name.substring(colonIndex + 1)).matches();
            }
        }
        return valid;
    }


    public void setWrapAttributes(boolean b) {
        if (!elementStack.isEmpty()) {
            XMLElement element = elementStack.peek();
            element.setWrapAttributes(b);
        }
    }


    public void writeStartElement(String name) throws IOException {
//        String qName = xmlWriterNamespaceManager.getQName(name);
//        if ( qName == null || qName.equals(name)) {
//            if (!isValidQName(name)) {
//                // Could not generate a valid QName, therefore, we cannot
//                // write valid XML - just throw an exception!
//                throw new IllegalElementNameException(name);
//
//            }
//        }
        XMLElement element = new XMLElement(name, elementStack.size());
        if (!elementStack.isEmpty()) {
            XMLElement topElement = elementStack.peek();
            if (topElement != null) {
                topElement.writeElementStart(false);
            }
        }
        elementStack.push(element);
    }


    public void writeEndElement() throws IOException {
        // Pop the element off the stack and write it out
        if (!elementStack.isEmpty()) {
            XMLElement element = elementStack.pop();
            element.writeElementEnd();
        }
    }


    public void writeAttribute(String attr, String val) {
        XMLElement element = elementStack.peek();
        element.setAttribute(xmlWriterNamespaceManager.getQName(attr), val);
    }


    public void writeTextContent(String text) {
        XMLElement element = elementStack.peek();
        element.setText(text, false);
    }

    public void writeXMLContent(String text) {
        XMLElement element = elementStack.peek();
        element.setXMLContent(text);
    }

    public void writeCData(String data) {
        XMLElement element = elementStack.peek();
        element.setText(data, true);
    }


    public void writeComment(String commentText) throws IOException {
        XMLElement element = new XMLElement(null, elementStack.size());
        element.setText("<!-- " + commentText.replaceAll("--","&#45;&#45;") + " -->", false);
        if (!elementStack.isEmpty()) {
            XMLElement topElement = elementStack.peek();
            if (topElement != null) {
                topElement.writeElementStart(false);
            }
        }
        if (preambleWritten) {
            element.writeElementStart(true);
        }
        else {
            elementStack.push(element);
        }
    }


    private void writeEntities(String rootName) throws IOException {
        writer.write("\n\n<!DOCTYPE " + xmlWriterNamespaceManager.getQName(rootName) + " [\n");
        for (String entityVal : entities.keySet()) {
            String entity = entities.get(entityVal);
            entity = entity.substring(1, entity.length() - 1);
            writer.write("    <!ENTITY ");
            writer.write(entity);
            writer.write(" \"");
            entityVal = EscapeUtils.escapeXML(entityVal);
            entityVal = entityVal.replace("%", PERCENT_ENTITY);
            writer.write(entityVal);
            writer.write("\" >\n");
        }
        writer.write("]>\n\n\n");
    }


    public void startDocument(String rootElementName) throws IOException {
        String encodingString = "";
        if (encoding.length() > 0) {
            encodingString = " encoding=\"" + encoding + "\"";
        }
        writer.write("<?xml version=\"1.0\"" + encodingString + "?>\n");
        if (XMLWriterPreferences.getInstance().isUseNamespaceEntities()) {
            writeEntities(rootElementName);
        }
        preambleWritten = true;
        while (!elementStack.isEmpty()) {
            elementStack.pop().writeElementStart(true);
        }
        writeStartElement(rootElementName);
        setWrapAttributes(true);
        String defaultNamespace = xmlWriterNamespaceManager.getDefaultNamespace();
        if (defaultNamespace.length() > 0) {
            writeAttribute("xmlns", defaultNamespace);
        }
        if (xmlBase.length() != 0) {
            writeAttribute("xml:base", xmlBase);
        }
        for (String curPrefix : xmlWriterNamespaceManager.getPrefixes()) {
            if (curPrefix.length() > 0) {
                writeAttribute("xmlns:" + curPrefix, xmlWriterNamespaceManager.getNamespaceForPrefix(curPrefix));
            }
        }
    }


    public void endDocument() throws IOException {
        // Pop of each element
        while (!elementStack.isEmpty()) {
            writeEndElement();
        }
        writer.flush();
    }


    private static final class StringLengthOnlyComparator implements
			Comparator<String> {
		public int compare(String o1, String o2) {
		    // Shortest string first
		    return o1.length() - o2.length();
		}
	}


	public class XMLElement {

        private String name;

        private Map<String, String> attributes;

        String textContent;

        private boolean cdata = false;

        private boolean startWritten;

        private int indentation;

        private boolean wrapAttributes;

        private boolean escape = true;


        public XMLElement(String name) {
            this(name, 0);
            wrapAttributes = false;
        }


        public XMLElement(String name, int indentation) {
            this.name = name;
            attributes = new LinkedHashMap<String, String>();
            this.indentation = indentation;
            textContent = null;
            startWritten = false;
        }


        public void setWrapAttributes(boolean b) {
        	//XXX it was:
            //wrapAttributes = true;
        	wrapAttributes = b;
        }


        public void setAttribute(String attribute, String value) {
            attributes.put(attribute, value);
        }


        public void setText(String content, boolean cdata) {
            textContent = content;
            this.cdata = cdata;
        }

        public void setXMLContent(String content) {
            textContent = content;
            this.escape = false;
        }


        public void writeElementStart(boolean close) throws IOException {
            if (!startWritten) {
                startWritten = true;
                insertIndentation();
                if (name != null) {
                    writer.write('<');
                    writer.write(name);
                    writeAttributes();
                    if (textContent != null) {
                        boolean wrap = textContent.length() > TEXT_CONTENT_WRAP_LIMIT;
                        if (wrap) {
                            writeNewLine();
                            indentation++;
                            insertIndentation();
                        }
                        writer.write('>');
                        writeTextContent();
                        if (wrap) {
                            indentation--;
                        }
                    }
                    if (close) {
                        if (textContent != null) {
                            writeElementEnd();
                        }
                        else {
                            writer.write("/>");
                            writeNewLine();
                        }
                    }
                    else {
                        if (textContent == null) {
                            writer.write('>');
                            writeNewLine();
                        }
                    }
                }
                else {
                    // Name is null so by convension this is a comment
                    if (textContent != null) {
                        writer.write("\n\n\n");
                        StringTokenizer tokenizer = new StringTokenizer(textContent, "\n", true);
                        while (tokenizer.hasMoreTokens()) {
                            String token = tokenizer.nextToken();
                            if (!token.equals("\n")) {
                                insertIndentation();
                            }
                            writer.write(token);
                        }
                        writer.write("\n\n");
                    }
                }
            }
        }


        public void writeElementEnd() throws IOException {
            if (name != null) {
                if (!startWritten) {
                    writeElementStart(true);
                }
                else {
                    if (textContent == null) {
                        insertIndentation();
                    }
                    writer.write("</");
                    writer.write(name);
                    writer.write(">");
                    writeNewLine();
                }
            }
        }


        private void writeAttribute(String attr, String val) throws IOException {
            writer.write(attr);
            writer.write('=');
            writer.write('"');
            if (XMLWriterPreferences.getInstance().isUseNamespaceEntities()) {
                writer.write(swapForEntity(EscapeUtils.escapeXML(val)));
            }
            else {
                writer.write(EscapeUtils.escapeXML(val));
            }
            writer.write('"');
        }


        private void writeAttributes() throws IOException {
            for (Iterator<String> it = attributes.keySet().iterator(); it.hasNext();) {
                String attr = it.next();
                String val = attributes.get(attr);
                writer.write(' ');
                writeAttribute(attr, val);
                if (it.hasNext() && wrapAttributes) {
                    writer.write("\n");
                    indentation++;
                    insertIndentation();
                    indentation--;
                }
            }
        }


        private void writeTextContent() throws IOException {
            if (textContent != null) {
                if (!cdata) {
                    if (escape) {
                        writer.write(EscapeUtils.escapeXML(textContent));
                    }
                    else {
                        writer.write(textContent);
                    }
                }
                else {
                    writer.write("<![CDATA[");
                    writer.write(textContent);
                    writer.write("]]>");
                }
            }
        }


        private void insertIndentation() throws IOException {
            if (XMLWriterPreferences.getInstance().isIndenting()) {
                for (int i = 0; i < indentation * XMLWriterPreferences.getInstance().getIndentSize(); i++) {
                    writer.write(' ');
                }
            }
        }


        private void writeNewLine() throws IOException {
            writer.write('\n');
        }
    }


}
