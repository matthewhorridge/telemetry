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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Author: Matthew Horridge<br>
 * The University of Manchester<br>
 * Information Management Group<br>
 * Date: 10-Mar-2010
 */
public class TelemetryTimer {


    private long startTime;

    public long lastStopTime = 0;

    private boolean stopped = true;

    private long stopDuration = 0;

    private static ThreadMXBean bean = ManagementFactory.getThreadMXBean();


    public void start() {
        getCurrentTime();

        long currentTime = getCurrentTime();
        if(lastStopTime != 0) {
            stopDuration = stopDuration + (currentTime - lastStopTime);
        }
        if (startTime == 0) {
            startTime = getCurrentTime();
        }
        stopped = false;
    }

    private long getCurrentTime() {
        return bean.getCurrentThreadUserTime();
    }

    public long getInitialStartTime() {
        return startTime;
    }

    public void stop() {
        if (!stopped) {
            lastStopTime = getCurrentTime();//
            stopped = true;
        }
    }



    public long getEllapsedTime() {
        long time;
        if(!stopped) {
            time = (getCurrentTime() - startTime) - stopDuration;
        }
        else {
            time = (lastStopTime - startTime) - stopDuration;
        }
        return time;
    }

    public void reset() {
        startTime = 0;
        lastStopTime = 0;
    }

    public boolean isRunning() {
        return !stopped;
    }

  
}
