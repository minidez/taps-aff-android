package uk.co.ianadie.tapsaff;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.bugsense.trace.BugSenseHandler;

public class TapsAffWidgetProvider extends AppWidgetProvider {

	SharedPreferences settings;
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		settings = PreferenceManager.getDefaultSharedPreferences(context);
		
		TapsAff.getAndStoreApiKeys(context);
		
		BugSenseHandler.initAndStartSession(context, settings.getString("bugsense_api_key", ""));

		Intent intent = new Intent(context.getApplicationContext(), UpdateWidgetService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		context.startService(intent);
	}

	public int getSize() {
		return TapsAff.SIZE_LARGE;
	}
}