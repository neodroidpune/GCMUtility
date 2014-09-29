/**
 * This library require google-play-services_lib
 */
package com.neo.gcmutility;

import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * @author Mohsin Sayyed 
 * @version 1.0
 */
public class GCMRegisterUtility {

	private static GoogleCloudMessaging gcm;
	private static final String TAG = "GCMRegisterUtility";
	private static String regid; 
	private static Context context = null;
	private static SharedPreferences prefs = null;
	private static final String REGISTERED_ID 		  = 	"REGISTERED_ID";
	private static final String PROPERTY_APP_VERSION  = 	"PROPERTY_APP_VERSION";


	/** Gets the current registration ID for application on GCM service.
	 * <p>
	 * @param  senderId senderId.
	 * @param  mContext application's context.
	 * @param  listner  The callback that will run.
	 * @return GCMRegisterListner implementor.
	 */
	public static void getGCMRegisteredId(String senderId ,Context mContext,GCMRegisterListner listner){

		GCMRegisterUtility.context = mContext;

		// Check device for Play Services APK.
		if (checkPlayServices(listner)) {

			// If this check succeeds, proceed with normal processing.
			// Otherwise, prompt user to get valid Play Services APK.
			gcm	    = GoogleCloudMessaging.getInstance(GCMRegisterUtility.context);
			regid   = getRegistrationId();

			if (regid!=null && regid.equalsIgnoreCase("")) {
				registerInBackground(listner,senderId);
			}else{
				listner.onPostRegister(regid);
			}

		}else{
			listner.onError("Google play service not available !!!");
		}

	}

	/**
	 * Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	private static boolean checkPlayServices(GCMRegisterListner lisner) {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(GCMRegisterUtility.context);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				/*GooglePlayServicesUtil.getErrorDialog(resultCode, context,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();*/

				lisner.onError("This device is not supported");

			} else {
				Log.i("GCM", "This device is not supported.");

				lisner.onError("This device is not supported");
			}
			return false;
		}
		return true;
	}

	/**
	 * Gets the current registration ID for application on GCM service.
	 * <p>
	 * If result is empty, the app needs to register.
	 *
	 * @return registration ID, or empty string if there is no existing
	 *         registration ID.
	 */
	private static String getRegistrationId() {

		String registrationId = getSharedPrefrences().getString(REGISTERED_ID,null);
		if (registrationId!=null && !registrationId.equalsIgnoreCase("")) {
			Log.i(TAG, "Registration not found.");
			return "";
		}else{

			System.out.println("ID : "+registrationId);
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersion =getSharedPrefrences().getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
		int currentVersion = getAppVersion();
		if (registeredVersion != currentVersion) {
			Log.i(TAG, "App version changed.");

			return "";
		}
		return registrationId;
	}

	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and app versionCode in the application's
	 * shared preferences.
	 */
	private static void registerInBackground(GCMRegisterListner listner,String senderId) {

		new RegisterGCMAsync(listner,senderId).execute();

	}


	private static class  RegisterGCMAsync extends AsyncTask<Void, String, String>{

		private GCMRegisterListner listner;
		private String senderId;
		private Context context;

		public RegisterGCMAsync(GCMRegisterListner listner,String senderId){

			this.listner  = listner; 
			this.senderId = senderId;
		}


		@Override
		protected String doInBackground(Void... params) {
			String msg = "";
			try {
				if (gcm == null) {
					gcm = GoogleCloudMessaging.getInstance(context);
				}
				regid = gcm.register(senderId);


				msg =  regid;

				// You should send the registration ID to your server over HTTP,
				// so it can use GCM/HTTP or CCS to send messages to your app.
				// The request to your server should be authenticated if your app
				// is using accounts.

				// For this demo: we don't need to send it because the device
				// will send upstream messages to a server that echo back the
				// message using the 'from' address in the message.

				// Persist the regID - no need to register again.
				storeRegistrationId(regid);

			} catch (IOException ex) {
				msg = "Error :" + ex.getMessage();

				// If there is an error, don't just keep trying to register.
				// Require the user to click a button again, or perform
				// exponential back-off.
			}
			return msg;
		}

		@Override
		protected void onPostExecute(String msg) {

			if(msg.contains("Error")){

				listner.onError(msg);

			}else{

				listner.onPostRegister(msg);
			}

		}

	}

	/**
	 * Stores the registration ID and app versionCode in the application's
	 * {@code SharedPreferences}.
	 *
	 * @param regId registration ID
	 */
	private static void storeRegistrationId(String regId) {
		int appVersion = getAppVersion();
		Log.i(TAG, "Saving regId on app version " + appVersion);

		getSharedPrefrences().edit().putString(REGISTERED_ID, regId);
		getSharedPrefrences().edit().putInt(PROPERTY_APP_VERSION, appVersion);
		getSharedPrefrences().edit().commit();

	}


	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion() {
		try {
			PackageInfo packageInfo = GCMRegisterUtility.context.getPackageManager()
					.getPackageInfo(GCMRegisterUtility.context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}


	public interface GCMRegisterListner{

		public void	onPreRegister();
		public void onPostRegister(String registeredId);
		public void onError(String error);
	}

	private static SharedPreferences getSharedPrefrences(){

		if(prefs == null){

			prefs = GCMRegisterUtility.context.getSharedPreferences("GCM_PREF", Context.MODE_PRIVATE);
		}

		return prefs;
	}

}
