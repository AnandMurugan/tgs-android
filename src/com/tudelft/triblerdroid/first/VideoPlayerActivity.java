//Skeleton example from Alexey Reznichenko

package com.tudelft.triblerdroid.first;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.util.Timer;

public class VideoPlayerActivity extends Activity {

	NativeLib nativelib = null;
    protected SwiftMainThread _swiftMainThread;
    protected StatsTask _statsTask;
	private VideoView mVideoView = null;
    protected ProgressDialog _dialog;
    protected Integer _seqCompInt;

    String hash; 
	String tracker;
	String destination;
	boolean inmainloop = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
	
	  super.onCreate(savedInstanceState);

//	  Raul, 2012-03-21: No necessary because of the notitle.fullscreen in Manifest
//      setTheme(android.R.style.Theme_Light);
//      requestWindowFeature(Window.FEATURE_NO_TITLE);
//      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
//                              WindowManager.LayoutParams.FLAG_FULLSCREEN); 
      setContentView(R.layout.main);

////      Raul, 2012-03-09: initialized in P2PStartActivity
//      ScriptApplication application = (ScriptApplication) getApplication();
//      if (application.readyToStart()) {
//        startService(new Intent(this, ScriptService.class));
//      }
      // Arno, 2012-02-15: Hack to keep this activity alive.
      // finish();
      
