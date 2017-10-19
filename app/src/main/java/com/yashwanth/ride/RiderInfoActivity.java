package com.yashwanth.ride;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RiderInfoActivity extends AppCompatActivity {

    private Button mSubmit;

    private ImageView vehicleImageView,RCImageView,LicenseImageView,aadharImageView;

    private Uri resultUri,vehicleUri,RCUri,licenseUri,aadharUri;

    private FirebaseAuth firebaseAuth;

    private DatabaseReference mCustomerDatabase;

    private String mUserId,mname,mprofileImageUrl,vehicle;

    private RadioGroup radioVehicle;
    private RadioButton radioButtonVehicle;

    private Boolean vehicleBoolean=false,RCBoolean=false,licenseBoolean=false,aadharBoolean=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_info);

        mSubmit=(Button)findViewById(R.id.submit);

        radioVehicle=(RadioGroup)findViewById(R.id.radioVehicle);

        vehicleImageView=(ImageView)findViewById(R.id.vehiclePicture);
        RCImageView=(ImageView)findViewById(R.id.RCPicture);
        LicenseImageView=(ImageView)findViewById(R.id.licensePicture);
        aadharImageView=(ImageView)findViewById(R.id.aadharPicture);

        radioVehicle.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                radioButtonVehicle=(RadioButton)findViewById(i);
                vehicle=radioButtonVehicle.getText().toString();
            }
        });


        firebaseAuth=FirebaseAuth.getInstance();
        mUserId=firebaseAuth.getCurrentUser().getUid();

        mCustomerDatabase= FirebaseDatabase.getInstance().getReference().child("Verify_Riders").child(mUserId);

        //getUserInfo();

        vehicleImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vehicleBoolean=true;
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });
        RCImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RCBoolean=true;
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });
        LicenseImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                licenseBoolean=true;
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });
        aadharImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                aadharBoolean=true;
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });

        mSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (vehicle==null||vehicleUri==null||RCUri==null||licenseUri==null||aadharUri==null){
                    Toast.makeText(RiderInfoActivity.this, "please fill all the fields", Toast.LENGTH_SHORT).show();
                }
                else{
                    saveUserInformation();
                }
            }
        });

    }



    /*private void getUserInfo(){
        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()&&dataSnapshot.getChildrenCount()>0){
                    Map<String,Object> map=(Map<String,Object>) dataSnapshot.getValue();
                    if (map.get("vehicle")!=null){
                        vehicle=map.get("vehicle").toString();
                        radioVehicle.check(Integer.parseInt(vehicle));
                    }
                    if (map.get("profileImageUrl")!=null){
                        mprofileImageUrl=map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(mprofileImageUrl).into(mProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }*/


    private void saveUserInformation(){

        ProgressDialog dialog = new ProgressDialog(RiderInfoActivity.this);
        dialog.setMessage("Saving your Information..");
        dialog.show();

        Map userInfo=new HashMap();
        userInfo.put("vehicle",vehicle);

        mCustomerDatabase.updateChildren(userInfo);

        if (resultUri!=null){
            StorageReference filePathVehicle = FirebaseStorage.getInstance().getReference().child("Rider_details").child(mUserId).child("vehicle_image");
            StorageReference filePathRC = FirebaseStorage.getInstance().getReference().child("Rider_details").child(mUserId).child("RC_image");
            StorageReference filePathLicense = FirebaseStorage.getInstance().getReference().child("Rider_details").child(mUserId).child("license_image");
            StorageReference filePathAadhar = FirebaseStorage.getInstance().getReference().child("Rider_details").child(mUserId).child("aadhar_image");

            Bitmap bitmapVehicle=null,bitmapRC=null,bitmapLicense=null,bitmapAadhar=null;
            try {
                bitmapVehicle= MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), vehicleUri);
                bitmapRC= MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), RCUri);
                bitmapLicense= MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), licenseUri);
                bitmapAadhar= MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), aadharUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ByteArrayOutputStream baosVehicle=new ByteArrayOutputStream();
            ByteArrayOutputStream baosRC=new ByteArrayOutputStream();
            ByteArrayOutputStream baosLicense=new ByteArrayOutputStream();
            ByteArrayOutputStream baosAadhar=new ByteArrayOutputStream();

            bitmapVehicle.compress(Bitmap.CompressFormat.JPEG,20, baosVehicle);
            bitmapRC.compress(Bitmap.CompressFormat.JPEG,20, baosRC);
            bitmapLicense.compress(Bitmap.CompressFormat.JPEG,20, baosLicense);
            bitmapAadhar.compress(Bitmap.CompressFormat.JPEG,20, baosAadhar);

            byte[] dataVehicle=baosVehicle.toByteArray();
            byte[] dataRC=baosRC.toByteArray();
            byte[] dataLicense=baosLicense.toByteArray();
            byte[] dataAadhar=baosAadhar.toByteArray();

            UploadTask uploadTaskVehicle=filePathVehicle.putBytes(dataVehicle);
            UploadTask uploadTaskRC=filePathRC.putBytes(dataRC);
            UploadTask uploadTaskLicense=filePathLicense.putBytes(dataLicense);
            UploadTask uploadTaskAadhar=filePathAadhar.putBytes(dataAadhar);


            //uploadTask task failureListener
            uploadTaskVehicle.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    finish();
                    return;
                }
            });
            uploadTaskRC.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    finish();
                    return;
                }
            });
            uploadTaskLicense.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    finish();
                    return;
                }
            });
            uploadTaskAadhar.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    finish();
                    return;
                }
            });


            //uploadTask task successListener
            uploadTaskVehicle.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl=taskSnapshot.getDownloadUrl();

                    Map imageMap=new HashMap();
                    imageMap.put("vehicle_image",downloadUrl.toString());
                    mCustomerDatabase.updateChildren(imageMap);
                }
            });
            uploadTaskRC.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl=taskSnapshot.getDownloadUrl();

                    Map imageMap=new HashMap();
                    imageMap.put("RC_image",downloadUrl.toString());
                    mCustomerDatabase.updateChildren(imageMap);
                }
            });
            uploadTaskLicense.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl=taskSnapshot.getDownloadUrl();

                    Map imageMap=new HashMap();
                    imageMap.put("license_image",downloadUrl.toString());
                    mCustomerDatabase.updateChildren(imageMap);
                }
            });
            uploadTaskAadhar.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl=taskSnapshot.getDownloadUrl();

                    Map imageMap=new HashMap();
                    imageMap.put("aadhar_image",downloadUrl.toString());
                    mCustomerDatabase.updateChildren(imageMap);
                }
            });
            dialog.dismiss();
            startActivity(new Intent(RiderInfoActivity.this,RiderMapActivity.class));
            finish();
        }
        else{
            dialog.dismiss();
            finish();
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==1&&resultCode== Activity.RESULT_OK){
            final Uri imageUri=data.getData();
            resultUri=imageUri;
            if (vehicleBoolean){
                vehicleBoolean=false;
                vehicleUri=resultUri;
                vehicleImageView.setImageURI(resultUri);
            }
            else if (RCBoolean){
                RCBoolean=false;
                RCUri=resultUri;
                RCImageView.setImageURI(resultUri);
            }
            else if (licenseBoolean){
                licenseBoolean=false;
                licenseUri=resultUri;
                LicenseImageView.setImageURI(resultUri);
            }
            else if (aadharBoolean){
                aadharBoolean=false;
                aadharUri=resultUri;
                aadharImageView.setImageURI(resultUri);
            }
        }
    }
}
