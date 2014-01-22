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

package org.gmote.common.packet;

import java.util.HashMap;

import org.gmote.common.Protocol.Command;

/**
 * To be send from server to client, in order to uniform the sensors state.
 * @author gaoqi
 *
 */
public class SensorStatePacket extends AbstractPacket {

	private static final long serialVersionUID = 1L;
	/** sensor state Hash map*/
	private HashMap<Integer, Boolean> sensorStateHashMap;

	/**
	 * sensor state packet.
	 * @param hm input the server sensors state hash map.
	 */
	public SensorStatePacket(final HashMap<Integer, Boolean> hm) {
		super(Command.SENSOR_STATE_EVENT);
		sensorStateHashMap = hm;
	}

	/**
	 * Get sensor state hash map.
	 * @return the hash map in the packet.
	 */
	public final HashMap<Integer, Boolean> getSensorStateHashMap() {
		return sensorStateHashMap;
	}
}
