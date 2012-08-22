//Skeleton example from Alexey Reznichenko
package com.tudelft.triblerdroid.first;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;

public class IntroActivity extends Activity {
    public static final String PREFS_NAME = "settings.dat";
    Button b_continue;
    CheckBox cb_showIntro;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	  
		final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		boolean showIntro = settings.getBoolean("showIntro", true);
		Intent intent;
		
		if (showIntro) {
			setContentView(R.layout.intro);
			cb_showIntro = (CheckBox) findViewById(R.id.cb_show_intro);
			cb_showIntro.setChecked(true);
			b_continue = (Button) findViewById(R.id.b_continue);
			b_continue.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					if (!cb_showIntro.isChecked()){
						//SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
						SharedPreferences.Editor editor = settings.edit();
						editor.putBoolean("showIntro", false);
						editor.commit(); //Raul: don't forget to commit edits!!
						Log.w("intro", "Don't show Intro next time");
					}
					Intent intent = getPlayerIntent();
					startActivityForResult(intent, 0);
				}  	
			});
		}
		else
		{
			Log.w("intro", "don't show intro: go to P2P directly");
			intent = getPlayerIntent();
			startActivityForResult(intent, 0);
		}
	}
	
	private Intent getPlayerIntent(){
		Intent intent;
		String hash = getHash();
		if (hash != null){
			//found hash: play video
			intent = new Intent(getBaseContext(), VideoPlayerActivity.class);
			intent.putExtra("hash", hash);
		}
		else{
			//no hash: show menu
			intent = new Intent(getBaseContext(), VideoListActivity.class);
		}
		return intent;

	}
	
	private String getHash(){
		String hash = null;
		String tracker = "192.16.127.98:20050"; //TODO
		Uri data = getIntent().getData(); 
		Uri datas = getIntent().getData(); 
		if (datas != null) { 
			System.out.println("URI: " + datas);
			String scheme = data.getScheme(); 
			String host = data.getHost(); 
			//			  int port = data.getPort();
			if (scheme.equals("ppsp")){
				hash = host;
			}
			if (host.equals("ppsp.me")){
				hash = data.getLastPathSegment();
			}
			Log.w("videoplayer", "ppsp link: " + hash);
			return hash;
		}

		Bundle extras = getIntent().getExtras();
		if (extras != null){
			String text = extras.getString("android.intent.extra.TEXT");
			if (text != null){
				//parameters come from twicca			
				Log.w("video twicca", text);
				Pattern p = Pattern.compile("ppsp://.{40}");
				Matcher m = p.matcher(text);
				if (m.find()) {
					String s = m.group();
					hash = s.substring(7);
					Log.w("video twicca", hash);
				}
				else{
					hash = null;
					Log.w("video twicca", "no ppsp link found");
				}
				tracker = "192.16.127.98:20050"; //TODO
			}
			return hash;
		}
		return hash;
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// Done, exit application
        finish();
	}
}
