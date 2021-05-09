package com.example.sossystem;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
    private Handler mHandler = new Handler();
    DatabaseReference reff;
    SosRecords sosrecords;
    boolean startclicked = true;
    String selectedOfficer = "Police";
    private static final String TAG = "MainActivity";
    int LOCATION_REQUEST_CODE = 1001;

    Location last_location;
    GoogleMap map;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;
    public Double latitude;
    public Double longitude;


    Button btnStartSOS, btnStopSOS;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button logout = findViewById(R.id.logoutBtn);
        btnStartSOS = (Button) findViewById(R.id.btnStartSos);

        btnStopSOS = findViewById(R.id.btnStopSos);
        btnStopSOS.setEnabled(false);

        sosrecords = new SosRecords();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        reff = FirebaseDatabase.getInstance().getReference().child("Records");

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startclicked == true) {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(getApplicationContext(), Login.class));
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, "You need to stop SOS before you can logout", Toast.LENGTH_SHORT).show();
                }

            }
        });

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                last_location = locationResult.getLastLocation();

                if (last_location != null) {
                    //map
                    SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);
                    supportMapFragment.getMapAsync(MainActivity.this);

                    double lat = last_location.getLatitude();
                    double lon = last_location.getLongitude();
                    latitude = lat;
                    longitude = lon;

                    //add to sosrecords
                    sosrecords.setLatitude(latitude);
                    sosrecords.setLongitude(longitude);

                }

            }
        };
        btnStartSOS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOptionDialog();
            }
        });

        btnStopSOS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startclicked = true;
                mHandler.removeCallbacks(LatLonLoop);
                onStop();
                stopLocationUpdates();
                deleteRecords(sosrecords.getRecordID());

                btnStartSOS.setEnabled(true);
                btnStopSOS.setEnabled(false);
            }
        });
    }

    private void showOptionDialog() {
        String[] officers = {"Police", "Hospital", "Bomba"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Choose SOS types");
        builder.setSingleChoiceItems(officers, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedOfficer = officers[which];
            }
        });
        builder.setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sosrecords.setCallFor(selectedOfficer);
                {
                    onStart();
                    startclicked = false;
                    //GET ALL INFORMATION FROM FIRESTORE AND SEND TO REALTIME DATABASE
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        DocumentReference df = FirebaseFirestore.getInstance().collection("Users").document(FirebaseAuth.getInstance().getCurrentUser().getUid());
                        df.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if (documentSnapshot.getString("FullName") != null) {
                                    String id = reff.push().getKey();
                                    Log.d(TAG, "asdasd" + id);
                                    SosRecords sosRecords = new SosRecords(documentSnapshot.getString("FullName"), (documentSnapshot.getString("PhoneNumber")), (documentSnapshot.getString("UserEmail")), sosrecords.getLatitude(), sosrecords.getLongitude(), sosrecords.getCallFor());
                                    reff.child(id).setValue(sosRecords);
                                    sosrecords.setRecordID(id);
                                    btnStartSOS.setEnabled(false);
                                    btnStopSOS.setEnabled(true);
                                    Toast.makeText(MainActivity.this, "You're are now activating SOS request !", Toast.LENGTH_SHORT).show();
                                    LatLonLoop.run();

                                    onMapReady(map);

                                }

                            }


                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                FirebaseAuth.getInstance().signOut();
                                startActivity(new Intent(getApplicationContext(), Login.class));
                                finish();
                            }
                        });
                    }
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        checkSettingsAndStartLocationUpdates();
                    } else {
                        askLocationPermission();
                    }


                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }


    private Runnable LatLonLoop = new Runnable() {
        @Override
        public void run() {

            HashMap hashMap = new HashMap();
            hashMap.put("latitude", sosrecords.getLatitude());
            hashMap.put("longitude", sosrecords.getLongitude());
            reff.child(sosrecords.getRecordID()).updateChildren(hashMap);


            mHandler.postDelayed(this, 3000);
        }
    };

    private void deleteRecords(String recordID) {

        DatabaseReference delRecord = FirebaseDatabase.getInstance().getReference("Records").child(sosrecords.getRecordID());
        delRecord.removeValue();
        Toast.makeText(MainActivity.this, "The SOS Request is Stopped!", Toast.LENGTH_SHORT).show();
    }


    private void checkSettingsAndStartLocationUpdates() {
        LocationSettingsRequest request = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build();
        SettingsClient client = LocationServices.getSettingsClient(this);

        Task<LocationSettingsResponse> locationSettingsResponseTask = client.checkLocationSettings(request);

        locationSettingsResponseTask.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                //setting of device are satistfied and start location update
                startLocationUpdates();
            }
        });

        locationSettingsResponseTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    ResolvableApiException apiException = (ResolvableApiException) e;
                    try {
                        apiException.startResolutionForResult(MainActivity.this, 1001);
                    } catch (IntentSender.SendIntentException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            startLocationUpdates();
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);

    }

    private void askLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d(TAG, "askLocationPermission: you shouw ");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //permission granted
                //getLastLocation();
                checkSettingsAndStartLocationUpdates();
            } else {
                //permission not granted
            }
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng latLng = new LatLng(last_location.getLatitude(), last_location.getLongitude());
        map=googleMap;
        //createmarker
        MarkerOptions options = new MarkerOptions().position(latLng);
        Marker marker = map.addMarker(options);

        map.addMarker(new MarkerOptions().position(latLng).title("here"));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(last_location.getLatitude(), last_location.getLongitude()), 11));

    }
}