      try
      {
    	  SwiftInitalize();
      }
      catch(Exception e)
      {
    	  e.printStackTrace();
      }
	  Bundle extras = getIntent().getExtras();
	  hash = extras.getString("hash");//"280244b5e0f22b167f96c08605ee879b0274ce22"
	  tracker = extras.getString("tracker"); // See VodoEitActivity to change this
	  destination = "/sdcard/swift/video.ts";
	  SwiftStartDownload();
  }
  
  public void onDestroy()
  {
		super.onDestroy();
		Log.w("SwiftStats", "*** SHUTDOWN SWIFT ***");
		// Raul, 2012-04-25: Halts swift completely on destroy
		_statsTask.cancel(true);
		Log.w("SwiftStats", "*** SHUTDOWN SWIFT ***");
		// Halts swift completely
		//nativelib.stop(); Raul: this raises an exception.
		//I think it's because there is not time to execute it onDestroy
  }
  
  
  /*
   *  Arno: From Riccardo's original SwiftBeta
   */
  
  protected void SwiftInitalize()
  {
	  // create dir for swift
	  String swiftFolder = "/swift";
	  String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
	  File mySwiftFolder = new File(extStorageDirectory + swiftFolder);
	  mySwiftFolder.mkdir();	  
  }
  
	//starts the download thread
	protected void SwiftStartDownload() {
		// Start the background process
		_swiftMainThread = new SwiftMainThread();
		_swiftMainThread.start();    	
		// start the progress bar
		SwiftCreateProgress();
		_statsTask = new StatsTask();
		_statsTask.execute( hash, tracker, destination );
	}
	
	// creates the progress dialog
	protected void SwiftCreateProgress() {
		_dialog = new ProgressDialog(VideoPlayerActivity.this);
	  _dialog.setCancelable(true);
	  _dialog.setMessage("Buffering...");
	  // set the progress to be horizontal
	  _dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	  // reset the bar to the default value of 0
	  _dialog.setProgress(0);
	  
	  //stop the engine if the procress scree is cancelled
	  _dialog.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
//				_text.setText("TODO HTTPGW engine stopped!");
				// Arno, 2012-01-30: TODO tell HTTPGW to stop serving data
				//nativelib.stop();
				// Raul, 2012-03-27: don't stay here with a black screen. 
				// Go back to video list
				finish();
			}
		});
	
	  // display the progressbar
	  _dialog.show();
	  
	}
	
	
	//starts the video playback
	private void SwiftStartPlayer() {
		//_dialog.dismiss();
		if (destination == null || destination.length() == 0) {
			Toast.makeText(VideoPlayerActivity.this, "File URL/path is empty",
					Toast.LENGTH_LONG).show();
		}
		else {
			runOnUiThread(new Runnable(){
				public void run() {
					getWindow().setFormat(PixelFormat.TRANSLUCENT);
//					_text.setText("Play " + destination);
		    		mVideoView = (VideoView) findViewById(R.id.surface_view);
	
		    		// Arno, 2012-01-30: Download *and* play, using HTTPGW
		    		//String filename = "/sdcard/swift/" + destination;
		    		//mVideoView.setVideoPath(destination);
		    		String urlstr = "http://127.0.0.1:8082/"+hash;
		    		//String urlstr = "file:"+destination;
		    		mVideoView.setVideoURI(Uri.parse(urlstr));
		    		
		    		mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
						@Override
						public void onPrepared (MediaPlayer mp) {
//							_text.setText("Player75 prepared!");
							_dialog.dismiss();
						}
					});
		    		
		    		
		    		MediaController mediaController = new MediaController(VideoPlayerActivity.this);
		    		mediaController.setAnchorView(mVideoView);
		            mVideoView.setMediaController(mediaController);
					mVideoView.start();
					mVideoView.requestFocus();
		    		//mediaController.show(0); // keep visible
				}
				
			});
			
		}
	}
	
    private class SwiftMainThread extends Thread
    {
        public void run() 
        {
    		try 
    		{
    			NativeLib nativelib =  new NativeLib();
    			String ret = nativelib.start(hash, tracker, destination);
    			
				SwiftStartPlayer();
				
				// Arno: Never returns, calls libevent2 mainloop
				if (!inmainloop) 
				{
					inmainloop = true;
					Log.w("Swift","Entering libevent2 mainloop");
					
					int progr = nativelib.mainloop();
					
					Log.w("Swift","LEFT MAINLOOP!");
    			}
    		}
        	catch (Exception e ) 
        	{
        			e.printStackTrace();
        	}
        }
    }
	
    
	/**
	* sub-class of AsyncTask. Retrieves stats from Swift via JNI and
	* updates the progress dialog.
	*/
	private class StatsTask extends AsyncTask<String, Integer, String> {
		
	  protected String doInBackground(String... args) {
	  	
	  	String ret = "hello";
	  	if (args.length != 3) {
	  		ret = "Received wrong number of parameters during initialization!";
	  	}
	  	else {
	  		try {//TODO: catch InterruptedException (onDestroy)
	
	  			NativeLib nativelib =  new NativeLib();
	  			mVideoView = (VideoView) findViewById(R.id.surface_view);
	  			boolean play = false, pause=false;
	  			
	  			while(true) {
	  				String progstr = nativelib.httpprogress(args[0]);
	  				String[] elems = progstr.split("/");
	  				long seqcomp = Long.parseLong(elems[0]);
	  				long asize = Long.parseLong(elems[1]);
	
	  				if (asize == 0)
	  					_dialog.setMax(1024);
	  				else
	  					_dialog.setMax((int)(asize/1024));
	  				
	  				_seqCompInt = new Integer((int)(seqcomp/1024));
	  				
	  				Log.w("SwiftStats", "SeqComp   " + seqcomp );
	  				
	  	    		runOnUiThread(new Runnable(){
	  	    			public void run() {
	          				_dialog.setProgress(_seqCompInt.intValue() );
	
	  	    			}
	  	    		});
	  				//Raul, 20120425: removed break which caused playback interruption when
	  	    		//(asize > 0 && seqcomp == asize) (e.i, file downloaded)
	  				Thread.sleep( 1000 );
	  			}
	  			
	  		}
	  		catch (Exception e ) {
	  			//System.out.println("Stacktrace "+e.toString());
	  			e.printStackTrace();
	  			ret = "error occurred during initialization!";
	  		}
	  	}
	      return ret;
	  }
	}
}
