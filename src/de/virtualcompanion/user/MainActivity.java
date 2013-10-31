package de.virtualcompanion.user;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainActivity extends Activity implements Runnable, LocationListener, SurfaceHolder.Callback {

	// Handler fuer zeitverzoegertes senden
	private Handler handler = new Handler();
	private static final int INTERVALL = 2000; // Verzoegerung in ms
	private Data data; // Datencontainer
	
	// Kamera
	private Camera camera = null;
	private SurfaceHolder holder = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Kamera
		final SurfaceView view = (SurfaceView) findViewById(R.id.view);
		view.setKeepScreenOn(true);
		holder = view.getHolder();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if(data == null)
			data = new Data(this);
		data.setStatus(true);
		data.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) this);
		
		// prüfen, ob Kamera vorhanden ist
		camera = Camera.open();
		if (camera != null) {
			// Fix Camera Orientation
			setCameraDisplayOrientation(this, 0, camera);
			
			Camera.Parameters p = camera.getParameters();

			// Kleinste Previewaufloesung
			List<Camera.Size> previewlist = p.getSupportedPreviewSizes();
			Camera.Size previewsize = previewlist.get(previewlist.size() - 1);

			int i=1;
			// TODO: Qualität vom Helfer senden lassen: 3 Stufen bis 640 x 480
			while(previewsize.width <= 200 & previewsize.height <= 100) {
				previewsize = previewlist.get(previewlist.size() - (1 + i));
				i++;
			}
			p.setPreviewSize(previewsize.width, previewsize.height);
			Debug.doDebug("Auflösung: " + previewsize.width + " x " + previewsize.height);
			camera.setParameters(p);
			holder.addCallback(this);
			
		}
		handler.postDelayed(this,INTERVALL); // startet handler (run())!
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		data.setStatus(false);
		data.locationManager.removeUpdates((LocationListener) this);
		
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

	@Override
	public void run() {
		// Bildle machen
		if(camera!=null)
			camera.setOneShotPreviewCallback(precallback);

		// Methode fuer den Handler laeuft alle INTERVALL ms
		data.updateData();
		data.publishData();
		data.sendData();
		
		if (data.isStatus())
			handler.postDelayed(this,INTERVALL); // startet nach INTERVALL wieder den handler (Endlosschleife)
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
}
