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
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Resources;
import android.locationprivacy.control.LocationPrivacyManager;
import android.locationprivacy.model.AbstractLocationPrivacyAlgorithm;
import android.locationprivacy.model.LocationPrivacyApplication;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.settings.R;
import com.google.android.gms.internal.v;

public class LocationPrivacyDialog extends Activity implements OnClickListener {

	private class DataFromWebservice implements Runnable {

		private static final double MINVOTES = 10;
		private static final double STARTHRESHOLD = 0.21;
		private static final String TAG = "DataFromWebservice";
		private String packagename;

		public DataFromWebservice(String packagename) {
			super();
			this.packagename = packagename;
		}

		@Override
		public void run() {
			final String HOST_ADDRESS = "https://"
					+ lpManager.getWebserviceHostAdress() + "/get";
			String urlString = HOST_ADDRESS;
			HashMap<Integer, AbstractLocationPrivacyAlgorithm> presetAlgorithms = lpManager
					.getPresetAlgorithms();
			urlString += "?app=" + packagename;
			if (!lpManager.isUseOnlineAlgorithm()) {
				urlString += "&street="
						+ presetAlgorithms.get(1).getConfiguration()
								.getInt("radius");
				urlString += "&district="
						+ presetAlgorithms.get(2).getConfiguration()
								.getInt("radius");
				urlString += "&city="
						+ presetAlgorithms.get(3).getConfiguration()
								.getInt("radius");
			}

			System.out.println(urlString);
			URL url;
			try {
				url = new URL(urlString);
			} catch (MalformedURLException e) {

				Log.e(TAG, "Error: could not build URL");
				Log.e(TAG, e.getMessage());
				Message msg = Message.obtain(mHandler, ERROR);
				msg.sendToTarget();
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
				// TODO Auto-generated catch block
				connection.disconnect();
				e.printStackTrace();
				Message msg = Message.obtain(mHandler, ERROR);
				msg.sendToTarget();
				return;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				connection.disconnect();
				e.printStackTrace();
				Message msg = Message.obtain(mHandler, ERROR);
				msg.sendToTarget();
				return;
			}
			connection.disconnect();

			HashMap<Integer, Integer> choosenPresets = new HashMap<Integer, Integer>();
			int[] values = new int[5];
			int[] stars = new int[5];
			for (int i = 0; i < 5; i++) {
				try {
					values[i] = json.getInt("" + i);
					System.out.println(i + " " + json.getInt("" + i));
				} catch (JSONException e) {
					Log.d(TAG, "Warning: No entry for preset " + i);
					System.out.println("Warning: No entry for preset " + i);
				}
			}
			int sum = 0;
			for (int i = 0; i < values.length; i++) {
				sum += values[i];
			}
			if (useStarsInDialog) {
				for (int i = 0; i < stars.length; i++) {
					stars[i] = (int) (((double) values[i]) / (sum * STARTHRESHOLD));
				}

				Message msg = Message.obtain(mHandler, RECIEVED_DATA, stars);
				msg.arg1 = sum;
				msg.sendToTarget();
			} else {
				Message msg = Message.obtain(mHandler, RECIEVED_DATA, values);
				msg.arg1 = sum;
				msg.sendToTarget();
			}

		}
	}
	protected static final int ERROR = 20;
	private static int notificationId = 0;
	protected static final int RECIEVED_DATA = 10;
	private static final String TAG = "LPDialog";
	private LPPresetConfigAdapter adapter;
	private LocationPrivacyApplication app;
	private View border;

	private TextView cancel;

	private int choosen;

	private TextView community;
	private TextView intro;
	private ListView list;

	private LocationPrivacyManager lpManager;
	private Handler mHandler;
	private boolean needRestartActivity = true;
	private Button ok_btn;
	private ProgressBar progress;
	Resources res;
	private boolean sharePrivacySettings;
	private boolean showCommunityAdvice;

	private boolean useStarsInDialog;

	private String getPresetConfigString(int communityPresetConf) {
		String[] preconfigs = res.getStringArray(R.array.preconfigs);
		return preconfigs[communityPresetConf];
	}

