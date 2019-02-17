// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2014 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.server;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import com.google.appinventor.server.flags.Flag;

import com.google.appinventor.server.storage.StorageIo;
import com.google.appinventor.server.storage.StorageIoInstanceHolder;
import com.google.appinventor.server.storage.StoredData.PWData;

import com.google.appinventor.server.util.PasswordHash;
import com.google.appinventor.server.util.UriBuilder;

import com.google.appinventor.shared.rpc.user.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.DefaultLdapConnectionFactory;
import org.apache.directory.ldap.client.api.DefaultPoolableLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnectionPool;


/**
 * LoginServlet -- Handle logging someone in using an email address for a login
 * name and a password, which is stored hashed (and salted). Facilities are
 * provided to e-mail a password to an e-mail address both to set one up the
 * first time and to recover a lost password.
 *
 * This implementation uses a helper server to send mail. It does a webservices
 * transaction (REST/POST) to the server with the email address and reset url.
 * The helper server then formats the e-mail message and sends it. The source
 * code is in misc/passwordmail/...
 *
 * @author jis@mit.edu (Jeffrey I. Schiller)
 */
@SuppressWarnings("unchecked")
public class LoginLDAPServlet extends BaseLoginServlet {
  protected static final Logger LOG = Logger.getLogger(LoginLDAPServlet.class.getName());
  private LdapConnectionPool ldapPool;

  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    // Init LDAP connection
    if (useLDAP.get()) {
      String server = ldapServer.get();
      int port = ldapPort.get();
      String name = ldapName.get();
      String credential = ldapCredential.get();
      int timeout = ldapTimeOut.get();
      boolean useSsl = ldapUseSsl.get();

      // Verify LDAP configuration
      if (server == null || port < 0 || name == null || credential == null || timeout < 0) {
        throw new ServletException("One of ldapauth.{server,port,name,credential,timeout} is not correctly configured");
      }

      LdapConnectionConfig connConfig = new LdapConnectionConfig();
      connConfig.setLdapHost(server);
      connConfig.setLdapPort(port);
      connConfig.setName(name);
      connConfig.setCredentials(credential);
      connConfig.setUseSsl(useSsl);
      connConfig.setTimeout(timeout);

      DefaultLdapConnectionFactory factory = new DefaultLdapConnectionFactory(connConfig);
      factory.setTimeOut(timeout);

      ldapPool = new LdapConnectionPool(new DefaultPoolableLdapConnectionFactory(factory));
    }
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
    if (DEBUG) {
      LOG.info("requestURI = " + req.getRequestURI());
    }

    // Check if LDAP login is enabled
    if (!useLDAP.get()) {
      req.setAttribute("title", "LDAP authentication is disabled");
      req.setAttribute("reason", "Please contact the server admin to require logging in with LDAP account.");
      req.getRequestDispatcher("/error.jsp").forward(req, resp);
      return;
    }

    assert ldapPool != null;

    // Parse common parameters
    Map<String, String> commonParams = getCommonParams(req);
    String locale = commonParams.get("locale");
    String repo = commonParams.get("repo");
    String galleryId = commonParams.get("galleryId");
    String redirect = commonParams.get("redirect");

    // Common method variables
    OdeAuthFilter.UserInfo userInfo = OdeAuthFilter.getUserInfo(req);
    ResourceBundle bundle = ResourceBundle.getBundle("com/google/appinventor/server/loginmessages", new Locale(locale));

    if (DEBUG) {
      LOG.info("locale = " + locale + " bundle: " + new Locale(locale));
    }

    // TODO redirect to login page and show LDAP option
  }

  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws IOException, ServletException {

    // Check if LDAP login is enabled
    if (!useLDAP.get()) {
      req.setAttribute("title", "LDAP authentication is disabled");
      req.setAttribute("reason", "Please contact the server admin to require logging in with LDAP account.");
      req.getRequestDispatcher("/error.jsp").forward(req, resp);
      return;
    }

    assert ldapPool != null;

    // Decode common parameters
    Map<String, String> commonParams = getCommonParams(req);
    String locale = commonParams.get("locale");
    String repo = commonParams.get("repo");
    String galleryId = commonParams.get("galleryId");
    String redirect = commonParams.get("redirect");

    // Function variables
    ResourceBundle bundle = ResourceBundle.getBundle("com/google/appinventor/server/loginmessages", new Locale(locale));

    if (DEBUG) {
      LOG.info("locale = " + locale + " bundle: " + new Locale(locale));
    }

    // Setup LDAP connections
    LdapConnection connection;
    try {
      connection = ldapPool.getConnection();
    } catch (LdapException e) {
      LOG.severe("LDAP connection failed: " + e.getMessage());
      fail(req, resp, "Failed to connect ot LDAP server"); // Not sure what else to do
      return;
    }

    // Authentication
    // TODO

    // Finalize
    connection.close();
  }

}
