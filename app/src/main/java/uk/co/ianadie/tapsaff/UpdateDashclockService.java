package uk.co.ianadie.tapsaff;

import android.app.Service;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bugsense.trace.BugSenseHandler;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by Ian on 04/06/13.
 */
public class UpdateDashclockService extends DashClockExtension implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, TapsAffCallbackListener {
    //	private LocationClient myLocationClient;
    GoogleApiClient myGoogleApiClient;
    Location myLocation;
    private LocationRequest myLocationRequest;
    SharedPreferences settings;

    @Override
    protected void onUpdateData(int arg0) {
        TapsAff.getAndStoreApiKeys(this);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        BugSenseHandler.initAndStartSession(this, settings.getString("bugsense_api_key", ""));

        if (settings.getLong("updatedTime", 0) < System.currentTimeMillis() - TapsAff.CACHE_TIME) {
            // more than one hour ago, get new values
            myGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        } else {
            this.updateWidgetViews(null);
        }
    }

    @Override
    public void updateWidgetViews(Object result) {
        int tapsAffTemp = Integer.parseInt(settings.getString("tapsAffTemp", "17"));
        int currentTemp;

        if (settings.getString("tempUnit", TapsAff.CELSIUS).equals(TapsAff.CELSIUS)) {
            currentTemp = settings.getInt("tempC", 17);
        } else {
            currentTemp = settings.getInt("tempF", 63);
        }

        String newText;
        int icon;
        SharedPreferences.Editor editor = settings.edit();
        if (currentTemp < tapsAffTemp) {
            newText = this.getResources().getString(R.string.taps_oan_string_large);
            icon = R.drawable.tapsoan;
            editor.putBoolean("tapsAff", false);
        } else {
            newText = this.getResources().getString(R.string.taps_aff_string_large);
            icon = R.drawable.tapsaff;
            editor.putBoolean("tapsAff", true);
        }
        editor.commit();

        Log.i(TapsAff.LOG_TAG, "Current temerature is " + currentTemp + ", it is " + newText + "!");

        String area = (String) settings.getString("area", null);

        publishUpdate(new ExtensionData().visible(true).icon(icon).status(newText).expandedTitle(newText).expandedBody(area + ": " + currentTemp + "°"));
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        //ignore
    }

    @Override
    public void onConnected(Bundle arg0) {
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

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Service.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TapsAff.LOG_TAG, "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onLocationChanged(Location location) {
        fetchWeatherForLocation(location);
    }

    private void fetchWeatherForLocation(Location l) {
        if (isConnected()) {
            new GetWeatherTask().execute(l, this, settings);
        } else {
            // TODO: Register for network change and then update.
        }
    }
}