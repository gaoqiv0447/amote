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

import java.io.Serializable;

import org.gmote.common.Protocol.Command;
import org.gmote.common.Protocol.MouseEvent;

/**
 * Client requests a mouse click on a specific tyle.
 * 
 * @author Marc Stogaitis
 * 
 */
public class TileClickReq extends AbstractPacket implements Serializable{

  private static final long serialVersionUID = 1L;

  private int tileIdX, tileIdY;
  private int pixelOffsetInTileX, pixelOffsetInTileY;
  private MouseEvent mouseEvent;
  
  public TileClickReq(int tileIdX, int tileIdY, int pixelOffsetInTileX, int pixelOffsetInTileY, MouseEvent mouseEvent) {
    super(Command.TILE_CLICK_REQ);
    this.tileIdX = tileIdX;
    this.tileIdY = tileIdY;
    this.pixelOffsetInTileX = pixelOffsetInTileX;
    this.pixelOffsetInTileY = pixelOffsetInTileY;
    this.mouseEvent = mouseEvent;
  }

  public int getPixelOffsetInTileX() {
    return pixelOffsetInTileX;
  }

  public int getPixelOffsetInTileY() {
    return pixelOffsetInTileY;
  }

  public MouseEvent getMouseEvent() {
    return mouseEvent;
  }

  public int getTileIdX() {
    return tileIdX;
  }

  public int getTileIdY() {
    return tileIdY;
  }
 
}
