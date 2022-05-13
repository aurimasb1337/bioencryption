package com.example.bioencryption;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.hardware.biometrics.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKeys;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import com.example.bioencryption.adapters.RecyclerAdapter;
import com.example.bioencryption.models.FileModel;
import com.example.bioencryption.utils.EncryptUtils;
import com.example.bioencryption.utils.PathUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.Executor;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {
    private FloatingActionButton addbtn;
    private int REQUEST_CODE_FILE = 205;
    private  RecyclerAdapter adapter;
    private RecyclerView mRecyclerView;
    private MaterialButton decryptbtn, deleteBtn;
    private ArrayList<FileModel> data = new ArrayList<>();
    private Dialog handlerDialog;
    private TextView dialogFileName;
    private String userId;
    private String actionType = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addbtn=findViewById(R.id.addbtn);


        mRecyclerView = findViewById(R.id.recycler_view);
        handlerDialog = new Dialog(this);
        handlerDialog.setCancelable(true);
        handlerDialog.setContentView(R.layout.filehandler_dialog);
        handlerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        handlerDialog.getWindow().setBackgroundDrawableResource(R.drawable.round);
        decryptbtn = handlerDialog.findViewById(R.id.decryptButton);
        deleteBtn = handlerDialog.findViewById(R.id.deleteFileButton);
        dialogFileName = handlerDialog.findViewById(R.id.dialogFileName);
        //showDummyData();

        mRecyclerView.setAdapter(new RecyclerAdapter(data, new RecyclerAdapter.OnItemClickListener() {
            @Override public void onItemClick(FileModel item) {

            }
        }));

        userId = FirebaseAuth.getInstance().getUid();

        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));


        androidx.biometric.BiometricManager biometricManager = androidx.biometric.BiometricManager.from(this);


        Executor executor = ContextCompat.getMainExecutor(this);

        final BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
            }


            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                //showFileChooser();
                Toast.makeText(getApplicationContext(), "Login Success", Toast.LENGTH_SHORT).show();

            }
            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });

        final BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder().setTitle("Autentikuokite")
                .setDescription("Palieskite pirsto jutikli").setNegativeButtonText("Atsaukti").build();
        addbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  biometricPrompt.authenticate(promptInfo);


                showFileChooser();

            }
        });



        loadSavedFiles();
    }

    private void showDummyData() {
        data.add(new FileModel());
        data.add(new FileModel());
        data.add(new FileModel());
        data.add(new FileModel());
        data.add(new FileModel());
        data.add(new FileModel());
        data.add(new FileModel());
        data.add(new FileModel());
        data.add(new FileModel());
        data.add(new FileModel());
        mRecyclerView.setAdapter(new RecyclerAdapter(data, new RecyclerAdapter.OnItemClickListener() {
            @Override public void onItemClick(FileModel item) {

            }
        }));
    }
    private DatabaseReference uploadedFilesRef;
    private StorageReference storageReference ;
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateRecyclerView (FileModel fileModel, boolean addToDatabase) throws Exception {
        storageReference = FirebaseStorage.getInstance().getReference();

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Keliamas...");

        String encodedString = Base64.getEncoder().encodeToString(fileModel.getName().getBytes());
        StorageReference ref = storageReference.child("files").child(userId).child(encodedString);









        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));


        uploadedFilesRef = FirebaseDatabase.getInstance().getReference().child("savedFiles").child(userId).child(encodedString);
        if(addToDatabase){
            progressDialog.show();
           // saveFileToExternal(fileModel);
            ref.putFile(fileModel.getUri())
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            FileModel tmp = new FileModel();
                            tmp.setPath(fileModel.getPath());
                            tmp.setTimestamp(fileModel.getTimestamp());
                            tmp.setName(fileModel.getName());
                            tmp.setSize(fileModel.getSize());

                            uploadedFilesRef.setValue(tmp).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    recreate();

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(MainActivity.this, "Ivyko klaida" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            Toast.makeText(MainActivity.this, "Failed " + e.getMessage() ,Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot
                                    .getTotalByteCount());
                            progressDialog.setMessage("Ikelta " + (int) progress + "%");
                        }
                    });


        }
        else{
            data.add(fileModel);
            mRecyclerView.setAdapter(new RecyclerAdapter(data, new RecyclerAdapter.OnItemClickListener() {
                @Override public void onItemClick(FileModel item) {

                    dialogFileName.setText(item.getName());
                    handlerDialog.show();
                    deleteBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            performDeleting(item);
                        }
                    });
                    decryptbtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            performDecryption(item);
                        }
                    });

                }
            }));
        }

    }

    private void performDecryption(FileModel item) {
    }

    private void performDeleting(FileModel item) {
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == REQUEST_CODE_FILE  && resultCode  == RESULT_OK) {

                Log.d("REQUEST_CODE_FILE", data.toString());
                if(data != null){

                    Uri uri = data.getData();


                    String filename = getFileName(uri);
                    String fileExtension = Common.getMimeType(this, uri);
                    Log.d("REQUEST_CODE_FILE", "File filename: " + filename);
                    Log.d("REQUEST_CODE_FILE", "File fileExtension: " +  fileExtension);

                    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

                    File file = new File(uri.getPath());

                    String path = file.getPath();
                    //   Log.d("REQUEST_CODE_FILE", "File path: " +  PathUtil.getPath(getApplicationContext(), uri));
                    FileModel fileModel = new FileModel();
                    fileModel.setName(filename);
                    fileModel.setType(fileExtension);
                    fileModel.setTimestamp(timestamp);
                    fileModel.setSize(getFileSize(uri));
                    fileModel.setPath(path);
                    fileModel.setFile(file);
                    fileModel.setUri(uri);
                    updateRecyclerView(fileModel, true);
                }
            }
        } catch (Exception ex) {
            Toast.makeText(MainActivity.this, ex.toString(),
                    Toast.LENGTH_SHORT).show();
        }

    }

    private void saveFileToExternal(FileModel fileModel) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        File file = new File(fileModel.getUri().getPath());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            File extStore = Environment.getExternalStorageDirectory();
            FileInputStream fis = new FileInputStream(file.getPath());
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
    }


        public void loadSavedFiles() {
        uploadedFilesRef = FirebaseDatabase.getInstance().getReference().child("savedFiles").child(userId);
        uploadedFilesRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists()){

                    for(DataSnapshot ds : snapshot.getChildren()){

                        FileModel fm = ds.getValue(FileModel.class);



                            try {
                                updateRecyclerView(fm, false);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }


                    }
                }
           uploadedFilesRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public long getFileSize(Uri uri) {
        Cursor returnCursor = getContentResolver().
                query(uri, null, null, null, null);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();

        long bytes = returnCursor.getLong(sizeIndex);
        return bytes;
    }
    @SuppressLint("Range")
    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void showFileChooser(){

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
//        intent.setType("text/xml");   //XML file only
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), REQUEST_CODE_FILE);
        } catch (android.content.ActivityNotFoundException ex) {

            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }
}



