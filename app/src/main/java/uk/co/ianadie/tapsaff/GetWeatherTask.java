package uk.co.ianadie.tapsaff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;

import com.bugsense.trace.BugSenseHandler;

public class GetWeatherTask extends AsyncTask<Object, Void, Boolean> {
	String reply = null;
	BufferedReader in = null;
	RemoteViews remoteViews;
	TapsAffCallbackListener mTapsAffListener;
	String tempUnit;
	SharedPreferences settings;
	int retry =0;

	@Override
	protected Boolean doInBackground(Object... params) {
		Location location = (Location) params[0];
		mTapsAffListener = (TapsAffCallbackListener) params[1];
		settings = (SharedPreferences) params[2];

		try {
			String request = ("http://api.openweathermap.org/data/2.5/weather?lat="+location.getLatitude()+"&lon="+location.getLongitude()+"&units=metric&appid="+settings.getString("owm_api_key", ""));

			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(request);
			HttpResponse response;
			
			while(reply==null && retry++<3) {
				response = client.execute(get);
				HttpEntity entity = response.getEntity();
				in = new BufferedReader(new InputStreamReader(entity.getContent()));
				reply = in.readLine();
			}
			Log.d(TapsAff.LOG_TAG, "weather response: "+reply);
			
			if(reply==null) {
				return false;
			}
			
			// Store data
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt("tempC", getTemperatureFromJson(reply, TapsAff.CELSIUS));
			editor.putInt("tempF", getTemperatureFromJson(reply, TapsAff.FARENHEIT));
			editor.putString("area", getLocationFromJson(reply));
			editor.putLong("updatedTime", System.currentTimeMillis());
			
			editor.commit();

			return true;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			BugSenseHandler.sendException(e);
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			BugSenseHandler.sendException(e);
			return false;
		} catch (JSONException e) {
			e.printStackTrace();
			BugSenseHandler.sendExceptionMessage("reply", reply, e);
			return false;
		}
	}

	@Override
	protected void onPostExecute(Boolean result) {
		// call parents updateViews method
		mTapsAffListener.updateWidgetViews(result);
	}

	private int getTemperatureFromJson(String json, String tempUnit) throws JSONException {
		JSONObject JSONResults = new JSONObject(json);
		JSONObject main = JSONResults.getJSONObject("main");
		int tempC = main.getInt("temp");

		if(tempUnit.equals(TapsAff.CELSIUS))
			return tempC;
		else
			return (int) (tempC*1.8+32);
	}

	private String getLocationFromJson(String json) throws JSONException {
		JSONObject JSONResults = new JSONObject(json);

		return JSONResults.getString("name");
	}
}
