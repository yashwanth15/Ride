package com.yashwanth.ride;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
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

public class PhoneActivity extends AppCompatActivity {

    private EditText mPhoneNumber,mOTP;
    private Button mSendOTP,mVerifyOTP;

    private String mVerificationId;

    private boolean mVerificationInProgress = false;

    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private FirebaseAuth mAuth;

    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        dialog = new ProgressDialog(PhoneActivity.this);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser()!=null){
            dialog.setMessage("Logging you in..");
            dialog.show();
            checkUserInfo();
        }

        mPhoneNumber=(EditText)findViewById(R.id.phoneNumber);
        mOTP=(EditText)findViewById(R.id.OTPeditText);

        mSendOTP=(Button)findViewById(R.id.sendOTP);
        mVerifyOTP=(Button) findViewById(R.id.OTPVERIFY);



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
                mOTP.setVisibility(View.VISIBLE);
                mVerifyOTP.setVisibility(View.VISIBLE);
                // ...
            }
        };

        mSendOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPhoneNumber.getText()==null){
                    Toast.makeText(PhoneActivity.this, "Enter an OTP to verify", Toast.LENGTH_SHORT).show();
                }
                else{
                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                            mPhoneNumber.getText().toString(),
                            60,
                            java.util.concurrent.TimeUnit.SECONDS,
                            PhoneActivity.this,
                            mCallbacks);
                }
            }
        });

        mVerifyOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOTP.getText()==null){
                    Toast.makeText(PhoneActivity.this, "Enter an OTP to verify", Toast.LENGTH_SHORT).show();
                }
                else{
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, mOTP.getText().toString());
                    // [END verify_with_code]
                    signInWithPhoneAuthCredential(credential);
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
                            checkUserInfo();
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

    private void checkUserInfo() {
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
                    startActivity(new Intent(PhoneActivity.this,UserInfoActivity.class));
                    finish();
                    return;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
}
