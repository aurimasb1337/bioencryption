package com.example.bioencryption.utils;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

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


   public static void encrypt() throws IOException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException {
        // Here you read the cleartext.
        File extStore = Environment.getExternalStorageDirectory();
        FileInputStream fis = new FileInputStream(extStore + "/sampleFile");
        // This stream write the encrypted text. This stream will be wrapped by
        // another stream.
        FileOutputStream fos = new FileOutputStream(extStore + "/encrypted");

        // Length is 16 byte
        SecretKeySpec sks = new SecretKeySpec("MyDifficultPassw".getBytes(),
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
    }
        public  static void decrypt() throws IOException, NoSuchAlgorithmException,
                NoSuchPaddingException, InvalidKeyException {

            File extStore = Environment.getExternalStorageDirectory();
            FileInputStream fis = new FileInputStream(extStore + "/encrypted");

            FileOutputStream fos = new FileOutputStream(extStore + "/decrypted");
            SecretKeySpec sks = new SecretKeySpec("MyDifficultPassw".getBytes(),
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
        }
}