	@Override
	public void onClick(View v) {
		if (v == ok_btn) {
			app.setPresetConfig(choosen);
			lpManager.setApplication(app);
			if (sharePrivacySettings) {
				lpManager.addPackageForWebserviceUpdate(app.getPackagename());
			}
			lpManager.setDialogInUse(false);
			needRestartActivity = false;
			startService(new Intent(this, SendDataService.class));
			Intent data = new Intent();
			data.putExtra("packagename", app.getPackagename());
			data.putExtra("preset", app.getPresetConfig());
			setResult(1, data);
			this.finish();
		} else if (v == cancel) {
			lpManager.setDialogInUse(false);
			needRestartActivity = false;
			this.setResult(0);
			this.finish();
		} else {
			choosen = v.getId();
			adapter.setChecked(choosen);
			list.invalidateViews();
			ok_btn.setEnabled(true);
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.lp_dialog);
		lpManager = new LocationPrivacyManager(this);
		sharePrivacySettings = lpManager.isSharePrivacySettings();
		showCommunityAdvice = lpManager.isShowCommunityAdvice();

		res = getResources();
		Bundle extras = getIntent().getExtras();
		app = extras.getParcelable("app");
		useStarsInDialog = lpManager.isUseStarsInDialog();
		choosen = app.getPresetConfig();

		list = (ListView) findViewById(R.id.lp_dialog_listview);
		ok_btn = (Button) findViewById(R.id.lp_dialog_ok);
		intro = (TextView) findViewById(R.id.lp_dialog_intro);
		community = (TextView) findViewById(R.id.lp_dialog_community_value);
		cancel = (TextView) findViewById(R.id.lp_dialog_cancel);
		border = (View) findViewById(R.id.lp_dialog_border_vertical);
		progress = (ProgressBar) findViewById(R.id.lp_dialog_progress);
		ok_btn.setOnClickListener(this);

		intro.setText(app.getLabel(getPackageManager()) + " "
				+ res.getString(R.string.lp_dialog_intro));

		if (showCommunityAdvice) {
			new Thread(new DataFromWebservice(app.getPackagename())).start();
			community.setText(R.string.lp_dialog_get_from_webservice);
			community.setVisibility(View.VISIBLE);
			progress.setVisibility(View.VISIBLE);
		}
		ArrayList<Integer> icons = new ArrayList<Integer>();
		icons.add(R.drawable.lp_on_light);
		icons.add(R.drawable.lp_street_light);
		icons.add(R.drawable.lp_postalcode_light);
		icons.add(R.drawable.lp_city_light);
		icons.add(R.drawable.lp_off_light);

		adapter = new LPPresetConfigAdapter(icons, getResources()
				.getStringArray(R.array.preconfigs), this, this);
		if (app.getPresetConfig() > -1) {
			cancel.setVisibility(View.VISIBLE);
			cancel.setOnClickListener(this);
			border.setVisibility(View.VISIBLE);
			ok_btn.setEnabled(true);

			adapter.setChecked(app.getPresetConfig());
			needRestartActivity = false;

		}
		list.setAdapter(adapter);

		this.setFinishOnTouchOutside(false);

		
		if (useStarsInDialog) {
			mHandler = new Handler(getMainLooper()) {
				public void handleMessage(Message inputMessage) {
					if (inputMessage.what == RECIEVED_DATA) {
						progress.setVisibility(View.GONE);
						int[] stars = (int[]) inputMessage.obj;
						int sum = inputMessage.arg1;
						if (sum > 10) {
							adapter.setStars(stars);
							adapter.notifyDataSetChanged();
							community.setText(R.string.lp_dialog_stars);
						} else {
							community.setText(R.string.lp_dialog_user_choice_no_recommendation);
						}

					} else if (inputMessage.what == ERROR) {
						progress.setVisibility(View.GONE);
						community.setText(R.string.lp_dialog_error);
					}
				}
			};
		} else {
			mHandler = new Handler(getMainLooper()) {
				public void handleMessage(Message inputMessage) {
					if (inputMessage.what == RECIEVED_DATA) {
						progress.setVisibility(View.GONE);
						String[] presets = LocationPrivacyDialog.this.getResources()
								.getStringArray(R.array.preconfigs_short);
						int[] values = (int[]) inputMessage.obj;
						int sum = inputMessage.arg1;
						if (sum > 10) {

							// to Do: add String value
							String uservote = res
									.getString(R.string.lp_dialog_user_choice)
									+ " ";
							for (int i = 0; i < values.length; i++) {
								int max = 0;
								for (int j = 1; j < values.length; j++) {
									int valueTmp = values[j];
									if (values[max] < valueTmp) {
										max = j;
									}
								}
								int value = values[max];
								double percent = (double) value / (double) sum;
								if (percent > 0.2) {
									uservote +=  ((int) (percent * 100)) + "% " + presets[max] + ", ";
								} else {
									break;
								}
								values[max] = -1;
							}
							uservote = uservote.substring(0,
									uservote.length() - 2);
							community.setText(uservote);
						} else {
							community.setText(R.string.lp_dialog_user_choice_no_recommendation);
						}

					} else if (inputMessage.what == ERROR) {
						progress.setVisibility(View.GONE);
						community.setText(R.string.lp_dialog_error);
					}
				}
			};
		}
	}

	@Override
	protected void onPause() {
		System.out.println("onPause");
		if (needRestartActivity) {
			Intent intent = new Intent(this, LocationPrivacyDialog.class);
			intent.putExtra("app", app);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent,
					0);

			Notification noti = new Notification.Builder(this)
					.setContentTitle(
							getResources().getString(
									R.string.lp_notification_title))
					.setContentText(
							getResources().getString(R.string.lp_notification_))
					.setSmallIcon(R.drawable.ic_settings_locationprivacy)
					.setContentIntent(pIntent).setAutoCancel(true).build();

			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			notificationManager.notify(notificationId, noti);

		}
		super.onPause();
	}

}
