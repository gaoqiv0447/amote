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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import com.aisino.server.ServerUtil;

import android.util.Log;

/**
 * Keeps a map of ip addresses and their preferred ports.
 * 
 */
public class PreferredPorts {
	private static final String TAG = "PreferredPorts";

	private static PreferredPorts instance = null;
	/** HashMap save IP(String) & port(Integer)*/
	private Map<String, Integer> preferredPorts = new HashMap<String, Integer>();

	public PreferredPorts() {
		loadData();
	}

	public synchronized static PreferredPorts instance() {
		if (instance == null) {
			instance = new PreferredPorts();
		}
		return instance;
	}

	public void addPort(String ip, int port) {
		if (preferredPorts.containsKey(ip)) {
			Log.d(TAG, "Overwriting port setting for ip: " + ip + " from "
					+ preferredPorts.get(ip) + " to " + port);
		}
		preferredPorts.put(ip, port);
		saveData();
	}

	public boolean isPortAssigned(int port) {
		for (int value : preferredPorts.values()) {
			if (value == port) {
				return true;
			}
		}
		return false;
	}

	public Integer getPreferredPort(String ip) {
		return preferredPorts.get(ip);
	}

	private void saveData() {
		ServerUtil.createIfNotExists(SystemPaths.getRootPath());
		try {
			PrintWriter writer = new PrintWriter(new FileWriter(
					SystemPaths.PREFERED_PORTS.getFullPath()));
			for (Map.Entry<String, Integer> entry : preferredPorts.entrySet()) {
				writer.println(entry.getKey() + "=" + entry.getValue());
			}
			writer.close();

		} catch (IOException e) {
			Log.d(TAG, e.getMessage(), e);

		}
	}

	private void loadData() {
		preferredPorts = new HashMap<String, Integer>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					SystemPaths.PREFERED_PORTS.getFullPath()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] lineSplit = line.split("=");
				preferredPorts
						.put(lineSplit[0], Integer.parseInt(lineSplit[1]));
			}
		} catch (FileNotFoundException e) {
			Log.d(TAG, e.getMessage(), e);
		} catch (IOException e) {
			Log.d(TAG, e.getMessage(), e);
		}
	}

	public Map<String, Integer> getPreferedPorts() {
		return preferredPorts;
	}
}
