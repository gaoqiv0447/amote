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

package com.aisino.server.settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

public abstract class AbstractSettings<E extends Enum<E>,T> {
    private final String TAG = "AbstractSettings";
    private Map<Enum<E>, T> settings = new HashMap<Enum<E>, T>();

    /** The default value to return if there is no entry for a particular property **/
    private T defaultSetting;

    /** The file where the key-value pairs are stored **/
    private String fileName;

    protected AbstractSettings(String fileName, T defaultSetting) {
      this.fileName = fileName;
      this.defaultSetting = defaultSetting;
      loadSettings();
    }

    private void loadSettings() {
      try {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        settings.clear();

        String line;
        try {
          while ((line = reader.readLine()) != null) {
            String[] fields = line.split("=");
            Enum<E> key =  convertKey(fields[0]);
            settings.put(key, convertValue(fields[1]));
          }
        } finally {
          reader.close();
        }
      } catch (IOException e) {
        Log.d(TAG, e.getMessage());
      }
    }



    private void writeSettings() {
      try {
        BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));

        for (Map.Entry<Enum<E>, T> entry : settings.entrySet()) {
          bw.write(entry.getKey() + "=" + entry.getValue());
          bw.newLine();
        }
        bw.close();
      } catch (IOException e) {
        Log.d(TAG, e.getMessage());
      }
    }

    public T getSetting(Enum<E> setting) {
      if (settings.containsKey(setting)) {
        return settings.get(setting);
      } else {
        return defaultSetting;
      }
    }

    public void setSetting(Enum<E> setting, T value) {
      settings.put(setting, value);
      writeSettings();
    }

    /**
     * Converts the 'value' field of a name value pair to an object.
     * Should be implemented by a subclass
     * @param value
     */
    protected abstract T convertValue(String value);


    protected abstract Enum<E> convertKey(String key);
}
