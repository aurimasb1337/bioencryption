package com.example.bioencryption.utils;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.bioencryption.models.FileModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptUtils {

    public static SecretKeySpec getKeyFromPassword(String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        String salt = "12345678";
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
        SecretKeySpec secret = new SecretKeySpec(factory.generateSecret(spec)
                .getEncoded(), "AES");
        return secret;
    }
    public static void encrypt(Context context, FileModel fileModel) throws IOException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {
        String [] fileNameArr = fileModel.getName().split("\\.");
 
        String   kelias=context.getExternalFilesDir(null) + "/encrypted";
        File file = new File(kelias, fileModel.getName());

        FileInputStream fis = new FileInputStream(file);

        FileOutputStream fos = new FileOutputStream(kelias + "/" +fileNameArr[0]+"-encrypted");


        SecretKeySpec sks = new SecretKeySpec(fileModel.getBase64id().substring(0,16).getBytes(),
                "AES");
        // Create cipher
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, sks);
        // Wrap the output stream
        CipherOutputStream cos = new CipherOutputStream(fos, cipher);
        // Write bytes
        int b;
        byte[] d = new byte[8];
        while ((b = fis.read(d)) != -1) {
            cos.write(d, 0, b);
        }
        // Flush and close streams.
        cos.flush();
        cos.close();
        fis.close();
        file.delete();
    }
    public  static void decrypt(Context context, FileModel fileModel) throws IOException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {
        String [] fileNameArr = fileModel.getName().split("\\.");
        String   kelias=context.getExternalFilesDir(null) + "/encrypted";
        File file = new File(kelias , fileNameArr[0] + "-encrypted");
        FileInputStream fis = new FileInputStream(file);

        FileOutputStream fos = new FileOutputStream(kelias + "/" + fileNameArr[0]+"-decrypted"+"."+fileNameArr[1]);

        SecretKeySpec sks = new SecretKeySpec(fileModel.getBase64id().substring(0,16).getBytes(),
                "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, sks);
        CipherInputStream cis = new CipherInputStream(fis, cipher);
        int b;
        byte[] d = new byte[8];
        while ((b = cis.read(d)) != -1) {
            fos.write(d, 0, b);
        }
        fos.flush();
        fos.close();
        cis.close();
        file.delete();



    }
}
