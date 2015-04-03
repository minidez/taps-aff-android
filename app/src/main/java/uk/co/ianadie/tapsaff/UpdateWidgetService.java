package uk.co.ianadie.tapsaff;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.bugsense.trace.BugSenseHandler;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by Ian on 04/06/13.
 */
public class UpdateWidgetService extends Service implements TapsAffCallbackListener, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private int[] allWidgetIds;
    private GoogleApiClient myGoogleApiClient;
    Location myLocation;
    private LocationRequest myLocationRequest;
    SharedPreferences settings;
    int widgetSize;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TapsAff.LOG_TAG,"onStartCommand");

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        allWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

        BugSenseHandler.initAndStartSession(this, settings.getString("bugsense_api_key", ""));

        if (settings.getLong("updatedTime", 0) < System.currentTimeMillis() - TapsAff.CACHE_TIME) {
            // more than one hour ago, get new values
            Log.d(TapsAff.LOG_TAG,"get new location");

            myGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            myGoogleApiClient.connect();
        } else {
            this.updateWidgetViews(null);
        }

        return START_NOT_STICKY;
    }

    public void updateWidgetViews(Object result) {
        Log.d(TapsAff.LOG_TAG,"updateWidgetViews");
        int tapsAffTemp = Integer.parseInt(settings.getString("tapsAffTemp", "17"));
        int currentTemp;

        if (settings.getString("tempUnit", TapsAff.CELSIUS).equals(TapsAff.CELSIUS)) {
            currentTemp = settings.getInt("tempC", 17);
        } else {
            currentTemp = settings.getInt("tempF", 63);
        }

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(UpdateWidgetService.this.getApplicationContext());

        SharedPreferences.Editor editor = settings.edit();

        if (currentTemp < tapsAffTemp) {
            editor.putBoolean("tapsAff", false);
        } else {
            editor.putBoolean("tapsAff", true);
        }
        editor.commit();

        for (int widgetId : allWidgetIds) {
            // Check if lockscreen/homescreen

            boolean isKeyguard;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Bundle myOptions = appWidgetManager.getAppWidgetOptions(widgetId);
                int category = myOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1);
                isKeyguard = category == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;
            } else {
                // old sdk that doesn't support lockscreen widgets
                isKeyguard = false;
            }

            AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(widgetId);
            if (info == null) {
                // Just in case it hasn't been bound to a provider yet. Unsure how often this happens so keeping track on BugSense.
                BugSenseHandler.sendEvent("null AppwidgetProvider");
                continue;
            }

            int initialLayout = info.initialLayout;

            switch (initialLayout) {
                case R.layout.widget_layout_small:
                    widgetSize = TapsAff.SIZE_SMALL;
                    break;
                case R.layout.widget_layout_medium:
                    widgetSize = TapsAff.SIZE_MEDIUM;
                    break;
                case R.layout.widget_layout_large:
                    widgetSize = TapsAff.SIZE_LARGE;
                    break;
            }
            RemoteViews remoteViews;
            remoteViews = TapsAff.updateViews(settings, this, isKeyguard, widgetSize);

            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
        editor = settings.edit();
        editor.putBoolean("viewsUpdated", true);
        editor.commit();
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
    }

    @Override
    public void onConnected(Bundle arg0) {
        Log.d(TapsAff.LOG_TAG,"onConnected");
        myLocation = LocationServices.FusedLocationApi.getLastLocation(myGoogleApiClient);

        if (myLocation != null) {
            fetchWeatherForLocation(myLocation);
        }

        //request an update and listen callback.
        myLocationRequest = LocationRequest.create();
        myLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        myLocationRequest.setInterval(3600000); // Update location every hour
        LocationServices.FusedLocationApi.requestLocationUpdates(myGoogleApiClient, myLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        //ignore
    }

    @Override
    public void onLocationChanged(Location location) {
        fetchWeatherForLocation(location);
    }

    private void fetchWeatherForLocation(Location l) {
        Log.d(TapsAff.LOG_TAG,"fetchWeatherForLocation");
        if (isConnected()) {
            new GetWeatherTask().execute(l, this, settings);
        } else {
            // TODO: Register for network change and then update.
        }
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Service.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}