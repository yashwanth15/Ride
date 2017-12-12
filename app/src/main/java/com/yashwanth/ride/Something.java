package com.yashwanth.ride;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;

public class Something extends AppCompatActivity {

    private Button mLogout;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_something);

        mLogout=(Button)findViewById(R.id.logout);

        mAuth=FirebaseAuth.getInstance();

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
                String CustomerOrRider=getIntent().getStringExtra("CustomerOrRider");
                if (CustomerOrRider.equals("rider")){
                    RiderMapActivity.riderMapActivityObject.finish();
                }
                else if (CustomerOrRider.equals("customer")){
                    CustomerMapActivity.CustomerMapActivityObject.finish();
                }
                startActivity(new Intent(Something.this,MainActivity.class));
                finish();
            }
        });
    }
}
