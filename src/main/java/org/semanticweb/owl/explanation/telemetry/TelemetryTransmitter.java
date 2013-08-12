package org.semanticweb.owl.explanation.telemetry;
/*
 * Copyright (C) 2010, University of Manchester
 *
 * Modifications to the initial code base are copyright of their
 * respective authors, or their employers as appropriate.  Authorship
 * of the modifications may be determined from the ChangeLog placed at
 * the end of this file.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

/**
 * Author: Matthew Horridge<br>
 * The University of Manchester<br>
 * Information Management Group<br>
 * Date: 14-Mar-2010
 */
public class TelemetryTransmitter implements TelemetryDevice {

    private static TelemetryReceiver nullTelemetryReceiver = new NullTelemetryReceiver();

    private static TelemetryTransmitter instance = new TelemetryTransmitter();

    private TelemetryReceiver telemetryReceiver;


    public static TelemetryTransmitter getTransmitter() {
        return instance;
    }

    private TelemetryTransmitter() {
        telemetryReceiver = nullTelemetryReceiver;
    }

    public void setTelemetryReceiver(TelemetryReceiver telemetryReceiver) {
        if (telemetryReceiver != null) {
            this.telemetryReceiver = telemetryReceiver;
        }
        else {
            this.telemetryReceiver = nullTelemetryReceiver;
        }
    }

    public void beginTransmission(TelemetryInfo transmitter) {
        telemetryReceiver.beginTransmission(transmitter);
    }

    public void recordMeasurement(TelemetryInfo info, String propertyName, String value) {
        telemetryReceiver.recordMeasurement(info, propertyName, value);
    }

    public void recordMeasurement(TelemetryInfo info, String propertyName, Number number) {
        telemetryReceiver.recordMeasurement(info, propertyName, number.toString());
    }

    public void recordMeasurement(TelemetryInfo info, String propertyName, boolean b) {
        telemetryReceiver.recordMeasurement(info, propertyName, Boolean.toString(b));
    }

    public void recordException(TelemetryInfo info, Throwable exception) {
        telemetryReceiver.recordException(info, exception);
    }

    public void recordObject(TelemetryInfo info, String namePrefix, String nameSuffix, Object object) {
        telemetryReceiver.recordObject(info, namePrefix, nameSuffix, object);
    }

    public void recordTiming(TelemetryInfo info, String name, TelemetryTimer telemetryTimer) {
        telemetryReceiver.recordTiming(info, name, telemetryTimer);
    }

    public void endTransmission(TelemetryInfo transmitter) {
        telemetryReceiver.endTransmission(transmitter);
    }
}
