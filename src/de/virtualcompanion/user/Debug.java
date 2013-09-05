package de.virtualcompanion.user;

import android.util.Log;

/*
 * Diese Klasse enthält hauptsächlich Hilfsmittel und Methoden,
 * die zum Debuggen der Applikation nützlich sind.
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
