package uk.co.ianadie.tapsaff;

import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RemoteViews;

import com.bugsense.trace.BugSenseHandler;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class WidgetSettings extends PreferenceActivity implements OnSharedPreferenceChangeListener, OnClickListener {

	private static int PLAY_SERVICES_CALLBACK = 1;
	int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	SharedPreferences settings;
	boolean isKeyguard, isDashClock=false;
	AppWidgetManager appWidgetManager;
	int widgetSize = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// get prefs and add change listener
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		settings.registerOnSharedPreferenceChangeListener(this);
		TapsAff.getAndStoreApiKeys(this);

		BugSenseHandler.initAndStartSession(this, settings.getString("bugsense_api_key", ""));

		addPreferencesFromResource(R.xml.preferences);

		// Check for Google Play Services
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if(resultCode != ConnectionResult.SUCCESS) {
			Dialog mDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_CALLBACK);
			if(mDialog!=null) {
				Log.e(TapsAff.LOG_TAG, "Play services not installed, creating error dialog");
				mDialog.show();
			}
		}

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
			mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			isDashClock = extras.getBoolean(DashClockExtension.EXTRA_FROM_DASHCLOCK_SETTINGS);
		}

		ListView v = getListView();
		Button saveButton = new Button(this);
		saveButton.setText("Save");
		saveButton.setOnClickListener(this);
		v.addFooterView(saveButton);

		if(!isDashClock) {
			// if we're not dashclock then get size and isLockscreen
			appWidgetManager = AppWidgetManager.getInstance(this);
			switch(appWidgetManager.getAppWidgetInfo(mAppWidgetId).initialLayout) {
			case R.layout.widget_layout_small: widgetSize = TapsAff.SIZE_SMALL; break;
			case R.layout.widget_layout_medium: widgetSize = TapsAff.SIZE_MEDIUM; break;
			case R.layout.widget_layout_large: widgetSize = TapsAff.SIZE_LARGE; break;
			}

			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
				Bundle myOptions = appWidgetManager.getAppWidgetOptions (mAppWidgetId);
				int category = myOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1);
				isKeyguard = category == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;
			} else {
				// old sdk that doesn't support lockscreen widgets
				isKeyguard = false;
			}
		}

		if(isKeyguard || isDashClock) {
			findPreference("textColour").setEnabled(false);
		}
	}

	@Override
	public void onBackPressed() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		// Make sure we pass back the original appWidgetId
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);

		if(!isDashClock) {
			RemoteViews rv = TapsAff.updateViews(settings, this, isKeyguard, widgetSize);
			appWidgetManager.updateAppWidget(mAppWidgetId, rv);
		}
		finish();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		//		if(key.equals("temperatureUnit")) {
		//			//temperature unit changed, need to change my array for values, and convert current value
		//			String unit = settings.getString("temperatureUnit", TapsAff.CELSIUS);
		//			//get current temp setting
		//			int currentTemp = Integer.parseInt(settings.getString("tapsAffTemp", "17"));
		//			int newUnitTemp;
		//
		//			ListPreference temp = (ListPreference) this.findPreference("tapsAffTemp");
		//			if(unit.equals(TapsAff.FARENHEIT)) {
		//				//Celsius -> Farenheit
		//				newUnitTemp = (int) (currentTemp*1.8+32);
		//				temp.setEntries(R.array.temp_array_f);
		//				temp.setEntryValues(R.array.temp_array_values_f);
		//			} else {
		//				// Farenheit -> Celsius
		//				newUnitTemp = (int) ((currentTemp-32)/1.8);
		//				temp.setEntries(R.array.temp_array_c);
		//				temp.setEntryValues(R.array.temp_array_values_c);
		//			}
		//			SharedPreferences.Editor editor = settings.edit();
		//			editor.putString("tapsAffTemp", Integer.toString(newUnitTemp));
		//			editor.commit();
		//		}
	}

	@Override
	public void onClick(View arg0) {
		//shortcut for now.
		this.onBackPressed();
	}
}
