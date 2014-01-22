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

package com.aisino.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import com.aisino.server.settings.PreferredPorts;

import android.util.Log;

public class UpnpUtil {

	private static final String TAG = "UpnpUtil";
	private static final int DEFAULT_STARTING_PORT = 8851;
	private static final int MAX_PORT = 9851;

	public int getPort(InetAddress address) {
		String ip = address.getHostAddress();
		Integer port = PreferredPorts.instance().getPreferredPort(ip);
		//if (port == null || !isPortAvailableLocally(port)) {
		if (port == null ) {
			// Assign a new port to this ip.
			port = selectNewPort("TCP");
			PreferredPorts.instance().addPort(ip, port);
			renewRouterNat(port, address, "TCP");
		}
		return port;
	}

	private boolean isPortAvailableLocally(int selectedPort) {
		try {
			ServerSocket serverSocket = new ServerSocket(selectedPort);
			serverSocket.close();
			// Log.d(TAG,"Found port that is available locally: " +
			// selectedPort);
			return true;
		} catch (IOException e) {
			Log.d(TAG, "Port is not available locally: " + selectedPort + " "
					+ e.getMessage(), e);
			return false;
		}
	}

	private int selectNewPort(String protocol) {
		Log.d(TAG, "Selecting new port");
		Integer availableLocalPort = null;
		for (int selectedPort = DEFAULT_STARTING_PORT; selectedPort <= MAX_PORT; selectedPort++) {
			if (!PreferredPorts.instance().isPortAssigned(selectedPort)) {
				if (isPortAvailableLocally(selectedPort)) {
					if (availableLocalPort == null) {
						availableLocalPort = selectedPort;
						return selectedPort;
					}
				}
			}
		}

		Log.d(TAG,
				"Unable to find available port. Returning default port. "
						+ DEFAULT_STARTING_PORT);
		return DEFAULT_STARTING_PORT;

	}

	/**
	 * Renews or creates a NAT entry for this computer that will be available
	 * for 1 month.
	 */
	private void renewRouterNat(int portChosen, InetAddress inetAddress,
			String protocol) {
		// TODO:
	}
}
