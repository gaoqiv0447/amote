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

package com.aisino.server.media.basic;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.blinkenlights.jid3.ID3Exception;
import org.blinkenlights.jid3.ID3Tag;
import org.blinkenlights.jid3.MP3File;
import org.blinkenlights.jid3.v1.ID3V1Tag;
import org.blinkenlights.jid3.v1.ID3V1_0Tag;
import org.blinkenlights.jid3.v1.ID3V1_1Tag;
import org.blinkenlights.jid3.v2.APICID3V2Frame;
import org.blinkenlights.jid3.v2.ID3V2_3_0Tag;
import org.gmote.common.media.MediaMetaInfo;

import android.util.Log;

public class PlayerUtil {
    private static final String TAG = "PlayerUtil";

    public static byte[] loadImage(String imageName) {

        InputStream is = PlayerUtil.class.getResourceAsStream("/res/" + imageName);
        if (is == null) {
          return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        byte[] image;
        try {
          while (is.read(buffer) != -1) {
            baos.write(buffer);
          }
          image = baos.toByteArray();
          baos.close();
          is.close();
          return image;
        } catch (IOException e) {
          Log.d(TAG, e.getMessage(), e);
          return null;
        }
      }
    public static MediaMetaInfo getSongMetaInfo(MP3File mp3) {

        String title = null;
        String artist = null;
        String album = null;
        try {
          for (ID3Tag id3Tag : mp3.getTags()) {
            if (id3Tag instanceof ID3V1_0Tag || id3Tag instanceof ID3V1_1Tag) {
              ID3V1Tag tag = (ID3V1Tag) id3Tag;
              title = setIfNull(title, tag.getTitle());
              artist = setIfNull(artist, tag.getArtist());
              album = setIfNull(album, tag.getAlbum());

            } else if (id3Tag instanceof ID3V2_3_0Tag) {
              ID3V2_3_0Tag tag = (ID3V2_3_0Tag)id3Tag;
              title = setIfNull(title, tag.getTitle());
              artist = setIfNull(artist, tag.getArtist());
              album = setIfNull(album, tag.getAlbum());
            }
          }
        } catch (ID3Exception e) {
          Log.d(TAG, e.getMessage(), e);
        }

        return new MediaMetaInfo(title, artist, album,null,true);
      }

    public static byte[] extractEmbeddedImageData(MP3File mp3) {

        try {
          for (ID3Tag tag : mp3.getTags()) {

            if (tag instanceof ID3V2_3_0Tag) {
              ID3V2_3_0Tag tag2 = (ID3V2_3_0Tag) tag;

              if (tag2.getAPICFrames() != null && tag2.getAPICFrames().length > 0) {
                // Simply take the first image that is available.
                APICID3V2Frame frame = tag2.getAPICFrames()[0];
                return frame.getPictureData();
              }
            }
          }
        } catch (ID3Exception e) {
          Log.d(TAG, e.getMessage(), e);
        }
        return null;
      }

      public static byte[] extractImageFromFolder(String mediaMrl) {
        File file = new File(mediaMrl);
        file = new File(file.getParent() + File.separator + "Folder.jpg");
        byte[] imageData = null;
        if (file.exists()) {
          imageData = extractImageArtworkFromFile(file.getAbsolutePath());
        }
        return imageData;
      }

      public static byte[] extractImageArtworkFromFile(String artworkUrl) {
          Log.d(TAG,"*****extractImageArtworkFromFile*******");
          return null;
        }

    private static String setIfNull(String metaField, String metaData) {
        return (metaField == null) ? metaData : metaField;
      }
}
