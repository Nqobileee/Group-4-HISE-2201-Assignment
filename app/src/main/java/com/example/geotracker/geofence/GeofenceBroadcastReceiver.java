package com.example.geotracker.geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.example.geotracker.data.AppDatabase;
import com.example.geotracker.data.LocationEntity;
import com.example.geotracker.notifications.NotificationHelper;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceReceiver";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing event error: " + geofencingEvent.getErrorCode());
            return;
        }

        int transitionType = geofencingEvent.getGeofenceTransition();
        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER ||
            transitionType == Geofence.GEOFENCE_TRANSITION_DWELL ||
            transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
            
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            for (Geofence geofence : triggeringGeofences) {
                String requestId = geofence.getRequestId();
                fetchLocationAndNotify(context, requestId, transitionType);
            }
        }
    }

    private void fetchLocationAndNotify(Context context, String requestId, int transitionType) {
        executor.execute(() -> {
            try {
                int id = Integer.parseInt(requestId);
                LocationEntity location = AppDatabase.getInstance(context).locationDao().getLocationById(id);
                if (location != null) {
                    String title = "Geofence Alert";
                    String body = "";
                    switch (transitionType) {
                        case Geofence.GEOFENCE_TRANSITION_ENTER:
                            body = "You entered " + location.name;
                            break;
                        case Geofence.GEOFENCE_TRANSITION_DWELL:
                            body = "You are lingering at " + location.name;
                            break;
                        case Geofence.GEOFENCE_TRANSITION_EXIT:
                            body = "You left " + location.name;
                            break;
                    }
                    NotificationHelper notificationHelper = new NotificationHelper(context);
                    notificationHelper.sendNotification(title, body);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid requestId: " + requestId);
            }
        });
    }
}
