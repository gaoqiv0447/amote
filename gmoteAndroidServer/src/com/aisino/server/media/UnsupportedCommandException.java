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

package com.aisino.server.media;

public class UnsupportedCommandException extends Exception {
    private static final long serialVersionUID = 1L;

    public UnsupportedCommandException() {
        super("This command is not supported by the media player");
    }

    public UnsupportedCommandException(String message) {
      super(message);
    }

}
