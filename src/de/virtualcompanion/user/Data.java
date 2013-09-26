package de.virtualcompanion.user;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

/*
 * Diese Klasse stellt den Datencontainer sowie die Methoden zum Versenden 
 * an den Webserver bereit.
 * 
 * Ablauf: 
 * 1. Container erstmalig erstellen
 * dann immer wieder:
 * - fuellen mit updateData(x,y,z);
 * - senden mit sendData(x,y,z);
 */

public class Data {

	// Handyausrichtung
	private String orientation; // Wie ist das Handy ausgerichtet
	
	// GPS Daten
	private Location location; // Position
	
	// Benutzerdaten
	private String name; // Simpler Benutzername
	private String ip;
	private String network_type; // Der Datenempfangstyp (GSM, GPRS, 3G, etc.. )
	private String verbindung; // Verbindung soll aktiv sein oder beendet
	private Date datum; // Aktuelle Zeit
	
	// Server
	private String httpurl; // HTTP Server address

	private Context context;
	private NetworkInfo netInf;
	private ConnectivityManager conMan;
	
	private SharedPreferences prefs;
	protected LocationManager locationManager;
	private String locationProvider;
	
	Data(Context context) {
	
		this.context = context;
		datum = new Date();
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		name = prefs.getString("username", "");
		httpurl = prefs.getString("httpserver", "");
		conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		network_type = getNetworkType();
		
		// Zugriff auf den Location Manager
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, (LocationListener) context);
	}
	
	// For Debugging
	public void publishData() {
		
		Debug.doDebug("publishData() called");
		Debug.doDebug("Datum: " + datum.getTime()/1000);
		Debug.doDebug("Name: " + name);
		Debug.doDebug("HTTP-URL: " + httpurl);
		Debug.doDebug("Netzwerktyp: " + network_type);
		Debug.doDebug("Location: " + location.toString());
	}
	
	public void updateData() {
		
		Debug.doDebug("updateData() called");
		datum = new Date();
		network_type = getNetworkType();
		location = getLocation();
	}
	
	public void sendData() {
		
		Debug.doDebug("sendData() called");
		if (netInf != null && netInf.isConnected())
			new SendToWebpage().execute(httpurl);
		else
			Debug.doError("No Network Connection available");
	}
	
	private String getNetworkType() {
		// Holt sich den Netzwerktyp fuer Daten
		String type;
		netInf = conMan.getActiveNetworkInfo();
		if (netInf.getType() == 1 ) // Typ 1 = WIFI
			type = netInf.getTypeName();
		else
			type = netInf.getSubtypeName();
		return type;
	}
	
private Location getLocation() {
		// Holt die Location fuer Daten
		locationProvider = LocationManager.GPS_PROVIDER;
		locationManager.requestLocationUpdates(locationProvider, 0, 0, (LocationListener) context);
		Location mlocation = locationManager.getLastKnownLocation(locationProvider);
		return mlocation;
	}
	
	private String startSending(String strUrl, HttpClient httpclient, HttpPost httppost){
		String sendString = String.valueOf(datum.getTime()/1000);
		try{
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		    nameValuePairs.add(new BasicNameValuePair("message", sendString));
		    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		    httpclient.execute(httppost);
		}catch (ClientProtocolException cpex) {
			return cpex.getMessage();
	    }
		catch (IOException ioex){
			return ioex.getMessage();
		}
		return sendString;
	}
	
	private class SendToWebpage extends AsyncTask<String, String, String>{
		@Override
		protected String doInBackground(String... httpurl){
			HttpClient httpclient = new DefaultHttpClient();
	   	    HttpPost httppost = new HttpPost(httpurl[0]);
			publishProgress(startSending(httpurl[0], httpclient, httppost));
			return "Done";
		}
		
		@Override
		protected void onProgressUpdate(String... string){
		}
		
		@Override
		protected void onPostExecute(String result){
		}
	}
	
}
