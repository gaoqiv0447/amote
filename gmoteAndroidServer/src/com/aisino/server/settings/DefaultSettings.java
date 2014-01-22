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

public class DefaultSettings extends AbstractSettings<DefaultSettingsEnum, String> {

    private static DefaultSettings instance = null;

    private DefaultSettings(String fileName) {
      super(fileName, "");
    }

    public static synchronized DefaultSettings instance() {
      if (instance == null) {
        instance = new DefaultSettings(SystemPaths.DEFAULT_SETTINGS.getFullPath());
      }
      return instance;
    }

    @Override
    protected String convertValue(String value) {
      return value;
    }

    @Override
    protected Enum<DefaultSettingsEnum> convertKey(String key) {
      return DefaultSettingsEnum.valueOf(key);
    }

    public static void createDefaultFile() {
      instance().setSetting(DefaultSettingsEnum.VOLUME, "80");
      instance().setSetting(DefaultSettingsEnum.MONITOR_X, "1");
      instance().setSetting(DefaultSettingsEnum.MONITOR_Y, "1");
      instance().setSetting(DefaultSettingsEnum.SHOW_ALL_FILES, "false");
      instance().setSetting(DefaultSettingsEnum.UDP_PORT, "9901");
      instance().setSetting(DefaultSettingsEnum.SHUFFLE_SONGS, "false");
    }

}
