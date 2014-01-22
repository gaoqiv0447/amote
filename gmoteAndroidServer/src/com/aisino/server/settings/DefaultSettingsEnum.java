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

public enum DefaultSettingsEnum {
 // Note: When you add a setting here, make sure to add it to the DefaultSettings.createDefaultFile()
    // Note: PORT is deprecated. Use PreferredPorts.
    PORT, PLAYER, LOG_VLC, VOLUME, MONITOR_X, MONITOR_Y, SHOW_ALL_FILES, UDP_PORT, SHUFFLE_SONGS;
}
