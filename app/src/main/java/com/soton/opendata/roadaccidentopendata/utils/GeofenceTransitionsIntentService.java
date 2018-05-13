package com.soton.opendata.roadaccidentopendata.utils;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.soton.opendata.roadaccidentopendata.R;


public class GeofenceTransitionsIntentService extends IntentService {
    private String CHANNEL_ID="noti";
    private NotificationManagerCompat notificationManager;

    public GeofenceTransitionsIntentService() {
        super("GeofenceTransitionsIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        createNotificationChannel();

        notificationManager = NotificationManagerCompat.from(getApplicationContext());

        Log.e("fence", "intent");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()) {
            String errorMessage = geofencingEvent.getErrorCode()+"";
            Log.e("fence", errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.e("fence","in");

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setContentTitle("The Status of Real-Time Warning")
                    .setContentText("In")
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("In."))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setAutoCancel(false);
            notificationManager.notify(1, mBuilder.build());
        }
        if(geofenceTransition==Geofence.GEOFENCE_TRANSITION_EXIT){
            Log.e("fence","out");

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setContentTitle("The Status of Real-Time Warning")
                    .setContentText("Out")
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("Out."))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setAutoCancel(false);
            notificationManager.notify(1, mBuilder.build());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("service","create");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("service","destroy");
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
}
