package com.soton.opendata.roadaccidentopendata.Activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;


import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.soton.opendata.roadaccidentopendata.R;
import com.soton.opendata.roadaccidentopendata.utils.GeofenceTransitionsIntentService;
import com.soton.opendata.roadaccidentopendata.utils.HttpURLConnectionNetworkTask;
import com.soton.opendata.roadaccidentopendata.utils.NetworkTask;
import com.soton.opendata.roadaccidentopendata.utils.ParseJSON;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private HeatmapTileProvider mProvider;
    private TileOverlay mOverlay;
    private int MODE = 1; //0为实时地图，1为热力图
    private FusedLocationProviderClient mFusedLocationClient;
    private Location clocation;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest=new LocationRequest().setInterval(2000);
    private NotificationManagerCompat notificationManager;
    private String CHANNEL_ID="noti";
    private GeofencingClient geofencingClient;
    private List<Geofence> geofences;
    private PendingIntent mGeofencePendingIntent;
    private ImageView cautionsign;
    private boolean inthezone=false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();
        notificationManager = NotificationManagerCompat.from(getApplicationContext());
        startService(new Intent(MainActivity.this,GeofenceTransitionsIntentService.class));
        geofencingClient=LocationServices.getGeofencingClient(this);
        addGeofences();

        cautionsign=findViewById(R.id.cautionsign);
        cautionsign.setVisibility(View.INVISIBLE);



        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        //获取位置权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                clocation=location;
            }
        });

        //处理位置更新，实时提醒处理
        locationCallback=new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                //最后位置
                Location lastLocation=locationResult.getLastLocation();
                toast(lastLocation.getLatitude()+" "+lastLocation.getLongitude());
                if((lastLocation.getLatitude()>50.9267)&&(lastLocation.getLatitude()<50.9269)){
                    if((lastLocation.getLongitude()>-1.3829)&&(lastLocation.getLongitude()<-1.3827)){
                        cautionsign.setVisibility(View.VISIBLE);
                        if(!inthezone){
                            MediaPlayer.create(getApplicationContext(),R.raw.indangerzone).start();
                        }
                        inthezone=true;
                    }else{
                        cautionsign.setVisibility(View.INVISIBLE);
                        inthezone=false;
                    }
                }

            }
        };


        //切换模式按钮
        Button btnSwitch = findViewById(R.id.btn_switch);
        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(MODE==1){
                    mOverlay.remove();
                    mOverlay.clearTileCache();
                    MODE=0;
                    //天气获取
                    String URL="http://api.openweathermap.org/data/2.5/weather?id=2643744&units=metric&appid=ca16b1c6672b2c0e020af5b8346797a3";
                    NetworkTask networkTask=new HttpURLConnectionNetworkTask(NetworkTask.GET);
                    networkTask.execute(URL);
                    networkTask.setResponceLintener(new NetworkTask.ResponceLintener() {
                        @Override
                        public void onSuccess(String result) {
                            String weathercode=ParseJSON.parseWeather(result);

                            //获取坐标
                            String URL="http://ec2-52-56-125-151.eu-west-2.compute.amazonaws.com:8983/solr/london_accidents_index/select?rows=99999&wt=json&indent=true&q=id%3A(2016*+%7C%7C+2015*)+%26%26+Weather_Conditions%3A"
                                    +weathercode+"" +
                                    "+%26%26+Hour%3A"+ Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                            Log.e("url",URL);

                            //获取地理位置
                            NetworkTask networkTask=new HttpURLConnectionNetworkTask(NetworkTask.GET);
                            networkTask.execute(URL);
                            networkTask.setResponceLintener(new NetworkTask.ResponceLintener() {
                                @Override
                                public void onSuccess(String result) {
                                    addHeatMap(ParseJSON.parseLatlng(result));
                                    Snackbar.make(getCurrentFocus(),"Switched to Real-Time Mode",Snackbar.LENGTH_LONG).show();
                                }

                                @Override
                                public void onError(String error) {

                                }
                            });
                        }
                        @Override
                        public void onError(String error) {
                            toast(error);
                        }
                    });

                }else if(MODE==0){
                    try {
                        addHeatMap(readItems(R.raw.caraccident));
                        Snackbar.make(getCurrentFocus(),"Switched to Full Mode",Snackbar.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    MODE=1;
                }
            }
        });



        //实时提醒开关
        ToggleButton tg=findViewById(R.id.toggleButton);
        tg.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    startLocationUpdates();


                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                            .setSmallIcon(R.drawable.notification_icon)
                            .setContentTitle("The Status of Real-Time Warning")
                            .setContentText("Real-Time Warning Turned On")
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText("Real-Time Warning Turned On."));
                    notificationManager.notify(1, mBuilder.build());
                }else {
                    stopLocationUpdates();

                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                            .setSmallIcon(R.drawable.notification_icon)
                            .setContentTitle("The Status of Real-Time Warning")
                            .setContentText("Real-Time Warning Turned Off")
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText("Real-Time Warning Turned Off."))
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setAutoCancel(false);
                    notificationManager.notify(1, mBuilder.build());
                }

            }
        });

        //使用自动完成插件位置搜索
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(final Place place) {
                // TODO:Get info about the selected place.
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(),15));

                String URL="http://api.openweathermap.org/data/2.5/weather?id=2643744&units=metric&appid=ca16b1c6672b2c0e020af5b8346797a3";
                NetworkTask networkTask=new HttpURLConnectionNetworkTask(NetworkTask.GET);
                networkTask.execute(URL);
                networkTask.setResponceLintener(new NetworkTask.ResponceLintener() {
                    @Override
                    public void onSuccess(String result) {
                        String weathercode=ParseJSON.parseWeather(result);

                        //获取坐标
                        String URL="http://ec2-52-56-125-151.eu-west-2.compute.amazonaws.com:8983/solr/london_accidents_index/select?wt=json&indent=true&q=id%3A(2016*+%7C%7C+2015*)+%26%26+Weather_Conditions%3A"
                                +weathercode+
                                "+%26%26+Longitude%3A%5B"
                                +(place.getLatLng().longitude-0.003) +
                                "+TO+"
                                + (place.getLatLng().longitude+0.003)+
                                "%5D+%26%26+Latitude%3A%5B"+(place.getLatLng().latitude-0.003)+"+TO+"+(place.getLatLng().latitude+0.003)+"%5D+%26%26+Hour%3A"
                                + Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                        Log.e("url",URL);

                        NetworkTask networkTask=new HttpURLConnectionNetworkTask(NetworkTask.GET);
                        networkTask.execute(URL);
                        networkTask.setResponceLintener(new NetworkTask.ResponceLintener() {
                            @Override
                            public void onSuccess(String result) {
                                int caseCount=ParseJSON.parseCaseCount(result);
                                showInfoDialog("The place you searched at the current condition had "+caseCount+" accidents happened in history.");
                            }

                            @Override
                            public void onError(String error) {

                            }
                        });
                    }
                    @Override
                    public void onError(String error) {
                        toast(error);
                    }
                });

                toast("搜索的地点经纬度"+place.getLatLng().toString());
            }

            @Override
            public void onError(Status status) {
                // TODO:Handle the error.
                Log.i("placeapi", "An error occurred: " + status);
            }
        });


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    //权限处理回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        //mMap.moveCamera(CameraUpdateFactory.zoomTo(10));
        //mMap.setLatLngBoundsForCameraTarget(new LatLngBounds(new LatLng(51.290718,0.308564),new LatLng(51.69155, 0.510023)));


        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public boolean onMyLocationButtonClick() {
                mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        clocation=location;
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(clocation.getLatitude(),clocation.getLongitude()),18));
                        Toast.makeText(getApplicationContext(),location.toString(),Toast.LENGTH_LONG).show();
                    }
                });

                return true;
            }
        });

        try {
            addHeatMap(readItems(R.raw.caraccident));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //权限处理
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

    }




    //添加热力图
    private void addHeatMap(List<LatLng> list) {


        // Create a heat map tile provider, passing it the latlngs of the police stations.
        mProvider = new HeatmapTileProvider.Builder()
                .data(list)
                .build();

        //释放内存
        list.clear();
        // Add a tile overlay to the map, using the heat map tile provider.
        mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
    }


    private ArrayList<LatLng> readItems(int resource) throws JSONException {
        ArrayList<LatLng> list = new ArrayList<LatLng>();
        InputStream inputStream = getResources().openRawResource(resource);
        String json = new Scanner(inputStream).useDelimiter("\\A").next();
        JSONArray array = new JSONArray(json);
        Toast.makeText(getApplicationContext(), array.length() + "", Toast.LENGTH_LONG).show();
        for (int i = 0; i < 55000; i++) {
            JSONObject object = array.getJSONObject(i);
            double lat = object.getDouble("Latitude");
            double lng = object.getDouble("Longitude");
            list.add(new LatLng(lat, lng));
        }
        return list;
    }

    private void toast(String content){
        Toast.makeText(getApplicationContext(),content,Toast.LENGTH_LONG).show();
    }


    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        mFusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback, null);
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(locationCallback);
        cautionsign.setVisibility(View.INVISIBLE);
        inthezone=false;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "mychannel";
            String description = "this is my channel";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showInfoDialog(String Content){
        final AlertDialog.Builder dialog=new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle("Query Result");
        dialog.setMessage(Content);
        dialog.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        dialog.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        dialog.show();
    }

    @SuppressLint("MissingPermission")
    private void addGeofences(){
        LatLng latLng=new LatLng(50.926845,-1.382881);
        geofences=new ArrayList<>();
        geofences.add(new Geofence.Builder()
                .setRequestId("0")
                .setCircularRegion(
                        latLng.latitude,
                        latLng.longitude,
                        10//圆半径
                )
                .setExpirationDuration(2000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        );
        geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Geofences added
                        // ...
                        Log.e("fence","added");
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to add geofences
                        // ...
                        Log.e("fence","addedFailed");
                    }
                });
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofences);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(MainActivity.this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }



}
