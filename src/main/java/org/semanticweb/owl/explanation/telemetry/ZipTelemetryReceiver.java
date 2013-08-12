package org.semanticweb.owl.explanation.telemetry;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Author: Matthew Horridge<br>
 * The University of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 07/01/2011
 */
public class ZipTelemetryReceiver implements TelemetryReceiver {

    private static final String ROOT_NAME = "telemetry/";

    private Map<TelemetryInfo, Properties> info2PropertiesMap = new WeakHashMap<TelemetryInfo, Properties>();

    private Map<TelemetryInfo, String> info2EntryMap = new WeakHashMap<TelemetryInfo, String>();

    private Stack<TelemetryInfo> telemetryInfoStack = new Stack<TelemetryInfo>();

    private Set<String> zipEntryNames = new HashSet<String>();

    private File zip;

    private ZipOutputStream zipOutputStream;

    public ZipTelemetryReceiver(File zipFile) {
        try {
            this.zip = zipFile;
            this.zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
            zipOutputStream.putNextEntry(new ZipEntry(ROOT_NAME));
            zipOutputStream.closeEntry();
            zipOutputStream.setLevel(9);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    zipOutputStream.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void close() {
        try {
            zipOutputStream.flush();
            zipOutputStream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void beginTransmission(TelemetryInfo info) {
        createTelemetryInfoEntry(info);
    }

    public void recordMeasurement(TelemetryInfo info, String propertyName, String value) {
        writeProperty(info, propertyName, value);
    }

    public void recordException(TelemetryInfo info, Throwable exception) {
    }

    public void recordObject(TelemetryInfo info, String namePrefix, String nameSuffix, Object object) {
        writeObject(info, namePrefix + nameSuffix, object);
    }

    public void recordTiming(TelemetryInfo info, String name, TelemetryTimer telemetryTimer) {
        long ellapsedTime = telemetryTimer.getEllapsedTime();
        recordMeasurement(info, name, Long.toString(ellapsedTime));
    }

    public void endTransmission(TelemetryInfo info) {
        TelemetryInfo popped = telemetryInfoStack.pop();
        writeTelemetryInfoProperties(info);
        try {
            zipOutputStream.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if(!popped.equals(info)) {
            System.err.println("ERROR: TelemetryInfo mismatch: " + info + " " + popped);
        }

    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void createTelemetryInfoEntry(TelemetryInfo info) {
        String infoEntry = createNumberedZipEntry(info);
        ZipEntry ze = new ZipEntry(infoEntry);
        try {
            zipOutputStream.putNextEntry(ze);
            zipOutputStream.closeEntry();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        telemetryInfoStack.push(info);
        info2EntryMap.put(info, infoEntry);
        info2PropertiesMap.put(info, new Properties());
    }


    private String createNumberedZipEntry(TelemetryInfo info) {
        String parentEntry;
        if (!telemetryInfoStack.isEmpty()) {
            TelemetryInfo parentInfo = telemetryInfoStack.peek();
            parentEntry = getTelemetryInfoZipEntryName(parentInfo);
        }
        else {
            parentEntry = ROOT_NAME;
        }
        int count = 0;
        while(true) {
            StringBuilder sb = new StringBuilder();
            sb.append(parentEntry);
            sb.append(info.getName());
            sb.append(".");
            sb.append(count);
            sb.append("/");
            String candidate = sb.toString();
            if(!zipEntryNames.contains(candidate)) {
                zipEntryNames.add(candidate);
                return candidate;
            }
            count++;
        }
    }

    private String getTelemetryInfoZipEntryName(TelemetryInfo info) {
        return info2EntryMap.get(info);
    }

    private String getTelemetryInfoArtefactZipEntryName(TelemetryInfo info, String artefactName) {
        StringBuilder sb = new StringBuilder();
        sb.append(getTelemetryInfoZipEntryName(info));
        sb.append(artefactName);
        return sb.toString();
    }

    public ZipEntry getPropertiesZipEntryName(TelemetryInfo info) {
        String zipEntryName = getTelemetryInfoArtefactZipEntryName(info, info.getName() + ".properties");
        return new ZipEntry(zipEntryName);
    }


    private void writeProperty(TelemetryInfo info, String propertyName, String value) {
        List<TelemetryTimer> paused = pauseRunningTimers();
        Properties properties = info2PropertiesMap.get(info);
        if(properties != null) {
            properties.setProperty(propertyName, value);
        }
        unpauseTimers(paused);
    }

    private void writeTelemetryInfoProperties(TelemetryInfo info) {
        Properties properties = info2PropertiesMap.get(info);
        try {
            ZipEntry propertiesZipEntry = getPropertiesZipEntryName(info);
            zipOutputStream.putNextEntry(propertiesZipEntry);
            BufferedOutputStream bos = new BufferedOutputStream(zipOutputStream);
            properties.store(bos, null);
            bos.flush();
            zipOutputStream.closeEntry();
            zipOutputStream.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void unpauseTimers(List<TelemetryTimer> paused) {
        for(TelemetryTimer timer : paused) {
            timer.start();
        }
    }

    private List<TelemetryTimer> pauseRunningTimers() {
        List<TelemetryTimer> paused = new ArrayList<TelemetryTimer>();
        for(TelemetryInfo i : telemetryInfoStack) {
            for (TelemetryTimer timer : i.getTimers()) {
                if(timer != null) {
                    if(timer.isRunning()) {
                       timer.stop();
                        paused.add(timer);
                    }
                }
            }
        }
        return paused;
    }



    private void writeObject(TelemetryInfo info, String name, Object object) {
        List<TelemetryTimer> paused = pauseRunningTimers();
        String zipEntryName = getTelemetryInfoArtefactZipEntryName(info, name);

            try {
                zipOutputStream.putNextEntry(new ZipEntry(zipEntryName));
//                BufferedOutputStream bos = new BufferedOutputStream(zipOutputStream);
                if(object instanceof TelemetryObject) {
                    TelemetryObject telemetryObject = (TelemetryObject) object;
                    telemetryObject.serialise(zipOutputStream);
                }
                else {
                    PrintWriter pw = new PrintWriter(zipOutputStream);
                    pw.print(object);
                    pw.flush();
                }
//                bos.flush();
                zipOutputStream.closeEntry();
                zipOutputStream.flush();
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        unpauseTimers(paused);
    }






}
