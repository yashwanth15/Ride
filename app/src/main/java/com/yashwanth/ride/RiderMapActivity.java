package com.yashwanth.ride;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import static android.app.PendingIntent.getActivity;
import static com.yashwanth.ride.R.id.map;
import static com.yashwanth.ride.R.id.visible;

public class RiderMapActivity extends AppCompatActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener, RoutingListener {

    static RiderMapActivity riderMapActivityObject;

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    SupportMapFragment mapFragment;

    private String user_id;

    private LatLng latlngDestination;

    private Marker destinationMarker;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mToggle;

    private Toolbar mToolbar;

    private Switch mStatus;

    private TextView mStatusLabel;

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.black};

    private CardView mBottomCardView;
    private LinearLayout mYesOrNo;
    private Button mYes,mNo;

    private Boolean mRequestAccepted=false;

    private DatabaseReference driverWorkingRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(RiderMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }else{
            mapFragment.getMapAsync(this);
        }

        riderMapActivityObject =this;

        polylines = new ArrayList<>();

        mStatus=(Switch)findViewById(R.id.status);

        mStatusLabel=(TextView)findViewById(R.id.statusLabel);

        user_id= FirebaseAuth.getInstance().getCurrentUser().getUid();

        mBottomCardView=(CardView)findViewById(R.id.bottomCardView);
        mYesOrNo=(LinearLayout) findViewById(R.id.yesOrNo);
        mYes=(Button) findViewById(R.id.yes);
        mNo=(Button)findViewById(R.id.no);


        mStatus.setChecked(false);
        mStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    if (latlngDestination==null){
                        mStatus.setChecked(false);
                        Toast.makeText(RiderMapActivity.this, "Select your destination first", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        //rider is ONLINE
                        addRiderRoute();
                        mStatusLabel.setText("Online");
                        getAssignedCustomer();
                    }
                }
                else{
                    DatabaseReference locationref = FirebaseDatabase.getInstance().getReference("driversAvailableLocation").child(user_id);
                    DatabaseReference destinationref = FirebaseDatabase.getInstance().getReference("driversAvailableDestination").child(user_id);
                    locationref.removeValue();
                    destinationref.removeValue();

                    mStatusLabel.setText("Offline");
                    if (destinationMarker!=null){
                        destinationMarker.remove();
                    }
                    if (latlngDestination!=null){
                        latlngDestination=null;
                    }
                    eraseRoute();
                }
            }
        });




        //PLACEAUTOCOMPLETE FRAGMENT
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                latlngDestination=place.getLatLng();
                if (destinationMarker!=null){
                    destinationMarker.remove();
                }
                if (mStatus.isChecked()){
                    addRiderRoute();
                }
                destinationMarker=mMap.addMarker(new MarkerOptions().position(latlngDestination).title("destination"));
                LatLng latlngLocation=new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());
                getRouteToMarker(latlngLocation,latlngDestination);
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Toast.makeText(RiderMapActivity.this, ""+status, Toast.LENGTH_SHORT).show();
                if (destinationMarker!=null){
                    destinationMarker.remove();
                }
                if (latlngDestination!=null){
                    latlngDestination=null;
                }
            }
        });



        //NACIGATION SIDE BAR CODE
        mToolbar=(Toolbar)findViewById(R.id.nav_no_action);
        setSupportActionBar(mToolbar);

        mDrawerLayout=(DrawerLayout)findViewById(R.id.drawLayout);
        mToggle=new ActionBarDrawerToggle(this,mDrawerLayout,R.string.open,R.string.close);

        mDrawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        NavigationView nv = (NavigationView)findViewById(R.id.navigationMenu);

        nv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case(R.id.settingsNavigation):
                        Intent intent = new Intent(getApplicationContext(),Something.class);
                        intent.putExtra("CustomerOrRider","rider");
                        startActivity(intent);
                        break;
                    /*case(R.id.freeRidesNavigation):
                        Intent intent1 = new Intent(getApplicationContext(),.class);
                        startActivity(intent1);
                        break;
                    case(R.id.paymentNavigation):
                        Intent intent2 = new Intent(getApplicationContext(),.class);
                        startActivity(intent2);
                        break;
                    case(R.id.yourTripsNavigation):
                        Intent intent3 = new Intent(getApplicationContext(),.class);
                        startActivity(intent3);
                        break;*/
                }
                return true;
            }
        });

        mYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRequestAccepted=true;
                countDownTimer.cancel();
                driverWorkingRef=FirebaseDatabase.getInstance().getReference().child("drivers_working");
                GeoFire geoFireLocation=new GeoFire(driverWorkingRef);
                geoFireLocation.setLocation(user_id,new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
                mBottomCardView.setVisibility(View.GONE);

                DatabaseReference locationref = FirebaseDatabase.getInstance().getReference("driversAvailableLocation").child(user_id);
                DatabaseReference destinationref = FirebaseDatabase.getInstance().getReference("driversAvailableDestination").child(user_id);
                locationref.removeValue();
                destinationref.removeValue();
                if (assignedCustomerRef!=null){
                    assignedCustomerRef.removeEventListener(assignedCustomerListener);
                }
            }
        });
        mNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRequestAccepted=false;
                requestCancled();
            }
        });
    }

    private void requestCancled() {
        if (pickupMarker!=null){
            pickupMarker.remove();
        }
        if (customerDestinationMarker!=null){
            customerDestinationMarker.remove();
        }
        customerId=null;
        eraseRoute();
        mBottomCardView.setVisibility(View.GONE);
        getAssignedCustomer();
        driverWorkingRef=FirebaseDatabase.getInstance().getReference().child("drivers_working").child(user_id);
        driverWorkingRef.removeValue();
        addRiderRoute();
        if (customerCancelRef!=null){
            customerCancelRef.removeEventListener(customerCancelListener);
        }
    }

    String customerId;
    DatabaseReference assignedCustomerRef;
    ValueEventListener assignedCustomerListener;
    CountDownTimer countDownTimer;
    private void getAssignedCustomer() {
        assignedCustomerRef=FirebaseDatabase.getInstance().getReference().child("customer_request").child(user_id);
        assignedCustomerListener=assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()&&customerId==null){
                    ifRequestCanceled();
                    customerId=dataSnapshot.getKey();
                    getAssignedCustomerLocation();
                    mBottomCardView.setVisibility(View.VISIBLE);
                    if (assignedCustomerRef!=null){
                        assignedCustomerRef.removeEventListener(assignedCustomerListener);
                    }
                    countDownTimer=new CountDownTimer(40000, 1000) {

                        public void onTick(long millisUntilFinished) {

                        }

                        public void onFinish() {
                            if (!mRequestAccepted){
                                requestCancled();
                            }
                        }
                    }.start();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    DatabaseReference customerCancelRef;
    ValueEventListener customerCancelListener;
    private void ifRequestCanceled() {
        customerCancelRef=FirebaseDatabase.getInstance().getReference().child("request_cancel").child(user_id);
        customerCancelListener=customerCancelRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    customerCancelRef.removeValue();
                    Toast.makeText(RiderMapActivity.this, "Customer cancelled the ride", Toast.LENGTH_SHORT).show();
                    requestCancled();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private LatLng customerLocationLatlng;
    private Marker pickupMarker;
    private void getAssignedCustomerLocation() {
        DatabaseReference customerLocationRef=FirebaseDatabase.getInstance().getReference().child("customer_location").child(customerId).child("location").child("l");
        customerLocationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    List<Object> map=(List<Object>) dataSnapshot.getValue();
                    double locationLat=0;
                    double locationLng=0;

                    if (map.get(0)!=null){
                        locationLat=Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1)!=null){
                        locationLng=Double.parseDouble(map.get(1).toString());
                    }
                    customerLocationLatlng=new LatLng(locationLat,locationLng);
                    pickupMarker=mMap.addMarker(new MarkerOptions().position(customerLocationLatlng).title("Pickup Location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.pickup_location)));
                    getAssignedCustomerDestination();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private LatLng customerDestinationLatlng;
    private Marker customerDestinationMarker;
    private void getAssignedCustomerDestination() {
        DatabaseReference customerLocationRef=FirebaseDatabase.getInstance().getReference().child("customer_location").child(customerId).child("destination").child("l");
        customerLocationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    List<Object> map=(List<Object>) dataSnapshot.getValue();
                    double locationLat=0;
                    double locationLng=0;

                    if (map.get(0)!=null){
                        locationLat=Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1)!=null){
                        locationLng=Double.parseDouble(map.get(1).toString());
                    }
                    customerDestinationLatlng=new LatLng(locationLat,locationLng);
                    customerDestinationMarker=mMap.addMarker(new MarkerOptions().position(customerDestinationLatlng).title("Customer Destination").icon(BitmapDescriptorFactory.fromResource(R.mipmap.destination)));
                    getRouteToMarker(customerLocationLatlng,customerDestinationLatlng);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getRouteToMarker(LatLng latlngLocation,LatLng latlngDestination) {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(latlngLocation, latlngDestination)
                .build();
        routing.execute();
    }

    public void eraseRoute(){
        for (Polyline line:polylines){
            line.remove();
        }
        polylines.clear();
    }

    //ON ITEMS SELECTED IN NAVIGATION
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mToggle.onOptionsItemSelected(item)){
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(RiderMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient=new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation =location;

        LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        if (mStatus.isChecked()&&latlngDestination!=null){
            if (customerId==null){
                DatabaseReference locationref = FirebaseDatabase.getInstance().getReference("driversAvailableLocation");
                GeoFire geoFireLocation=new GeoFire(locationref);
                geoFireLocation.setLocation(user_id,new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
            }
            else{
                DatabaseReference driverWorkingRef=FirebaseDatabase.getInstance().getReference().child("drivers_working");
                GeoFire geoFireLocation=new GeoFire(driverWorkingRef);
                geoFireLocation.setLocation(user_id,new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(100000);
        mLocationRequest.setFastestInterval(100000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(RiderMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    final int LOCATION_REQUEST_CODE = 1;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case LOCATION_REQUEST_CODE:{
                if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mapFragment.getMapAsync(this);
                } else{
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {

    }

    private void addRiderRoute(){
        DatabaseReference locationref = FirebaseDatabase.getInstance().getReference("driversAvailableLocation");
        DatabaseReference destinationref = FirebaseDatabase.getInstance().getReference("driversAvailableDestination");

        GeoFire geoFireLocation=new GeoFire(locationref);
        GeoFire geoFireDestination=new GeoFire(destinationref);
        geoFireLocation.setLocation(user_id,new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
        geoFireDestination.setLocation(user_id,new GeoLocation(latlngDestination.latitude,latlngDestination.longitude));
    }
}
