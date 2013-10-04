package de.virtualcompanion.user;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.format.Formatter;

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
	private String ip = "0.0.0.0";
	private String network_type; // Der Datenempfangstyp (GSM, GPRS, 3G, etc.. )
	private boolean status; // Verbindung soll aktiv sein oder beendet
	private Date datum; // Aktuelle Zeit
	
	// Server
	private String httpurl; // HTTP Server address

	private Context context;
	private NetworkInfo netInf;
	private ConnectivityManager conMan;
	private SharedPreferences prefs;
	protected LocationManager locationManager;
	
	/* Konstanten */	
	private static final String TAG_TIMESTAMP = "timestamp";
	private static final String TAG_STATUS = "status";
	private static final String TAG_NAME = "name";
	private static final String TAG_IP = "ip";
	private static final String TAG_NETWORK = "network";
	private static final String TAG_PIC = "pic";
	private static final String TAG_LOC = "location";
	private static final String TAG_LOC_LONG = "long";
	private static final String TAG_LOC_LAT = "lat";
	private static final String TAG_LOC_ACC = "acc";
	private static final String TAG_LOC_ET = "et";
	private static final String TAG_LOC_ALT = "alt";
	private static final String TAG_LOC_BEAR = "bear";
	
	Data(Context context) {
	
		this.context = context;
		datum = new Date();
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		name = prefs.getString("username", "Unbekannt");
		httpurl = prefs.getString("httpserver", "http://virtuellerbegleiter.rothed.de/post.php");
		conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		network_type = getNetworkType();
		ip = getLocalIpAddress();
		
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
		Debug.doDebug("IP: " + getLocalIpAddress());
		Debug.doDebug("JSON: " + createJSON().toString());
	}
	
	public void updateData() {
		
		Debug.doDebug("updateData() called");
		datum = new Date();
		network_type = getNetworkType();
		location = getLocation();
		ip = getLocalIpAddress();	
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
		if(netInf != null) {
			if (netInf.getType() == 1 ) // Typ 1 = WIFI
				type = netInf.getTypeName();
			else
				type = netInf.getSubtypeName();
			return type;
		}
		return null;
	}
	
	private String getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                    String ip = Formatter.formatIpAddress(inetAddress.hashCode());
	                    return ip;
	                }
	            }
	        }
	    } catch (SocketException ex) {
	    	ex.printStackTrace();
	    }
	    return null;
	}
	
	private Location getLocation() {
		// Holt die Location fuer Daten
		Location mlocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		return mlocation;
	}
	
	public void setStatus(boolean status) {
		this.status = status;
	}
	
	public boolean isStatus() {
		return this.status;
	}
	
	private JSONObject createJSON() {
		JSONObject object = new JSONObject();
		JSONObject subobject = new JSONObject();
		try {
			subobject.put(TAG_LOC_ACC, location.getAccuracy());
			subobject.put(TAG_LOC_ALT, location.getAltitude());
			subobject.put(TAG_LOC_BEAR, location.getBearing());
			subobject.put(TAG_LOC_LAT, location.getLatitude());
			subobject.put(TAG_LOC_LONG, location.getLongitude());
			subobject.put(TAG_LOC_ET, location.getElapsedRealtimeNanos());
			
			object.put(TAG_TIMESTAMP, String.valueOf(datum.getTime()/1000));			
			object.put(TAG_STATUS, (status ? "TRUE" : "FALSE"));
			object.put(TAG_NAME, name);
			object.put(TAG_IP, ip);
			object.put(TAG_NETWORK, network_type);
			object.put(TAG_PIC, "tux.jpg");

			object.putOpt(TAG_LOC, subobject);
			
			return object;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/*		FOR SENDING		*/
	private String startSending(String strUrl, HttpClient httpclient, HttpPost httppost){
		try{
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		    nameValuePairs.add(new BasicNameValuePair("message", createJSON().toString()));
		    
		    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		    httpclient.execute(httppost);
		}catch (ClientProtocolException cpex) {
			return cpex.getMessage();
	    }
		catch (IOException ioex){
			return ioex.getMessage();
		}
		return "DONE";
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
