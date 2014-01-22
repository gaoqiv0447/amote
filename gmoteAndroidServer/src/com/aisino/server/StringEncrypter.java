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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import com.aisino.server.settings.SystemPaths;

import android.util.Log;

/**
 * Based on the example code found at:
 * http://www.devx.com/Java/10MinuteSolution/21385/1954 from Javid Jamae Allows
 * us to encrypt our passwords when storing it to a file. This is not secure
 * since the key can be reverse engineering out of this file but we assume that
 * the user's computer is reasonably secure.
 */
public class StringEncrypter {

    public static final String DES_NAME = "DES";

    /**
     * Key used during encryption. This could easily be reversed engineered.
     */
    public static final String SECRET = "ABKDIEKF3Ikdiekdjfow FKEIDKSI fkeijklas2f";

    private KeySpec keySpec;

    private SecretKeyFactory keyFactory;

    private Cipher cipher;

    private static final String UNICODE_FORMAT = "UTF8";

    private static final String TAG = "StringEncrypter";

    public static void writePasswordToFile(String password) throws EncryptionException {

        try {
            StringEncrypter se = new StringEncrypter();
            byte[] cipherText = se.encrypt(password);

            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
                    SystemPaths.PASSWORD.getFullPath()));
            oos.writeObject(new ByteContainer(cipherText));
            oos.close();

        } catch (FileNotFoundException e) {
            Log.d(TAG, e.getMessage(), e);
            throw new EncryptionException(e);
        } catch (IOException e) {
            Log.d(TAG, e.getMessage(), e);
            throw new EncryptionException(e);
        } catch (InvalidKeyException e) {
            Log.d(TAG, e.getMessage(), e);
            throw new EncryptionException(e);
        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, e.getMessage(), e);
            throw new EncryptionException(e);
        } catch (NoSuchPaddingException e) {
            Log.d(TAG, e.getMessage(), e);
            throw new EncryptionException(e);
        } catch (InvalidKeySpecException e) {
            Log.d(TAG, e.getMessage(), e);
            throw new EncryptionException(e);
        } catch (IllegalBlockSizeException e) {
            Log.d(TAG, e.getMessage(), e);
            throw new EncryptionException(e);
        } catch (BadPaddingException e) {
            Log.d(TAG, e.getMessage(), e);
            throw new EncryptionException(e);
        }
    }

    public static synchronized String readPasswordFromFile() {

        ObjectInputStream is;
        try {
            is = new ObjectInputStream(new FileInputStream(SystemPaths.PASSWORD.getFullPath()));

            ByteContainer cipherText;
            cipherText = (ByteContainer) is.readObject();
            is.close();
            StringEncrypter se = new StringEncrypter();
            return se.decrypt(cipherText.getCipherText());
        } catch (FileNotFoundException e) {
            Log.d(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.d(TAG, e.getMessage(), e);
        } catch (InvalidKeyException e) {
            Log.d(TAG, e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, e.getMessage(), e);
        } catch (NoSuchPaddingException e) {
            Log.d(TAG, e.getMessage(), e);
        } catch (InvalidKeySpecException e) {
            Log.d(TAG, e.getMessage(), e);
        } catch (IllegalBlockSizeException e) {
            Log.d(TAG, e.getMessage(), e);
        } catch (BadPaddingException e) {
            Log.d(TAG, e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            Log.d(TAG, e.getMessage(), e);
        }
        return "";
    }

    public StringEncrypter() throws UnsupportedEncodingException, InvalidKeyException,
            NoSuchAlgorithmException, NoSuchPaddingException {

        byte[] keyAsBytes = SECRET.getBytes(UNICODE_FORMAT);

        keySpec = new DESKeySpec(keyAsBytes);

        keyFactory = SecretKeyFactory.getInstance(DES_NAME);
        cipher = Cipher.getInstance(DES_NAME);

    }

    private byte[] encrypt(String unencryptedString) throws InvalidKeySpecException,
            InvalidKeyException, UnsupportedEncodingException, IllegalBlockSizeException,
            BadPaddingException {

        SecretKey key = keyFactory.generateSecret(keySpec);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cleartext = unencryptedString.getBytes(UNICODE_FORMAT);
        byte[] ciphertext = cipher.doFinal(cleartext);

        return ciphertext;

    }

    private String decrypt(byte[] ciphertext) throws InvalidKeySpecException, InvalidKeyException,
            IOException, IllegalBlockSizeException, BadPaddingException {

        SecretKey key = keyFactory.generateSecret(keySpec);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] cleartext = cipher.doFinal(ciphertext);

        return bytes2String(cleartext);
    }

    private static String bytes2String(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            stringBuffer.append((char) bytes[i]);
        }
        return stringBuffer.toString();
    }
}
