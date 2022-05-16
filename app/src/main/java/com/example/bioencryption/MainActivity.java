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


import java.io.File;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.Executor;


import javax.crypto.NoSuchPaddingException;


public class MainActivity extends AppCompatActivity {
    private FloatingActionButton addbtn;
    private int REQUEST_CODE_FILE = 205;
    private RecyclerView mRecyclerView;
    private MaterialButton decryptbtn, deleteBtn;
    private ArrayList<FileModel> data = new ArrayList<>();
    private Dialog handlerDialog;
    private TextView dialogFileName;
    private String userId;
    private String actionType = "";
    private  String kelias;
    private BiometricPrompt biometricPrompt;
    private  BiometricPrompt.PromptInfo promptInfo;
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

         biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(MainActivity.this, "Autentikavimo klaida" + errString, Toast.LENGTH_SHORT).show();
            }


            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                if(actionType.equals("ADD_FILE")){
                    showFileChooser();
                }
                else if(actionType.equals("DECRYPT_FILE")){
                    try {
                        EncryptUtils.decrypt(getApplicationContext(), globalFile);
                        DatabaseReference fileRef = FirebaseDatabase.getInstance().getReference().child("savedFiles").child(userId).child(globalFile.getBase64id());
                        fileRef.removeValue();
                        handlerDialog.hide();
                        recreate();
                        Toast.makeText(MainActivity.this, "Sekmingai issifruotas failas.", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();

                        Toast.makeText(MainActivity.this, "Klaida" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();

                        Toast.makeText(MainActivity.this, "Klaida" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    } catch (NoSuchPaddingException e) {
                        e.printStackTrace();

                        Toast.makeText(MainActivity.this, "Klaida" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();

                        Toast.makeText(MainActivity.this, "Klaida" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    } catch (InvalidKeySpecException e) {
                        e.printStackTrace();
                    }
                }


            }
            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(MainActivity.this, "Autentikavimo klaida", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder().setTitle("Autentikuokite")
                .setDescription("Palieskite pirsto jutikli").setNegativeButtonText("Atsaukti").build();
        addbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               biometricPrompt.authenticate(promptInfo);
                actionType="ADD_FILE";



            }
        });

         kelias=getExternalFilesDir(null) + "/encrypted";
        File folder = new File(kelias);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        if(success){
           // Toast.makeText(this,"Sukurta "+folder.getAbsolutePath(),Toast.LENGTH_LONG).show();
        } else{
          //  Toast.makeText(this,"Nesukurta" +folder.getAbsolutePath(),Toast.LENGTH_LONG).show();
        }

        loadSavedFiles();
        try {
            createEmptyFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void createEmptyFiles() throws IOException {

        File file = new File(kelias, "testas.txt");
        File f1 = new File(kelias, "foto.png");
        File f3 = new File(kelias, "doc.pdf");
        File f2 = new File(kelias, "foto.jpeg");
        File f5 = new File(kelias, "video.mp4");

        f1.createNewFile();
        f3.createNewFile();
        f2.createNewFile();
        f5.createNewFile();
        FileWriter fw;


        try {
            fw = new FileWriter(file);
            fw.write("test");
            fw.close();

        } catch (IOException e) {
            Toast.makeText(this,"Nepavyko issaugoti",Toast.LENGTH_SHORT).show();

        }
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
    private  ProgressDialog progressDialog;
    private FileModel globalFile;
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateRecyclerView (FileModel fileModel, boolean addToDatabase) throws Exception {
        storageReference = FirebaseStorage.getInstance().getReference();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Keliamas...");

        String encodedString = Base64.getEncoder().encodeToString(fileModel.getName().getBytes());
        StorageReference ref = storageReference.child("files").child(userId).child(encodedString);









        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));


        uploadedFilesRef = FirebaseDatabase.getInstance().getReference().child("savedFiles").child(userId).child(encodedString);
        if(addToDatabase){
            progressDialog.show();


            ref.putFile(fileModel.getUri())
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            FileModel tmp = new FileModel();
                            tmp.setPath(fileModel.getPath());
                            tmp.setTimestamp(fileModel.getTimestamp());
                            tmp.setName(fileModel.getName());
                            tmp.setSize(fileModel.getSize());
                            tmp.setBase64id(encodedString);
                            tmp.setType(fileModel.getType());
                            try {
                                EncryptUtils.encrypt(getApplicationContext(), tmp);
                                recreate();
                            } catch (IOException e) {
                                e.printStackTrace();
                                progressDialog.dismiss();
                                Toast.makeText(MainActivity.this, "Klaida" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                                progressDialog.dismiss();
                                Toast.makeText(MainActivity.this, "Klaida" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            } catch (NoSuchPaddingException e) {
                                e.printStackTrace();
                                progressDialog.dismiss();
                                Toast.makeText(MainActivity.this, "Klaida" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            } catch (InvalidKeyException e) {
                                e.printStackTrace();
                                progressDialog.dismiss();
                                Toast.makeText(MainActivity.this, "Klaida" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            } catch (InvalidKeySpecException e) {
                                e.printStackTrace();
                            }
                            uploadedFilesRef.setValue(tmp).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {




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
                    DatabaseReference fileRef = FirebaseDatabase.getInstance().getReference().child("savedFiles").child(userId).child(item.getBase64id());
                    deleteBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                    fileRef.removeValue();
                            String [] fileNameArr = item.getName().split("\\.");
                            File file = new File(kelias + "/" +fileNameArr[0]+"-encrypted");
                            if(file.exists()){
                                file.delete();
                            }
                    handlerDialog.hide();
                    recreate();
                        }
                    });
                    decryptbtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            actionType="DECRYPT_FILE";
                            biometricPrompt.authenticate(promptInfo);
                        globalFile = item;
                        }
                    });

                }
            }));
        }

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
        handlerDialog.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();
        for(FileModel fm : data){
            DatabaseReference fileRef = FirebaseDatabase.getInstance().getReference().child("savedFiles").child(userId).child(fm.getBase64id());
            if(!checkIfFileIsInStorage(fm)){
                recreate();
            }
        }
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




        public void loadSavedFiles() {
        uploadedFilesRef = FirebaseDatabase.getInstance().getReference().child("savedFiles").child(userId);
        uploadedFilesRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists()){

                    for(DataSnapshot ds : snapshot.getChildren()){

                        FileModel fm = ds.getValue(FileModel.class);

                        assert fm != null;


                            try {
                                if(  checkIfFileIsInStorage(fm)) {
                                    updateRecyclerView(fm, false);
                                }
                                else{
                                    DatabaseReference fileRef = FirebaseDatabase.getInstance().getReference().child("savedFiles").child(userId).child(fm.getBase64id());
                                    fileRef.removeValue();
                                }
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

    private boolean checkIfFileIsInStorage(FileModel fileModel) {
        String [] fileNameArr = fileModel.getName().split("\\.");
        File file = new File(kelias + "/" +fileNameArr[0]+"-encrypted");
        return file.exists();
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

        Uri targetUri = Uri.parse(getExternalFilesDir(null) + "/encrypted");
//        intent.setType("text/xml");   //XML file only
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setDataAndType(targetUri, "*/*");

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), REQUEST_CODE_FILE);
        } catch (android.content.ActivityNotFoundException ex) {

            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }
}




