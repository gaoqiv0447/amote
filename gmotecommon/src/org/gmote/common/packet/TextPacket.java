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

import org.gmote.common.Protocol.Command;

public class TextPacket extends AbstractPacket {
	String TextStr;
	
	public TextPacket(String TextStr) {
		super(Command.TEXTSTR_REQ);
		this.TextStr = TextStr;
	}
	
	public String getTextPacket() {
	    return this.TextStr;
	  }
	private static final long serialVersionUID = 1L;
}
