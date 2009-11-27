package edu.ucla.cens.wetap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class WeTapActivity extends Activity {

	/* Fields */
	private final int SPLASH_DISPLAY_LENGHT = 1; 
    private String TAG = "SPLASH";

	/* Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);

        Log.d(TAG, "started splash");

		/* only start authenticateIntent the first time onCreate is called */
		if(savedInstanceState == null || !savedInstanceState.getBoolean("started"))
			new Handler().postDelayed(new Runnable(){
				public void run() {
					// Create an Intent that will start the Authenticate-Activity. // 
					WeTapActivity.this.startActivity(new Intent(
                        WeTapActivity.this,authenticate.class));
                    Log.d(TAG, "started authentication intent");
					WeTapActivity.this.finish(); 
				} 
			}, SPLASH_DISPLAY_LENGHT);
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putBoolean("started", true);
		super.onSaveInstanceState(savedInstanceState);
	}
}
