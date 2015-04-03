package uk.co.ianadie.tapsaff;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class About extends Activity implements OnClickListener {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.about);

		TextView comments = (TextView) this.findViewById(R.id.about_comments);
		Linkify.addLinks(comments, Linkify.ALL);

		Button donateButton = (Button) this.findViewById(R.id.about_rateButton);

		donateButton.setOnClickListener(this);
	}

	public void onClick(View arg0) {
		if (arg0.getId() == R.id.about_rateButton) {
			Intent myIntent =
				new Intent(
						Intent.ACTION_VIEW,
						Uri.parse(
								"market://details?id=uk.co.ianadie.tapsaff"));

			startActivity(myIntent);
		}
	}
}
