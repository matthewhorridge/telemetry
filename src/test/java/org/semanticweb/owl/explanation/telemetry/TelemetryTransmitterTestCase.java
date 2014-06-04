package org.semanticweb.owl.explanation.telemetry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics Research Group, Date: 04/06/2014
 */
@RunWith(MockitoJUnitRunner.class)
public class TelemetryTransmitterTestCase {


    @Mock
    private TelemetryInfo info;

    @Mock
    private TelemetryReceiver receiver;

    private TelemetryTransmitter transmitter;

    @Before
    public void setUp() throws Exception {
        transmitter = TelemetryTransmitter.getTransmitter();
        transmitter.setTelemetryReceiver(receiver);
    }

    @Test
    public void shouldCallBeginTransmission() {
        transmitter.setTelemetryReceiver(receiver);
        transmitter.beginTransmission(info);
        verify(receiver, times(1)).beginTransmission(info);
    }

    @Test
    public void shouldCallEndTransmission() {
        transmitter.endTransmission(info);
        verify(receiver, times(1)).endTransmission(info);
    }

    @Test
     public void shouldCallRecordMeasurement() {
        String name = "name";
        String value = "value";
        transmitter.recordMeasurement(info, name, value);
        verify(receiver).recordMeasurement(info, name, value);
    }

    @Test
    public void shouldCallRecordMeasurementWithIntegerValue() {
        String name = "name";
        Integer value = 3;
        transmitter.recordMeasurement(info, name, value);
        verify(receiver).recordMeasurement(info, name, "3");
    }

    @Test
    public void shouldCallRecordMeasurementWithBooleanValue() {
        String name = "name";
        transmitter.recordMeasurement(info, name, true);
        verify(receiver).recordMeasurement(info, name, "true");
    }

    @Test
    public void shouldCallRecordException() {
        Throwable exception = mock(Throwable.class);
        transmitter.recordException(info, exception);
        verify(receiver).recordException(info, exception);
    }

    @Test
    public void shouldCallRecordTiming() {
        TelemetryTimer timer = mock(TelemetryTimer.class);
        String time = "time";
        transmitter.recordTiming(info, time, timer);
        verify(receiver).recordTiming(info, time, timer);
    }

    @Test
    public void shouldCallRecordObject() {
        Object object = mock(Object.class);
        transmitter.recordObject(info, "prefix", "suffix", object);
        verify(receiver).recordObject(info, "prefix", "suffix", object);
    }

}
