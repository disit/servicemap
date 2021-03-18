/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.disit.servicemap;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author bellini
 */
public class Encrypter {

  public static String encrypt(String value) throws Exception{
    Configuration conf = Configuration.getInstance();
    String sha256SecretKey = DigestUtils.sha256Hex(conf.get("encryptKey", null));
    String sha256IvParameter = DigestUtils.sha256Hex(conf.get("encryptIV", null));
    IvParameterSpec iv;
    try {
      iv = new IvParameterSpec(sha256IvParameter.substring(0, 16).getBytes(StandardCharsets.UTF_8));
      SecretKeySpec skeySpec = new SecretKeySpec(sha256SecretKey.substring(0, 32).getBytes(StandardCharsets.UTF_8), "AES");
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
      byte[] encrypted = cipher.doFinal(value.getBytes());
      java.util.Base64.Encoder b64 = java.util.Base64.getEncoder();
      return b64.encodeToString(b64.encodeToString(encrypted).getBytes());
      //return Base64.encodeBase64String(Base64.encodeBase64String(encrypted).getBytes());
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
            | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
      ServiceMap.notifyException(e);
      throw e;
    }
  }

  public static String decrypt(String encrypted) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sha256SecretKey = DigestUtils.sha256Hex(conf.get("encryptKey", null));
    String sha256IvParameter = DigestUtils.sha256Hex(conf.get("encryptIV", null));
    IvParameterSpec iv;
    try {
      iv = new IvParameterSpec(sha256IvParameter.substring(0, 16).getBytes(StandardCharsets.UTF_8));
      SecretKeySpec skeySpec = new SecretKeySpec(sha256SecretKey.substring(0, 32).getBytes(StandardCharsets.UTF_8), "AES");
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
      cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
      byte[] original = cipher.doFinal(Base64.decodeBase64(Base64.decodeBase64(encrypted)));

      return new String(original);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
            | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
      ServiceMap.notifyException(e);
      throw e;
    }
  }
}
