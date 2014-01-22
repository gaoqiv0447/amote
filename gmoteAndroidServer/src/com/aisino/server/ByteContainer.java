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

import java.io.Serializable;

public class ByteContainer implements Serializable{

    private static final long serialVersionUID = 1L;
    private byte[] cipherText;

    public ByteContainer( byte[] cipherText) {
        this.cipherText = cipherText;
    }
    public byte[] getCipherText() {
      return cipherText;
    }
  }

