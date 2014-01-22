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


public class StartupSettings extends AbstractSettings<StartupSettingsEnum, Boolean> {
    private static StartupSettings instance = null;

    private StartupSettings(String fileName) {
      super(fileName, false);
    }

    public static synchronized StartupSettings instance() {
      if (instance == null) {
        instance = new StartupSettings(SystemPaths.STARTUP_SETTINGS.getFullPath());
      }
      return instance;
    }

    @Override
    protected Boolean convertValue(String value) {
      return Boolean.valueOf(value);
    }

    @Override
    protected Enum<StartupSettingsEnum> convertKey(String key) {
      return StartupSettingsEnum.valueOf(key);
    }

    public static void createDefaultFile() {
      instance().setSetting(StartupSettingsEnum.ADDED_DVD_DRIVES, true);
      instance().setSetting(StartupSettingsEnum.ADDED_TO_STARTUP, true);
      instance().setSetting(StartupSettingsEnum.PASSWORD_SHOWN, false);
      instance().setSetting(StartupSettingsEnum.PATH_SHOWN, false);
      instance().setSetting(StartupSettingsEnum.POPUP_SHOWN, false);
    }


}
