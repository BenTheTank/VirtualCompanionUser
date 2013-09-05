package de.virtualcompanion.user;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
/**
 * This is Service for managing the communication.
 * For more information about Services: http://developer.android.com/guide/components/services.html
 */
public class ComService extends Service {
	
	// Foreground-Stuff
	NotificationManager notificationManager = null;
	private static final int NOTIFICATION_ID = 1;
	Notification notification;
	// ######################
	
	/*
	 * ############################################################################
	 * Stuff for binding to the Service
	 */
	
	private final IBinder mBinder = new ComBinder();
	public class ComBinder extends Binder {
		ComService getService()	{
			// Return this instance of LocalService so clients can call public methods
			return ComService.this;
		}
	}
	
	public IBinder onBind(Intent intent)	{
		// This is called when you bind to a service so you can directly interact with it (use methods etc.)
		
		return mBinder;
	}
	
	/*
	 * ADD TO ACITIVITY:
	 * #################
	 * 
	ComService comService;
	boolean comServiceBound = false;
	
	// Defines callbacks for service binding, passed to bindService()
	private ServiceConnection comConnection = new ServiceConnection()	{
		
		@Override
		public void onServiceConnected(ComponentName className, IBinder service)	{
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			ComBinder binder = (ComBinder) service;
			comService = binder.getService();
			comServiceBound = true;
		}
		
		@Override
		public void onServiceDisconnected(ComponentName arg0)	{
			comServiceBound = false;
		}
	};
	
	
	* in the method you want to bind to the service:
	// Bind to ComService
    Intent intent = new Intent(this, ComService.class);
    bindService(intent, comConnection, Context.BIND_AUTO_CREATE);
	
	 */
	
	/*
	 * Binding DONE
	 * #############################################################################
	 */
	
	@Override
	public void onCreate()	{
		
		//TODO
	}
	
	public int onStartCommand(Intent intent, int flags, int startId)	{
		// Called by the system every time a client explicitly starts the service by calling
		// startService(Intent), providing the arguments it supplied and a unique integer token
		// representing the start request. Do not call this method directly.
		// For further information: http://developer.android.com/reference/android/app/Service.html#onStartCommand%28android.content.Intent,%20int,%20int%29
		// TODO
	}
	
	@Override
	public void onDestroy()	{
		// TODO
	}
	
	
	/*
	 * ##################################################################################
	 * FOREGROUND
	 */
	public void enableForeground()	{
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		  
		// Sets an ID for the notification, so it can be updated
		  
		// With this we generate a notification bar entry
		// so our service doesn't get killedld();
		notification = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.icon)
				.setContentTitle(getString(R.string.app_name))
				.build();
		startForeground(NOTIFICATION_ID, notification);
	}
	
	/*
	 * FOREGROUND DONE
	 * ####################################################################################
	 */
	
}
