package com.yashwanth.ride;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CustomerOrDriverActivity extends AppCompatActivity {

    private Button mCustomer,mRider;

    private FirebaseAuth firebaseAuth;

    private DatabaseReference mCustomerDatabase;

    private String mUserId;

    private Boolean flag=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_or_driver);

        mCustomer=(Button)findViewById(R.id.customer);
        mRider=(Button)findViewById(R.id.rider);

        firebaseAuth=FirebaseAuth.getInstance();
        mUserId=firebaseAuth.getCurrentUser().getUid();

        mCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            startActivity(new Intent(CustomerOrDriverActivity.this,CustomerMapActivity.class));
            finish();
            return;
            }
        });

        mRider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flag){
                    startActivity(new Intent(CustomerOrDriverActivity.this,RiderMapActivity.class));
                    finish();
                    return;
                }
                else{
                    startActivity(new Intent(CustomerOrDriverActivity.this,RiderInfoActivity.class));
                    finish();
                    return;
                }
            }
        });

        checkRiderInfo();
    }

    private void checkRiderInfo() {
        mCustomerDatabase= FirebaseDatabase.getInstance().getReference().child("Verify_Riders").child(mUserId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    flag=true;
                }
                else{
                    flag=false;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
