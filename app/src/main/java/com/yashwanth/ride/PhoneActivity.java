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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
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

public class PhoneActivity extends AppCompatActivity {

    private EditText mPhoneNumber,mOTP;
    private Button mSendOTP,mVerifyOTP;
    private TextView mRideName1,mRideName2;

    private String mVerificationId,name,mUserId;

    private Uri resultUri;

    private boolean mVerificationInProgress = false;

    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private FirebaseAuth mAuth;

    private ProgressDialog dialog;

    private DatabaseReference mCustomerDatabase;
    private ImageView mProfileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        dialog = new ProgressDialog(PhoneActivity.this);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser()!=null){
            dialog.setMessage("Logging you in..");
            dialog.show();
            startActivity(new Intent(PhoneActivity.this,MainActivity.class));
            finish();
            return;
        }

        mPhoneNumber=(EditText)findViewById(R.id.phoneNumber);
        mOTP=(EditText)findViewById(R.id.OTPeditText);

        mSendOTP=(Button)findViewById(R.id.sendOTP);
        mVerifyOTP=(Button) findViewById(R.id.OTPVERIFY);

        mRideName1=(TextView)findViewById(R.id.rideName1);
        mRideName2=(TextView)findViewById(R.id.rideName2);



        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // Log.d(TAG, "onVerificationCompleted:" + credential);
                mVerificationInProgress = false;
                Toast.makeText(PhoneActivity.this,"Verification Complete",Toast.LENGTH_SHORT).show();
                dialog.setMessage("Logging you in..");
                dialog.show();
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // Log.w(TAG, "onVerificationFailed", e);
                Toast.makeText(PhoneActivity.this,"Verification Failed",Toast.LENGTH_SHORT).show();
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    Toast.makeText(PhoneActivity.this,"InValid Phone Number",Toast.LENGTH_SHORT).show();
                    // ...
                } else if (e instanceof FirebaseTooManyRequestsException) {
                }

            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // Log.d(TAG, "onCodeSent:" + verificationId);
                Toast.makeText(PhoneActivity.this,"Verification code has been send on your number",Toast.LENGTH_SHORT).show();
                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;
                mPhoneNumber.setVisibility(View.GONE);
                mSendOTP.setVisibility(View.GONE);
                mRideName1.setVisibility(View.GONE);

                mRideName2.setVisibility(View.VISIBLE);
                mOTP.setVisibility(View.VISIBLE);
                mVerifyOTP.setVisibility(View.VISIBLE);
                // ...
            }
        };

        mSendOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mPhoneNumber.getText())){
                    Toast.makeText(PhoneActivity.this, "Enter an OTP to verify", Toast.LENGTH_SHORT).show();
                }
                else{
                    try {
                        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                                mPhoneNumber.getText().toString(),
                                60,
                                java.util.concurrent.TimeUnit.SECONDS,
                                PhoneActivity.this,
                                mCallbacks);
                    }
                    catch (Exception e){
                        Toast.makeText(PhoneActivity.this, "Error!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        mVerifyOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mOTP.getText())){
                    Toast.makeText(PhoneActivity.this, "Enter an OTP to verify", Toast.LENGTH_SHORT).show();
                }
                else{
                    try {
                        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, mOTP.getText().toString());
                        // [END verify_with_code]
                        signInWithPhoneAuthCredential(credential);
                    }
                    catch(Exception e){
                        Toast.makeText(PhoneActivity.this, "Error!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });


    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Log.d(TAG, "signInWithCredential:success");
                            startActivity(new Intent(PhoneActivity.this,MainActivity.class));
                            finish();
                            return;
                            // ...
                        } else {
                            // Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                Toast.makeText(PhoneActivity.this,"Invalid Verification",Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    /*private void checkUserInfo() {
        String user_id=FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dialog!=null){
                    dialog.dismiss();
                }
                if (dataSnapshot.exists()){
                    startActivity(new Intent(PhoneActivity.this,CustomerOrDriverActivity.class));
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
                    Toast.makeText(PhoneActivity.this, "Please fill all the details!", Toast.LENGTH_SHORT).show();
                }
                else{
                    saveUserInformation();
                }
            }
        });

        dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
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

                    startActivity(new Intent(PhoneActivity.this,CustomerOrDriverActivity.class));
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
    }*/
}
