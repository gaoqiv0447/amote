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


/**
 * A request that the server starts sending a set of tiles. 
 * 
 * @author Marc Stogaitis
 * 
 */
public class TileSetReq extends AbstractPacket implements Serializable{

  private static final long serialVersionUID = 1L;

  private int tile1X, tile1Y, tile2X, tile2Y;
  
  public TileSetReq(int tile1X, int tile1Y,int tile2X,int tile2Y) {
    super(Command.TILE_SET_REQ);
    this.tile1X = tile1X;
    this.tile1Y = tile1Y;
    this.tile2X = tile2X;
    this.tile2Y = tile2Y;
  }

  public int getTile1X() {
    return tile1X;
  }

  public int getTile1Y() {
    return tile1Y;
  }

  public int getTile2X() {
    return tile2X;
  }

  public int getTile2Y() {
    return tile2Y;
  }

  @Override
  public String toString() {
    return tile1X + " " + tile1Y + " " + tile2X + " " + tile2Y;
  }
  
  
}
