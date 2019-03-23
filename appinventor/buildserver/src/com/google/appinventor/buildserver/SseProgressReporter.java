// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver;

import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

public class SseProgressReporter implements ProgressReporter {
    // We create a ProgressReporter instance which is handed off to the
    // project builder and compiler. It is called to report the progress
    // of the build. The reporting is done by calling the callback URL
    // and putting the status inside a "build.status" file. This isn't
    // particularly efficient, but this is the version 0.9 implementation
    
    SseEventSink eventSink;
    Sse sse;
    
    SseProgressReporter(SseEventSink eventSink, Sse sse) {
        this.eventSink = eventSink;
        this.sse = sse;
    }

    public void report(int progress) {
        if (!eventSink.isClosed()) {
          eventSink.send(sse.newEvent("progress", Integer.toString(progress)));
        }
    }
}
