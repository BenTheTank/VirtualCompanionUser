package de.virtualcompanion.user;

import android.util.Log;

/*
 * Diese Klasse enth�lt haupts�chlich Hilfsmittel und Methoden,
 * die zum Debuggen der Applikation n�tzlich sind.
 */

public class Debug {

	public static void doInfo(String text) {
		
		Log.i("VirtualCompanion", text);		
	}
	
	public static void doError(String text) {
		
		Log.e("VirtualCompanion", text);		
	}
	
	public static void doDebug(String text) {
		
		Log.d("VirtualCompanion", text);		
	}
}
