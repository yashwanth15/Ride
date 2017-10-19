package com.yashwanth.ride;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.R.attr.radius;
import static com.yashwanth.ride.R.id.map;

public class CustomerMapActivity extends AppCompatActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener, RoutingListener, GoogleMap.OnMarkerClickListener {

    static CustomerMapActivity CustomerMapActivityObject;
    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    SupportMapFragment mapFragment;

    private LatLng latlngDestination;

    private Marker destinationMarker;

    private LatLng destinationLatlng;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mToggle;

    private Toolbar mToolbar;

    private Button mBook;

    private String mUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }else{
            mapFragment.getMapAsync(this);
        }

        CustomerMapActivityObject=this;

        mBook=(Button)findViewById(R.id.book);

        mUserId=FirebaseAuth.getInstance().getCurrentUser().getUid();

        polylines = new ArrayList<>();

        //PLACEAUTOCOMPLETE FRAGMENT
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                latlngDestination=place.getLatLng();
                destinationLatlng=place.getLatLng();
                destinationMarker=mMap.addMarker(new MarkerOptions().position(latlngDestination).title("destination"));
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Toast.makeText(CustomerMapActivity.this, ""+status, Toast.LENGTH_SHORT).show();
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
                        intent.putExtra("CustomerOrRider","customer");
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
    }


    //ON ITEMS SELECTED IN NAVIGATION
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mToggle.onOptionsItemSelected(item)){
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private int radius=1;
    private String driverFoundId;
    private GeoQuery geoQuery;
    private Map<String,LatLng> markersLocation=new HashMap<String, LatLng>();
    public void getCloseDrivers(){
        DatabaseReference ref=FirebaseDatabase.getInstance().getReference().child("driversAvailableLocation");
        GeoFire geoFire=new GeoFire(ref);
        geoQuery=geoFire.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()),radius);

        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                driverFoundId=key;
                getRiderLocation(driverFoundId);

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (radius<100){
                    radius++;
                    getCloseDrivers();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }



    private Marker mDriverLocationMarker;
    private void getRiderLocation(final  String driverFoundId){
        DatabaseReference driverLocationRef=FirebaseDatabase.getInstance().getReference().child("driversAvailableLocation").child(driverFoundId).child("l");
        driverLocationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()&&!markersLocation.containsKey(driverFoundId)){
                    List<Object> map=(List<Object>) dataSnapshot.getValue();
                    double locationLat=0;
                    double locationLng=0;

                    if (map.get(0)!=null){
                        locationLat=Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1)!=null){
                        locationLng=Double.parseDouble(map.get(1).toString());
                    }
                    final LatLng driverLocationLatLng=new LatLng(locationLat,locationLng);
                    DatabaseReference ref=FirebaseDatabase.getInstance().getReference().child("Verify_Riders").child(driverFoundId).child("vehicle");
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()){
                                String vehicleType=dataSnapshot.getValue().toString();
                                if (vehicleType.equals("Car")){
                                    mDriverLocationMarker=mMap.addMarker(new MarkerOptions().position(driverLocationLatLng).title("Driver Location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.car_icon)));
                                }
                                else if(vehicleType.equals("MotorCycle")){
                                    mDriverLocationMarker=mMap.addMarker(new MarkerOptions().position(driverLocationLatLng).title("Rider Location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.rider_location)));
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
                    markersLocation.put(driverFoundId,driverLocationLatLng);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    public void getRiderRoute(LatLng start,LatLng end) {

        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(start, end)
                .build();
        routing.execute();
    }

    public void eraseRoute(){
        for (Polyline line:polylines){
            line.remove();
        }
        polylines.clear();
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
        mMap.setOnMarkerClickListener(this);


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
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
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12),2000,null);

        getCloseDrivers();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(100000);
        mLocationRequest.setFastestInterval(100000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
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


    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.black};

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
        /*if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }*/

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

    @Override
    public boolean onMarkerClick(Marker marker) {
        for (final Map.Entry<String, LatLng> entry : markersLocation.entrySet())
        {
            if (marker.getPosition().equals(entry.getValue())){
                getDriverDestination(entry.getKey(),entry.getValue());
                mBook.setVisibility(View.VISIBLE);
                mBook.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (latlngDestination==null){
                            Toast.makeText(CustomerMapActivity.this, "Please enter your destination first!", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            bookRider(entry.getKey());
                        }
                    }
                });
            }
        }
        return false;
    }

    private void bookRider(String key) {
        DatabaseReference customerLocationRef=FirebaseDatabase.getInstance().getReference().child("customer_request").child(key).child("location");
        DatabaseReference customerDestinationRef=FirebaseDatabase.getInstance().getReference().child("customer_request").child(key).child("destination");

        GeoFire geoFireLocation=new GeoFire(customerLocationRef);
        GeoFire geoFireDestination=new GeoFire(customerDestinationRef);

        geoFireLocation.setLocation(mUserId,new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
        geoFireDestination.setLocation(mUserId,new GeoLocation(latlngDestination.latitude,latlngDestination.longitude));
        mBook.setVisibility(View.GONE);
    }

    private Marker mDriverDestinationMarker;
    private void getDriverDestination(String key,final LatLng driverLocationLatLng) {
        DatabaseReference driverLocationRef=FirebaseDatabase.getInstance().getReference().child("driversAvailableDestination").child(key).child("l");
        driverLocationRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
                    LatLng driverDestinationLatLng=new LatLng(locationLat,locationLng);
                    mDriverDestinationMarker=mMap.addMarker(new MarkerOptions().position(driverDestinationLatLng).title("Rider Destination").icon(BitmapDescriptorFactory.fromResource(R.mipmap.destination)));
                    getRiderRoute(driverLocationLatLng,driverDestinationLatLng);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
