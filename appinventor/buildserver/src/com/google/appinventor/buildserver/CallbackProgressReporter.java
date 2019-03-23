// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver;

import com.google.common.io.ByteStreams;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.logging.Logger;


public class CallbackProgressReporter implements ProgressReporter {
  // We create a ProgressReporter instance which is handed off to the
  // project builder and compiler. It is called to report the progress
  // of the build. The reporting is done by calling the callback URL
  // and putting the status inside a "build.status" file. This isn't
  // particularly efficient, but this is the version 0.9 implementation
  
  private static final Logger LOG = Logger.getLogger(CallbackProgressReporter.class.getName());
  String callbackUrlStr;
  
  CallbackProgressReporter(String callbackUrlStr) {
    this.callbackUrlStr = callbackUrlStr;
  }

  public void report(int progress) {
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      ZipOutputStream zipoutput = new ZipOutputStream(output);
      zipoutput.putNextEntry(new ZipEntry("build.status"));
      PrintWriter pout = new PrintWriter(zipoutput);
      pout.println(progress);
      pout.flush();
      zipoutput.flush();
      zipoutput.close();
      ByteArrayInputStream zipinput = new ByteArrayInputStream(output.toByteArray());
      URL callbackUrl = new URL(callbackUrlStr);
      HttpURLConnection connection = (HttpURLConnection) callbackUrl.openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");
      // Make sure we aren't misinterpreted as
      // form-url-encoded
      connection.addRequestProperty("Content-Type","application/zip; charset=utf-8");
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);
      BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(connection.getOutputStream());
      try {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(zipinput);
        try {
          ByteStreams.copy(bufferedInputStream,bufferedOutputStream);
          bufferedOutputStream.flush();
        } finally {
          bufferedInputStream.close();
        }
      } finally {
        bufferedOutputStream.close();
      }
      if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
        LOG.severe("Bad Response Code! (sending status): "+ connection.getResponseCode());
      }
    } catch (IOException e) {
      LOG.severe("IOException during progress report!");
    }
  }
}


