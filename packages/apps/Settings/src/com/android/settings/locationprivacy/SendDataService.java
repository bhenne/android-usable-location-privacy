/*
 * Copyright (C) 2014 B. Henne, C. Kater,
 *   Distributed Computing & Security Group,
 *   Leibniz Universitaet Hannover, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.locationprivacy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.conn.ssl.SSLSocketFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.locationprivacy.control.LocationPrivacyManager;
import android.locationprivacy.model.LocationPrivacyApplication;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class SendDataService extends Service {

	public class SendData implements Runnable {
		private static final String TAG = "Runnable SendData";
		private ArrayList<String> appsToSend;
		private Context context;
		private String HOST_ADDRESS;
		private LocationPrivacyManager lpManager;

		public SendData() {
			super();
			context = SendDataService.this;

		}

		private Account getAccount(AccountManager accountManager) {
			Account[] accounts = accountManager.getAccountsByType("com.google");
			Account account;
			if (accounts.length > 0) {
				account = accounts[0];
			} else {
				account = null;
			}
			return account;
		}

		String getEmail() {
			AccountManager accountManager = AccountManager.get(context);
			Account account = getAccount(accountManager);
			if (account == null) {
				return null;
			} else {
				return account.name;
			}
		}

		private boolean isWlanConnected() {
			ConnectivityManager connectivityManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = null;
			if (connectivityManager != null) {
				networkInfo = connectivityManager
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			}
			return networkInfo == null ? false : networkInfo.isConnected();
		}

		@Override
		public void run() {
			lpManager = new LocationPrivacyManager(context);
			appsToSend = lpManager.getAppsToSend();
			HOST_ADDRESS = "https://" + lpManager.getWebserviceHostAdress()
					+ "/set";
			if (isWlanConnected()
					&& GooglePlayServicesUtil
							.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
					&& lpManager.isWebhostShareSettings()) {
				// get Access Token for OAuth2
				String token = null;
				String email = getEmail();
				if (email != null) {
					try {
						token = GoogleAuthUtil
								.getToken(context, email,
										"oauth2:https://www.googleapis.com/auth/userinfo.profile");
					} catch (UserRecoverableAuthException e) {
						Intent i = new Intent(context,
								UserRecoverableAuth.class);
						i.putExtra("authIntent", e.getIntent());
						i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						SendDataService.this.startActivity(i);
						e.printStackTrace();
						return;
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
						return;
					} catch (GoogleAuthException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
						return;
					}
					if (token != null) {

						ArrayList<String> finishedApps = new ArrayList<String>();

						// Send Data to Webservice
						while (!appsToSend.isEmpty()) {
							String packagename = appsToSend.remove(0);
							LocationPrivacyApplication app = lpManager
									.getApplication(packagename);

							if (app != null) {
								String urlString = HOST_ADDRESS;
								urlString += "?app=" + packagename;
								urlString += "&preset=" + app.getPresetConfig();
								urlString += "&accesstoken=" + token;
								if (!lpManager.isUseOnlineAlgorithm()) {
									urlString += "&radius="
											+ lpManager.getPresetConfiguration(app.getPresetConfig());
								}
								URL url;
								try {
									url = new URL(urlString);
								} catch (MalformedURLException e) {
									Log.e(TAG, "Error: could not build URL");
									Log.e(TAG, e.getMessage());
									continue;
								}
								HttpsURLConnection connection = null;

								try {
									connection = (HttpsURLConnection) url
											.openConnection();
									connection
											.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
									InputStream is = connection
											.getInputStream();
									BufferedReader reader = new BufferedReader(
											new InputStreamReader(is));
									String line = reader.readLine();
									if (line.startsWith("error:")) {
										Log.d(TAG, line);
										break;
									}
								} catch (IOException e) {
									connection.disconnect();
									break;
								}
								connection.disconnect();
							}
							finishedApps.add(packagename);
						}

						// Delete Sended Data
						for (String packagename : finishedApps) {
							lpManager.deleteAppToSend(packagename);
						}
					}
				}
				SendDataService.this.stopSelf();
			}
		}

	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		new Thread(new SendData()).start();

		return super.onStartCommand(intent, flags, startId);
	}
}
