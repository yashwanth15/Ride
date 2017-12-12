package com.yashwanth.ride;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private DatabaseReference mCustomerDatabase;
    private ImageView mProfileImage;

    private String name,mUserId;

    private Uri resultUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser()!=null){
            checkUserInfo();
        }
        else{
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    //This code is run all 60seconds
                    Boolean myBooleanVar = true;
                    //If you want to operate UI modifications, you must run ui stuff on UiThread.
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(new Intent(MainActivity.this,PhoneActivity.class));
                        }
                    });
                }
            }, 3000);
        }
    }

    private void checkUserInfo() {
        String user_id=FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    startActivity(new Intent(MainActivity.this,CustomerOrDriverActivity.class));
                    finish();
                    return;
                }
                else{
                    enterUserInfo();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void enterUserInfo() {
        AlertDialog.Builder dialog=new AlertDialog.Builder(this);
        dialog.setTitle("USER INFORMATION!");
        dialog.setMessage("Enter your name and your picture");

        LayoutInflater inflater=LayoutInflater.from(this);
        View registerLayout=inflater.inflate(R.layout.activity_user_info,null);

        mProfileImage=registerLayout.findViewById(R.id.profileImage);
        final MaterialEditText mName=registerLayout.findViewById(R.id.name);

        mUserId=mAuth.getCurrentUser().getUid();
        mCustomerDatabase= FirebaseDatabase.getInstance().getReference().child("Users").child(mUserId);

        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });

        dialog.setView(registerLayout);

        dialog.setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                name=mName.getText().toString();
                if (mName==null&&resultUri==null){
                    Toast.makeText(MainActivity.this, "Please fill all the details!", Toast.LENGTH_SHORT).show();
                }
                else{
                    saveUserInformation();
                }
            }
        });

        dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mAuth.signOut();
                dialog.dismiss();
                startActivity(new Intent(MainActivity.this,PhoneActivity.class));
                finish();
                return;
            }
        });
    }

    private void saveUserInformation(){

        Map userInfo=new HashMap();
        userInfo.put("name",name);

        mCustomerDatabase.updateChildren(userInfo);

        if (resultUri!=null){
            StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile_images").child(mUserId);

            Bitmap bitmap=null;
            try {
                bitmap= MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,20, baos);
            byte[] data=baos.toByteArray();
            UploadTask uploadTask=filePath.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    finish();
                    return;
                }
            });

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl=taskSnapshot.getDownloadUrl();

                    Map imageMap=new HashMap();
                    imageMap.put("profile_image",downloadUrl.toString());
                    mCustomerDatabase.updateChildren(imageMap);

                    startActivity(new Intent(MainActivity.this,CustomerOrDriverActivity.class));
                    finish();
                }
            });

        }
        else{
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==1&&resultCode== Activity.RESULT_OK){
            final Uri imageUri=data.getData();
            resultUri=imageUri;
            mProfileImage.setImageURI(resultUri);
        }
    }
}
