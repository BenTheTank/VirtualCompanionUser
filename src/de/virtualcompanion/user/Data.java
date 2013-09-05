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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

/*
 * Diese Klasse stellt den Datencontainer sowie die Methoden zum Versenden 
 * an den Webserver bereit.
 */

public class Data {

	// Handyausrichtung
	private String orientation; // Wie ist das Handy ausgerichtet
	
	// GPS Daten
	private String position; // Lang long
	private String quality; // Wie genau ist die Position
	
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

	Data(Context context) {
	
		this.context = context;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		datum = new Date();
		name = prefs.getString("username", "");
		httpurl = prefs.getString("httpserver", "");
		ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		netInf = conMan.getActiveNetworkInfo();
		if (netInf.getType() == 1 ) // Typ 1 = WIFI
			network_type = netInf.getTypeName();
		else
			network_type = netInf.getSubtypeName();
		
	}
	
	// For Debugging
	public void publishData() {
		
		Debug.doDebug("Datum: " + datum.getTime()/1000);
		Debug.doDebug("Name: " + name);
		Debug.doDebug("HTTP-URL: " + httpurl);
		Debug.doDebug("Netzwerktyp: " + network_type);
	}
	
	public void updateData() {
		
	}
	
	public void sendData() {
		
		if (netInf != null && netInf.isConnected())
			new SendToWebpage().execute(httpurl);
		else
			Debug.doError("No Network Connection available");
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
