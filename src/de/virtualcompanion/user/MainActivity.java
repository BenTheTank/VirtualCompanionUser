package de.virtualcompanion.user;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity implements Runnable, LocationListener {

	// Handler fuer zeitverzoegertes senden
	private Handler handler = new Handler();
	private static final int INTERVALL = 2000; // Verzoegerung in ms
	private Data data; // Datencontainer
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if(data == null)
			data = new Data(this);
		data.setStatus(true);
		data.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) this);
		handler.postDelayed(this,INTERVALL); // startet handler (run())!
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		data.setStatus(false);
		data.locationManager.removeUpdates((LocationListener) this);
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
	
}
