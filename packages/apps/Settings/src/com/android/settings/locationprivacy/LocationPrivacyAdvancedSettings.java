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
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.locationprivacy.control.LocationPrivacyManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class LocationPrivacyAdvancedSettings extends SettingsPreferenceFragment
		implements OnPreferenceClickListener, OnPreferenceChangeListener {
	private class InfoFromWebservice implements Runnable {

		private static final String TAG = "InfoFromWebservice";

		private String host;

		public InfoFromWebservice(String host) {
			super();
			this.host = host;
		}

		@Override
		public void run() {
			final String HOST_ADDRESS = "https://" + host + "/info";

			URL url;
			try {
				url = new URL(HOST_ADDRESS);
			} catch (MalformedURLException e) {
				Message msg = Message.obtain(mHandler, WEBSERVICE_ERROR);
				msg.sendToTarget();
				Log.e(TAG, "Error: could not build URL");
				Log.e(TAG, e.getMessage());
				return;
			}
			HttpsURLConnection connection = null;
			JSONObject json;
			try {
				connection = (HttpsURLConnection) url.openConnection();
				connection
						.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
				InputStream is = connection.getInputStream();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is));
				String line = reader.readLine();
				System.out.println("Line " + line);
				json = new JSONObject(line);
			} catch (IOException e) {
				Message msg = Message.obtain(mHandler, WEBSERVICE_ERROR);
				msg.sendToTarget();
				// TODO Auto-generated catch block
				connection.disconnect();
				e.printStackTrace();
				return;
			} catch (JSONException e) {
				Message msg = Message.obtain(mHandler, WEBSERVICE_ERROR);
				msg.sendToTarget();
				// TODO Auto-generated catch block
				connection.disconnect();
				e.printStackTrace();
				return;
			}
			connection.disconnect();

			boolean shareSettings = false;
			boolean showCommunityAdvice = false;
			try {
				shareSettings = (Boolean) json.get("shareSettings");
				showCommunityAdvice = (Boolean) json.get("showCommunityAdvice");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			lpManager.setWebserviceHostAdress(host);
			lpManager.setWebhostShareSettings(shareSettings);
			lpManager.setWebhostShowCommunityAdvice(showCommunityAdvice);
			Message msg = Message.obtain(mHandler, WEBSERVICE_OK);
			msg.arg1 = showCommunityAdvice ? 1 : 0;
			msg.arg2 = shareSettings ? 1 : 0;
			msg.sendToTarget();
		}
	}

	private static final int GOOGLE_PLAY = 50;
	private static final int WEBSERVICE_ERROR = 0;
	private static final int WEBSERVICE_OK = 10;

	private LocationPrivacyManager lpManager;
	private Handler mHandler;
	private PreferenceScreen screen;

	private void addNotification(int id, int textID) {
		String text = getResources().getString(textID);
		Intent intent = new Intent(getActivity(), getActivity().getClass());
		intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
				LocationPrivacySettings.class.getName());
		intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pIntent = PendingIntent.getActivity(getActivity(), 0,
				intent, 0);

		Notification noti = new Notification.Builder(getActivity())
				.setContentTitle(
						getResources().getString(
								R.string.lp_webservice_notification_title))
				.setSmallIcon(R.drawable.ic_settings_locationprivacy)
				.setStyle(new Notification.BigTextStyle().bigText(text))
				.setContentIntent(pIntent).setAutoCancel(true).build();

		NotificationManager notificationManager = (NotificationManager) getSystemService(getActivity().NOTIFICATION_SERVICE);
		notificationManager.cancel(WEBSERVICE_ERROR);
		notificationManager.cancel(WEBSERVICE_OK);
		notificationManager.cancel(GOOGLE_PLAY);
		notificationManager.notify(id, noti);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		lpManager = new LocationPrivacyManager(getActivity());
		mHandler = new Handler(getActivity().getMainLooper()) {
			public void handleMessage(Message inputMessage) {
				Resources res = getResources();
				if (inputMessage.what == WEBSERVICE_ERROR) {
					addNotification(WEBSERVICE_ERROR,
							R.string.lp_webservice_error);
				} else if (inputMessage.what == WEBSERVICE_OK) {
					boolean showCommunityAdvice = inputMessage.arg1 == 1;
					boolean shareSettings = inputMessage.arg2 == 1;
					lpManager.setShowCommunityAdvice(showCommunityAdvice);
					lpManager.setSharePrivacySettings(shareSettings);
					refresh();

					if (shareSettings && showCommunityAdvice) {
						addNotification(WEBSERVICE_OK,
								R.string.lp_webservice_both);
					} else if (shareSettings) {
						addNotification(WEBSERVICE_OK,
								R.string.lp_webservice_share);
					} else if (showCommunityAdvice) {
						addNotification(WEBSERVICE_OK,
								R.string.lp_webservice_show);
					} else {
						addNotification(WEBSERVICE_OK,
								R.string.lp_webservice_nothing);
					}
				}
			}
		};
		screen = getPreferenceScreen();
		if (screen != null) {
			screen.removeAll();
		}
		addPreferencesFromResource(R.xml.locationprivacy_advanced_settings);
		screen = getPreferenceScreen();

		refresh();

	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		System.out.println(preference);
		if (preference.getKey().equals(
				"lp_settings_advanced_useonlinealgorithm")) {
			lpManager.setUseOnlineAlgorithm((Boolean) newValue);
			startActivity(new Intent(getActivity(),
					LocationPrivacyOnlineInfoActivity.class));
		} else if (preference.getKey().equals(
				"lp_settings_advanced_shareprivacyaettings")) {
			lpManager.setSharePrivacySettings((Boolean) newValue);
		} else if (preference.getKey().equals(
				"lp_settings_advanced_showcommunityadvice")) {
			lpManager.setShowCommunityAdvice((Boolean) newValue);
		} else if (preference.getKey().equals(
				"lp_settings_advanced_showcommunityadvice")) {
			lpManager.setShowCommunityAdvice((Boolean) newValue);
		} else if (preference.getKey()
				.equals("lp_settings_advanced_webservice")) {
			new Thread(new InfoFromWebservice((String) newValue)).start();
			lpManager.setWebhostShareSettings(false);
			lpManager.setWebhostShowCommunityAdvice(false);
			((CheckBoxPreference) screen
					.findPreference("lp_settings_advanced_showcommunityadvice"))
					.setEnabled(false);
			((CheckBoxPreference) screen
					.findPreference("lp_settings_advanced_shareprivacyaettings"))
					.setEnabled(false);
		} else if (preference.getKey().equals("lp_settings_advanced_stars")) {
			lpManager.setUseStarsInDialog((Boolean) newValue);
		}
		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals("lp_settings_advanced_reset")) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(R.string.lp_settings_advanced_reset_warning)
					.setTitle(R.string.lp_settings_advanced_reset_warning_title)
					.setPositiveButton(R.string.ok,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									lpManager.resetToFactoryDefaults();
									lpManager = new LocationPrivacyManager(
											getActivity());
									startFragment(getParentFragment(),
											LocationPrivacySettings.class
													.getName(), 0, new Bundle());

								}
							})
					.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// Do Nothing

								}
							});
			AlertDialog dialog = builder.create();
			dialog.show();

		} else if (preference.getKey().equals("lp_settings_advanced_settings")) {
			if (GooglePlayServicesUtil
					.isGooglePlayServicesAvailable(getActivity()) != ConnectionResult.SUCCESS) {
				addNotification(GOOGLE_PLAY,
						R.string.lp_settings_google_play_service);
			}
		}
		return true;
	}

	private void refresh() {
		for (int i = 0; i < screen.getPreferenceCount(); i++) {
			Preference pref = screen.getPreference(i);
			if (pref != null) {
				System.out.println(pref);
				String key = pref.getKey();
				if (key != null) {
					System.out.println(key);
					if (key.equals("lp_settings_advanced_useonlinealgorithm")) {
						CheckBoxPreference checkPref = (CheckBoxPreference) pref;
						checkPref.setChecked(lpManager.isUseOnlineAlgorithm());
					} else if (key
							.equals("lp_settings_advanced_shareprivacyaettings")) {
						CheckBoxPreference checkPref = (CheckBoxPreference) pref;
						checkPref
								.setChecked(lpManager.isSharePrivacySettings());
						checkPref
								.setEnabled(GooglePlayServicesUtil
										.isGooglePlayServicesAvailable(getActivity()) == ConnectionResult.SUCCESS
										&& lpManager.isWebhostShareSettings()
										&& AccountManager
												.get(getActivity())
												.getAccountsByType("com.google").length >= 1);
					} else if (key
							.equals("lp_settings_advanced_showcommunityadvice")) {
						CheckBoxPreference checkPref = (CheckBoxPreference) pref;
						checkPref.setChecked(lpManager.isShowCommunityAdvice());
						checkPref.setEnabled(lpManager.isShowCommunityAdvice());
					} else if (key.equals("lp_settings_advanced_stars")) {
						CheckBoxPreference checkPref = (CheckBoxPreference) pref;
						checkPref.setChecked(lpManager.isUseStarsInDialog());
					} else if (key.equals("lp_settings_advanced_webservice")) {
						EditTextPreference editTextPref = (EditTextPreference) pref;
						editTextPref.setText(""
								+ lpManager.getWebserviceHostAdress());
					} else if (key.equals("lp_settings_advanced_setpreset_min")) {
						EditTextPreference editTextPref = (EditTextPreference) pref;
						editTextPref.setText("" + lpManager.getMinDistance());
					}
					pref.setOnPreferenceChangeListener(this);
					pref.setOnPreferenceClickListener(this);
				}

			}
		}

	}
}
