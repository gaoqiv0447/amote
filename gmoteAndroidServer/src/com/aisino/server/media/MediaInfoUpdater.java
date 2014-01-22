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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import org.blinkenlights.jid3.ID3Exception;
import org.blinkenlights.jid3.ID3Tag;
import org.blinkenlights.jid3.MP3File;
import org.blinkenlights.jid3.v2.APICID3V2Frame;
import org.blinkenlights.jid3.v2.ID3V2_3_0Tag;
import org.gmote.common.TcpConnection;
import org.gmote.common.media.MediaMetaInfo;
import org.gmote.common.packet.AbstractPacket;
import org.gmote.common.packet.MediaInfoPacket;
import org.gmote.common.packet.MediaInfoReqPacket;

import com.aisino.server.media.basic.PlayerUtil;

import android.util.Log;

/**
 * Periodically polls information about the currently playing media and sends it
 * to the phone. This is needed for media players that do not allow us to
 * receive a callback when media changes (such as a playlist going to the next
 * song, or the user changing the song directly on the computer's user
 * interface.
 *
 * @author Marc Stogaitis
 */

public class MediaInfoUpdater {
  private static MediaInfoUpdater instance = null;
  private static final int MEDIA_INFO_UPDATE_DELAY = 5000;
private static final String TAG = "MediaInfoUpdater";
  private byte[] lastGeneratedImageData = null;

  Timer pollingTimer = new Timer("MediaInfoTimer");

  MediaPlayerInterface mediaPlayer;
  TcpConnection con = null;

  // Private constructor to prevent instantiation.
  private MediaInfoUpdater() {

  }

  public static MediaInfoUpdater instance() {
    if (instance == null) {
      instance = new MediaInfoUpdater();
    }
    return instance;
  }

  public void setClientConnection(TcpConnection con) {
    this.con = con;
    changePollingState();
  }

  /**
   * Sends information about the currently playing media to the client.
   *
   * @param mediaInfo
   */
  public synchronized void sendMediaUpdate(MediaMetaInfo mediaInfo) {
    if (mediaInfo == null || con == null) {
      return;
    } else {
      try {
        Log.i(TAG,"----->Sending media info update");
        con.sendPacket(new MediaInfoPacket(mediaInfo));
      } catch (IOException e) {
        Log.d(TAG, e.getMessage(), e);
        setClientConnection(null);
      }
    }
  }

  public synchronized void setPlayerToPoll(MediaPlayerInterface mediaPlayer) {
    this.mediaPlayer = mediaPlayer;
    changePollingState();
  }

  private boolean shouldPoll() {
    return mediaPlayer != null;
  }

  private boolean clientIsConnected() {
    return con != null;
  }

  private void changePollingState() {
    if (!clientIsConnected()) {
      pollingTimer.cancel();
    } else if (!shouldPoll()) {
      pollingTimer.cancel();
    } else {
      pollingTimer.cancel();
      pollingTimer = new Timer("MediaInfoTimer");
      pollingTimer.schedule(new UpdateTask(), MEDIA_INFO_UPDATE_DELAY, MEDIA_INFO_UPDATE_DELAY);
    }
  }

  class UpdateTask extends TimerTask {
    @Override
    public synchronized void run() {
      sendMediaUpdate(mediaPlayer.getNewMediaInfo());
    }
  }

  /**
   * Convenience function to generate information about media based only on a
   * file on the local disc. Most media players have access to more information
   * than this and will therefore implement their own algorithm.
   * @param forceImageUpdate
   */
  public MediaMetaInfo generateMediaMetaInfo(String fileName, boolean forceImageUpdate) {
    MP3File mp3 = new MP3File(new File(fileName));
    MediaMetaInfo fileInfo = PlayerUtil.getSongMetaInfo(mp3);
    byte[] imageData = PlayerUtil.extractEmbeddedImageData(mp3);
    if (imageData == null) {
      // In windows, a folder.jpg file often contains the album art
      imageData = PlayerUtil.extractImageFromFolder(fileName);
    }

    boolean imageIsSame = lastGeneratedImageData != null && imageData != null && Arrays.equals(lastGeneratedImageData, imageData);
    if (imageIsSame && !forceImageUpdate) {
      fileInfo.setImageSameAsPrevious(true);
    } else {
      lastGeneratedImageData = imageData;
      fileInfo.setImage(imageData);
    }

    return fileInfo;
  }



  /**
   * Returns a packet with the latest media info.
   */
  public AbstractPacket handleMediaInfoReq(AbstractPacket packet) {
    MediaInfoReqPacket mediaInfoReq = (MediaInfoReqPacket) packet;
    if (new File(mediaInfoReq.getPathAndFileName()).exists()) {
      MediaMetaInfo fileInfo = generateMediaMetaInfo(mediaInfoReq.getPathAndFileName(), mediaInfoReq.isForceImageUpdate());
      if (fileInfo.getTitle() != null || fileInfo.getArtist() != null
          || fileInfo.getImage() != null) {
        return new MediaInfoPacket(fileInfo);
      }
    }
    return null;
  }
}

