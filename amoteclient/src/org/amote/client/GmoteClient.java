/**
 * Copyright (C) 2009 Aisino Corporation Inc.
 *
 * No.18A, Xingshikou street, Haidian District,Beijing
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information of 
 * Aisino Corporation Inc. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in 
 * accordance with the terms of the license agreement you entered into
 * with Aisino.
 */

package org.amote.client;

import org.amote.client.android.ScreenInfo;
import org.amote.utils.SensorHub;
import org.gmote.common.ServerInfo;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/*
 * First Page, the entry of program.
 */
public class GmoteClient extends Activity {
  private static final String DEBUG_TAG = "GmoteClient";
  static final String PREFS = "prefs";
  static final String KEY_SERVER = "server";
  static final String KEY_PORT = "port";
  static final String KEY_PASSWORD = "password";
  static final String KEY_UDP_PORT = "udpport";
  static final String KEY_IN_STREAM_MODE = "stream_mode";
  static final String KEY_IS_MANUAL_IP = "is_manual_ip";
  private static final String TAG = "GmoteClient";

  /** Called when the activity is first created. */
  @Override
  public void onCreate(final Bundle icicle){
    super.onCreate(icicle);
    Log.d(DEBUG_TAG, "Client# onCreate");

    final String appVersion = getVersionNumber();
    Log.w(DEBUG_TAG, "ManifestVersion: " + appVersion + " ClientVersion" + Remote.GMOTE_CLIENT_VERSION);
    if (!Remote.GMOTE_CLIENT_VERSION.equalsIgnoreCase(appVersion)) {
      Log.w(DEBUG_TAG, "Manifest version doesn't match APP_VERSION. These two version numbers should always be in sync. Please update the approprivate value.");
    }

    final SharedPreferences prefs = getSharedPreferences(PREFS, MODE_WORLD_WRITEABLE);
    final String server = prefs.getString(KEY_SERVER, null);
    Remote.getInstance().setPassword(prefs.getString(KEY_PASSWORD, "1234"));
//    Log.i(DEBUG_TAG, "======setPassword  @   GmoteClient onCreate()");
//    Log.i(DEBUG_TAG, "--->password: "+Remote.getInstance().password);

    if (server == null) {
      setContentView(R.layout.welcome);
      // "Email me the link" button
      final TextView sendEmail = (TextView) findViewById(R.id.email_link);
      sendEmail.setOnClickListener(new OnClickListener() {
        public void onClick(final View v) {
          sendEmail();
        }
      });

      final Button continueButton = (Button) findViewById(R.id.welcome_continue);

      continueButton.setOnClickListener(new OnClickListener() {
          public void onClick(View v) {
              listServers();
          }
      });

    } else {
      final String serverIp = Remote.getInstance().getServerIp();
      // Only set the server if it's not already set (if we don't do this, it's
      // possible that the user will play a song on the phone, hit 'home' and
      // re-enter the app which would close the connection to the server.
      if (serverIp == null || serverIp.length() == 0) {
        setServerIpAndPassword(prefs, server);
      }
      startController();
    }
    setScreenInfo();
    SensorHub.getInstance(getApplicationContext());
  }

  private void setScreenInfo(){
      DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
      ScreenInfo.width = displayMetrics.widthPixels;
      ScreenInfo.height = displayMetrics.heightPixels;
      Log.i(TAG, "--->device width="+ScreenInfo.width+" height=ScreenInfo.height");
  }

  public static boolean isManualIp(SharedPreferences prefs) {
    return prefs.getBoolean(GmoteClient.KEY_IS_MANUAL_IP, false);
  }

  public static void setServerIpAndPassword(SharedPreferences prefs, String serverAddress) {

    int port = prefs.getInt(KEY_PORT, ServerInfo.DEFAULT_PORT);
    int udpPort = prefs.getInt(KEY_UDP_PORT, ServerInfo.DEFAULT_UDP_PORT);
    Remote.getInstance().setServer(new ServerInfo(serverAddress, port, udpPort));
    Remote.getInstance().setPassword(prefs.getString(KEY_PASSWORD, "1234"));
    Log.i(DEBUG_TAG, "======setPassword  @   GmoteClient setServerIpAndPassword()");

  }
    void startController() {
      final Intent intent = new Intent();
      intent.setClass(GmoteClient.this, MainActivity.class);
      startActivity(intent);
      finish();
    }

    void listServers() {
      final Intent intent = new Intent();
      intent.setClass(GmoteClient.this, ListServers.class);
      startActivity(intent);
      finish();
    }

    private void sendEmail() {
      final Intent emailIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:"));
      // We call the gmail application directly since there is a bug in the normal mail application
      // that prevents it from interpreting the EXTRA_SUBJECT and EXTRA_TEXT properly.
      emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Gmote Server Link");
      emailIntent.putExtra(Intent.EXTRA_TEXT,
      getString(R.string.email_intent));
      try {
        startActivity(emailIntent);
      } catch (final ActivityNotFoundException e) {
        // Try letting the user pick his own mail application. He will need to
        // copy the server url from the 'to' field
        emailIntent.setData(Uri.parse("mailto:http://mdd.aisino.com/"));
        emailIntent.setComponent(null);
        try {
          startActivity(emailIntent);
        } catch (final ActivityNotFoundException e2) {
          // Giving up.
          Toast.makeText(this, getString(R.string.launch_mail_error) + e2.getMessage(), 5);
        }
      }

    }

    private String getVersionNumber() {
    String version = "0.0.0";
    try {
      final PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
      version = pi.versionName;
    } catch (final PackageManager.NameNotFoundException e) {
      Log.e(DEBUG_TAG, "Package name not found", e);
    }
    if (version.equals("0.0.0")) {
      Log.w(DEBUG_TAG, "Unable to find the app's version number: " + version);
    }
    return version;
}

}