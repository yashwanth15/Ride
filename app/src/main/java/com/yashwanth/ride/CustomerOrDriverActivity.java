package com.yashwanth.ride;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class CustomerOrDriverActivity extends AppCompatActivity {

    private Button mCustomer,mRider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_or_driver);

        mCustomer=(Button)findViewById(R.id.customer);
        mRider=(Button)findViewById(R.id.rider);

        mCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(CustomerOrDriverActivity.this,CustomerMapActivity.class));
                finish();
            }
        });

        mRider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(CustomerOrDriverActivity.this,DriverMapActivity.class));
                finish();
            }
        });
    }
}
