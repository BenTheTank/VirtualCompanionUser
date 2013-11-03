package de.virtualcompanion.user;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import de.virtualcompanion.user.IncomingCallFragment.IncomingCallFragmentListener;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class MainActivity extends Activity implements LocationListener, SurfaceHolder.Callback, 
									IncomingCallFragmentListener, OnSharedPreferenceChangeListener {

	// Handler fuer zeitverzoegertes senden
	private Handler sethandler = new Handler();
	private Handler gethandler = new Handler();
	private Runnable setrun = new Runnable() {

		@Override
		public void run() {
			data.getData();
			if(data.CamHasChanged())
				setCameraParameters();
			if (data.isStatus())
				sethandler.postDelayed(setrun, LONG_INTERVALL); // startet nach INTERVALL wieder den handler (Endlosschleife)
		}		
	};
	
	private Runnable getrun = new Runnable() {

		@Override
		public void run() {
			// Bildle machen
			if(camera!=null)
				camera.setOneShotPreviewCallback(precallback);
			
			data.updateData();
			data.publishData();
			data.sendData();
			
			// Changing the make call icon
			updateCallIcon();
						
			if (data.isStatus())
				gethandler.postDelayed(getrun, INTERVALL); // startet nach INTERVALL wieder den handler (Endlosschleife)
		}		
	};

	private static final int INTERVALL = 50; // Verzoegerung in ms
	private static final int LONG_INTERVALL = 5000; // Verzoegerung in ms
	private Data data; // Datencontainer
	private TextToSpeech tts;
	
	// Kamera
	private Camera camera = null;
	private SurfaceHolder holder = null;
	
	/*
	 * SIP
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	
	protected Sip sip = null;
	private boolean isInCall = false;
	private boolean startStopCallButtonReady = false;
	private ImageButton buttonStartStopCall;
	private OnClickListener buttonStartStopCall_OnClickListener = new OnClickListener(){

		@Override
		public void onClick(View v) {
			callButtonBehaviour();
			Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE);
			if(vibrator.hasVibrator())	
				vibrator.vibrate(50);
		}
		
	};
	
	
	/*
	 * SIP variables DONE
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Kamera
		final SurfaceView view = (SurfaceView) findViewById(R.id.view);
		view.setKeepScreenOn(true);
		holder = view.getHolder();
		buttonStartStopCall = (ImageButton) findViewById(R.id.imageButtonStartStopCall);
		buttonStartStopCall.setOnClickListener(buttonStartStopCall_OnClickListener);
		startSip();
		
		// TTS
		tts = new TextToSpeech(this, new OnInitListener() {
			@Override
			public void onInit(int status) {
				tts.setLanguage(Locale.GERMAN);
				tts.speak("Herzlich willkommen beim Virtuellen Begleiter. " +
						"Auf der oberen Bildschirmhälfte befindet sich der Knopf zum Anrufen ", TextToSpeech.QUEUE_FLUSH, null);
			}
		});
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		if(data == null)
			data = new Data(this);
		data.setStatus(true);
		data.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) this);
		data.getData();
		
		// prüfen, ob Kamera vorhanden ist
		camera = Camera.open();
		if (camera != null) {
			// Fix Camera Orientation
			setCameraDisplayOrientation(this, 0, camera);
			setCameraParameters();
			
			holder.addCallback(this);
			
		}
		gethandler.postDelayed(getrun,INTERVALL); // startet handler (run())!
		sethandler.postDelayed(setrun,LONG_INTERVALL);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		data.setStatus(false);
		data.locationManager.removeUpdates((LocationListener) this);
		
		tts.shutdown();
		
		if (camera != null) {
			holder.removeCallback(this);
			camera.stopPreview();
			camera.release();
			camera = null;
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
	}
	
    @Override
    protected void onStop() {
        super.onStop();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	sip.onDestroy();
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)	{
		
		switch(item.getItemId()) {
		
		case R.id.action_settings:
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
			
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * Following methods are for LocationListener Implementation
	 */
	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	/*
	 * Following methods are for camera implementation
	 */
	
	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			camera.setPreviewDisplay(holder);
			camera.startPreview();
		} catch (IOException e) {
			Log.e("VirtualCompanion", "surfaceCreated()", e);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if(camera!=null)
		camera.stopPreview();
		
	}
	
	private PreviewCallback precallback = new PreviewCallback() {

		@Override
		public void onPreviewFrame(byte[] stream, Camera camera) {
			try {
			 Camera.Parameters parameters = camera.getParameters();
             Size size = parameters.getPreviewSize();
             YuvImage image = new YuvImage(stream, ImageFormat.NV21, size.width, size.height, null);
             Rect rectangle = new Rect();
             rectangle.bottom = size.height;
             rectangle.top = 0;
             rectangle.left = 0;
             rectangle.right = size.width;
             ByteArrayOutputStream out2 = new ByteArrayOutputStream();
             image.compressToJpeg(rectangle, 90, out2);
             byte[] b = out2.toByteArray();
 			 String encodedImage = Base64.encodeToString(b, Base64.NO_WRAP);
 			 data.setPic(encodedImage);
			} catch (RuntimeException e) {
				Debug.doError("RuntimeExceptione: "+ e.toString());
			}
		}
	};
	
	public static void setCameraDisplayOrientation(Activity activity,
	         int cameraId, android.hardware.Camera camera) {
	     android.hardware.Camera.CameraInfo info =
	             new android.hardware.Camera.CameraInfo();
	     android.hardware.Camera.getCameraInfo(cameraId, info);
	     int rotation = activity.getWindowManager().getDefaultDisplay()
	             .getRotation();
	     int degrees = 0;
	     switch (rotation) {
	         case Surface.ROTATION_0: degrees = 0; break;
	         case Surface.ROTATION_90: degrees = 90; break;
	         case Surface.ROTATION_180: degrees = 180; break;
	         case Surface.ROTATION_270: degrees = 270; break;
	     }

	     int result;
	     if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
	         result = (info.orientation + degrees) % 360;
	         result = (360 - result) % 360;  // compensate the mirror
	     } else {  // back-facing
	         result = (info.orientation - degrees + 360) % 360;
	     }
	     camera.setDisplayOrientation(result);
	}
	
	private void setCameraParameters() {
		// TODO NULLPOINTER in camera
		camera.stopPreview();
		Camera.Parameters p = camera.getParameters();

		// Kleinste Previewaufloesung
		List<Camera.Size> previewlist = p.getSupportedPreviewSizes();
		Camera.Size previewsize = previewlist.get(previewlist.size() - 1);
		
		// Autofokus
		p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

		if(data.getResolution() == null)
			data.CamHasChanged(true);
		else if(data.getResolution().equals("low")) {
			tts.speak("Niedrige Auflösung", TextToSpeech.QUEUE_ADD, null);
			data.CamHasChanged(false);
		}
		else if(data.getResolution().equals("medium") || data.getResolution().equals("high")) {
			int i=1, minwidth=0, minheight=0;
			if(data.getResolution().equals("medium")) {
				tts.speak("Mittlere Auflösung", TextToSpeech.QUEUE_ADD, null);
				minwidth = 320;
				minheight = 240;
			} else if(data.getResolution().equals("high")) {
				tts.speak("Hohe Auflösung", TextToSpeech.QUEUE_ADD, null);
				minwidth = 640;
				minheight = 480;
			}
			
			while(previewsize.width < minwidth & previewsize.height < minheight) {
				previewsize = previewlist.get(previewlist.size() - (1 + i));
				i++;
			}
			data.CamHasChanged(false);
		}
		
		if(data.getFlashlight() & (data.getResolution() != null)) {
			p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			tts.speak("Kameralicht aktiviert", TextToSpeech.QUEUE_ADD, null);	
			data.CamHasChanged(false);
		} else if((!data.getFlashlight()) & (data.getResolution() != null)) {
			p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			tts.speak("Kameralicht deaktiviert", TextToSpeech.QUEUE_ADD, null);
			data.CamHasChanged(false);
		}
		
		p.setPreviewSize(previewsize.width, previewsize.height);
		Debug.doDebug("Auflösung: " + previewsize.width + " x " + previewsize.height);
		camera.setParameters(p);
		camera.startPreview();
	}
	
	/**
	 * Some routines for sip
	 */
	
	private void startSip()	{
	    sip = new Sip(this);
	}
	
	// Method for changing the call icon
	public void updateCallIcon()	{
		if(sip != null & sip.isSipRegistrated())	{
			isInCall = sip.isInCall();
			if(isInCall)
				buttonStartStopCall.setImageResource(R.drawable.phone_red_big);
			else
				buttonStartStopCall.setImageResource(R.drawable.phone_green_big);
		} else if(sip.isError())	{
			buttonStartStopCall.setImageResource(R.drawable.phone_error_big);
		} else	{
			buttonStartStopCall.setImageResource(R.drawable.phone_gray_big);
		}
		
		// enables the menu button
		startStopCallButtonReady = true;
	}
	
	// Behaviour selection for clicking the call button
	private void callButtonBehaviour()	{
		if(startStopCallButtonReady & sip.isSipRegistrated() & !isInCall)	{
    		sip.initiateAudioCall();
    		this.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    		
    		// this makes it impossible to execute the associated method multiple times
    		// at the same time by pressing multiple times at the icon before it
    		// changed its appearance
    		startStopCallButtonReady = false;
    	} else if(startStopCallButtonReady & sip.isSipRegistrated() & isInCall)	{
    		sip.endAudioCall();
    		this.setVolumeControlStream(AudioManager.STREAM_RING);
    		
    		// this makes it impossible to execute the associated method multiple times
    		// at the same time by pressing multiple times at the icon before it
    		// changed its appearance
    		startStopCallButtonReady = false;
    	}
	}

	public void openIncomingCallDialog()	{
		DialogFragment dialog = new IncomingCallFragment();
		dialog.show(getFragmentManager(), "IncomingCallFragment");
	}
	
	@Override
	public void onDialogPositiveClick(DialogFragment dialog) {
		sip.answerCall();
		this.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
		sip.endAudioCall();
	}
	
	/**
	 * Method of PreferenceChanged-Listener
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(sip != null)	{
			sip.onDestroy();
			sip = null;
			sip = new Sip(this);
		}
	}
	
	/**
	 * SIP Methods done
	 */
	
}
