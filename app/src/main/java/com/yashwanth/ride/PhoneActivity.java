package com.yashwanth.ride;

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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class PhoneActivity extends AppCompatActivity {

    private EditText mPhoneNumber,mOTP;
    private Button mSendOTP,mVerifyOTP;

    private String mVerificationId;

    private boolean mVerificationInProgress = false;

    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        mPhoneNumber=(EditText)findViewById(R.id.phoneNumber);
        mOTP=(EditText)findViewById(R.id.OTPeditText);

        mSendOTP=(Button)findViewById(R.id.sendOTP);
        mVerifyOTP=(Button) findViewById(R.id.OTPVERIFY);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser()!=null){
            startActivity(new Intent(this,CustomerOrDriverActivity.class));
            finish();
            return;
        }

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // Log.d(TAG, "onVerificationCompleted:" + credential);
                mVerificationInProgress = false;
                Toast.makeText(PhoneActivity.this,"Verification Complete",Toast.LENGTH_SHORT).show();

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
                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                        mPhoneNumber.getText().toString(),
                        60,
                        java.util.concurrent.TimeUnit.SECONDS,
                        PhoneActivity.this,
                        mCallbacks);
            }
        });

        mVerifyOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, mOTP.getText().toString());
                // [END verify_with_code]
                signInWithPhoneAuthCredential(credential);
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
                            startActivity(new Intent(PhoneActivity.this,CustomerOrDriverActivity.class));
                            Toast.makeText(PhoneActivity.this,"Verification Done",Toast.LENGTH_SHORT).show();
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
}
