package uk.co.ianadie.tapsaff;

import java.util.Properties;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

public class TapsAff {
	static final String CONFIG_FILE_NAME = "config.txt";
	static final String LOG_TAG = "TapsAff";
	static final String CELSIUS = "Celsius";
	static final String FARENHEIT = "Fahrenheit";
	static final int CACHE_TIME = 0;
//	static final int CACHE_TIME = 3600000;

	static final int SIZE_SMALL = 0;
	static final int SIZE_MEDIUM = 1;
	static final int SIZE_LARGE = 2;

	static final int COLOUR_LIGHT = 0;
	static final int COLOUR_DARK = 0;

	public static void getAndStoreApiKeys(Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		SharedPreferences.Editor editor = settings.edit();

		//Read API keys from config file
		AssetsPropertyReader apr = new AssetsPropertyReader(c);
		Properties props = apr.getProperties("config.txt");
		editor.putString("owm_api_key", props.getProperty("owm_api_key"));
		editor.putString("bugsense_api_key", props.getProperty("bugsense_api_key"));
		editor.commit();
	}

	public static RemoteViews updateViews(SharedPreferences settings, Context c, boolean isKeyguard, int widgetSize) {
		String newText = "";
		String newTempText = settings.getInt("tempC", 1)+"°";
		RemoteViews remoteViews = null;

		if(isKeyguard) {
			remoteViews = new RemoteViews(c.getApplicationContext().getPackageName(), R.layout.widget_layout_keyguard);
		}else if(widgetSize == TapsAff.SIZE_LARGE) {
			remoteViews = new RemoteViews(c.getApplicationContext().getPackageName(), R.layout.widget_layout_large);
		} else if(widgetSize == TapsAff.SIZE_MEDIUM) {
			remoteViews = new RemoteViews(c.getApplicationContext().getPackageName(), R.layout.widget_layout_medium);
		} else if(widgetSize == TapsAff.SIZE_SMALL) {
			remoteViews = new RemoteViews(c.getApplicationContext().getPackageName(), R.layout.widget_layout_small);
		}

		if(!settings.contains("tapsAff")) {
			newText = c.getResources().getString(R.string.loading_string);
		} else if (settings.getBoolean("tapsAff", false)) {
			Log.i(TapsAff.LOG_TAG, "It is "+newTempText+", it is taps aff!");
			if(widgetSize == TapsAff.SIZE_LARGE) {
				newText = c.getResources().getString(R.string.taps_aff_string_large);
				newTempText = settings.getString("area", null) + ": " + newTempText;
			} else if(widgetSize == TapsAff.SIZE_MEDIUM) {
				newText = c.getResources().getString(R.string.taps_aff_string_medium);
			}
		} else {
			Log.i(TapsAff.LOG_TAG, "It is "+newTempText+", it is taps oan!");
			if(widgetSize == TapsAff.SIZE_LARGE) {
				newText = c.getResources().getString(R.string.taps_oan_string_large);
				newTempText = settings.getString("area", null) + ": " + newTempText;
			} else if(widgetSize == TapsAff.SIZE_MEDIUM) {
				newText = c.getResources().getString(R.string.taps_oan_string_medium);
			}
		}
		remoteViews.setTextViewText(R.id.text, newText);
		remoteViews.setTextViewText(R.id.temp, newTempText);

		if(isKeyguard) {
			// lockscreen widget
			if(settings.getLong("updatedTime", -1)<0) {
				remoteViews.setImageViewBitmap(R.id.image, BitmapFactory.decodeResource(c.getResources(), R.drawable.haudoanwhite));
				// text already light.
			} else {
				if(settings.getBoolean("tapsAff", true)) {
					remoteViews.setImageViewBitmap(R.id.image, BitmapFactory.decodeResource(c.getResources(), R.drawable.tapsaffwhite));
				} else {
					remoteViews.setImageViewBitmap(R.id.image, BitmapFactory.decodeResource(c.getResources(), R.drawable.tapsoanwhite));
				}
			}
		} else {
			// homescreen widget
			Bitmap newBitmap;
			if(settings.getString("textColour", "Light").equals("Light")) {
				remoteViews.setTextColor(R.id.text, c.getResources().getColor(R.color.light_font));
				remoteViews.setTextColor(R.id.temp, c.getResources().getColor(R.color.light_font_temp));
				if(!settings.contains("tapsAff")) {
					newBitmap = BitmapFactory.decodeResource(c.getResources(), R.drawable.haudoanwhite);
				} else if (settings.getBoolean("tapsAff", false)) {
					newBitmap = BitmapFactory.decodeResource(c.getResources(), R.drawable.tapsaffwhite);
				} else {
					newBitmap = BitmapFactory.decodeResource(c.getResources(), R.drawable.tapsoanwhite);
				}
			} else {
				remoteViews.setTextColor(R.id.text, c.getResources().getColor(R.color.dark_font));
				remoteViews.setTextColor(R.id.temp, c.getResources().getColor(R.color.dark_font_temp));
				if(!settings.contains("tapsAff")) {
					newBitmap = BitmapFactory.decodeResource(c.getResources(), R.drawable.haudoan);
				} else if (settings.getBoolean("tapsAff", false)) {
					newBitmap = BitmapFactory.decodeResource(c.getResources(), R.drawable.tapsaff);
				} else {
					newBitmap = BitmapFactory.decodeResource(c.getResources(), R.drawable.tapsoan);
				}
			}
			remoteViews.setImageViewBitmap(R.id.image, newBitmap);
		}
		return remoteViews;
	}
}