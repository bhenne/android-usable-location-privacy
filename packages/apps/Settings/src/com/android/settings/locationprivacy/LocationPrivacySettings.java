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

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.locationprivacy.control.LocationPrivacyManager;
import android.locationprivacy.model.LocationPrivacyApplication;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/**
 * LocationPrivacySettings provides GUI of main configuration dialog of location
 * privacy framework
 * 
 * @author Christian Kater
 * 
 */
public class LocationPrivacySettings extends SettingsPreferenceFragment
		implements OnPreferenceClickListener {

	private static final int APP_REQUEST = 30;
	private List<LocationPrivacyApplication> apps;
	private int comparator;
	private final Comparator<LocationPrivacyApplication> compareLabel = new Comparator<LocationPrivacyApplication>() {

		@Override
		public int compare(LocationPrivacyApplication o1,
				LocationPrivacyApplication o2) {

			return o1.getLabel(pm).toLowerCase()
					.compareTo(o2.getLabel(pm).toLowerCase());
		}
	};

	private final Comparator<LocationPrivacyApplication> compareLastAccess = new Comparator<LocationPrivacyApplication>() {

		@Override
		public int compare(LocationPrivacyApplication o1,
				LocationPrivacyApplication o2) {
			Calendar c1 = lpManager.getLastAccess(o1.getPackagename());
			Calendar c2 = lpManager.getLastAccess(o2.getPackagename());
			int compare = c2.compareTo(c1);
			if(compare == 0){
				return compareLabel.compare(o1, o2);
			}
			return compare;
		}
	};

	private final Comparator<LocationPrivacyApplication> comparePrecision = new Comparator<LocationPrivacyApplication>() {

		@Override
		public int compare(LocationPrivacyApplication o1,
				LocationPrivacyApplication o2) {
			if(o1.getPresetConfig() < o2.getPresetConfig()){
				return -1;
			} else if(o1.getPresetConfig() > o2.getPresetConfig()) {
				return 1;
			} else  {
				return compareLabel.compare(o1, o2);
			}
		}
	};
	
	private final Comparator<LocationPrivacyApplication> compare28Days = new Comparator<LocationPrivacyApplication>() {

		@Override
		public int compare(LocationPrivacyApplication o1,
				LocationPrivacyApplication o2) {
			int count1 = count(lpManager.getLocationAccessStatistic(o1
					.getPackagename()));
			int count2 = count(lpManager.getLocationAccessStatistic(o2
					.getPackagename()));
			if (count1 == count2) {
				return compareLabel.compare(o1, o2);
			} else {
				return count2 - count1;
			}
		}
	};
	
	private int count(HashMap<Calendar, Integer> locationAccessStatistic) {
		int sum = 0;
		for (Integer value : locationAccessStatistic.values()) {
			sum += value;
		}
		return sum;
	}

	private LocationPrivacyManager lpManager;

	private PackageManager pm;

	private PreferenceScreen screen;

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == APP_REQUEST && resultCode == 1) {
			String packagename = data.getStringExtra("packagename");
			String appKey = "app_" + packagename;
			int preset = data.getIntExtra("preset", -1);
			if (appKey != null && preset >= 0) {
				LocationPrivacyAppPreference pref = (LocationPrivacyAppPreference) screen
						.findPreference(appKey);
				pref.setPreset(preset);
				apps = lpManager.getApplications();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setHasOptionsMenu(true);
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);

		pm = getActivity().getPackageManager();
		lpManager = new LocationPrivacyManager(getActivity());
		addPreferencesFromResource(R.xml.locationprivacy_settings);
		screen = getPreferenceScreen();

		screen.setOrderingAsAdded(true);
		comparator = 0;
		refresh();
		lpManager.cleanDatabase();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.location_privacy_settings_menu, menu);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (screen != null) {
			screen.removeAll();
		}
		screen = null;
		lpManager = null;
		apps = null;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.lp_menu_label_app:
			comparator = 0;
			refresh();
			Log.d("LpSettings", "order app by name");
			break;

		case R.id.lp_menu_last_access_app:
			comparator = 1;
			refresh();
			Log.d("LpSettings", "order app by last access");
			break;
		case R.id.lp_menu_precision_app:
			comparator = 2;
			refresh();
			Log.d("LpSettings", "order app by location precision");
			break;
		case R.id.lp_menu_number_access_28_days_app:
			comparator = 3;
			refresh();
			Log.d("LpSettings", "order app number of location access in last 28 days");
			break;
		}		
		return super.onOptionsItemSelected(item);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.preference.Preference.OnPreferenceClickListener#onPreferenceClick
	 * (android.preference.Preference)
	 */
	public boolean onPreferenceClick(Preference preference) {
		System.out.println(preference);
		if (preference.getKey().startsWith("app_")) {
			Intent i = new Intent(getActivity(), LocationPrivacyDialog.class);
			System.out.println(LocationPrivacyDialog.class);
			System.out.println(LocationPrivacyDialog.class.getName());
			LocationPrivacyAppPreference lpPref = (LocationPrivacyAppPreference) preference;
			i.putExtra("app", lpPref.getApp());
			startActivityForResult(i, APP_REQUEST);
		}
		return true;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (screen != null) {
			refresh();
		}
	}

	private Comparator<LocationPrivacyApplication> getComparator() {
		switch (comparator) {
		case 0:
			return compareLabel;
		case 1:
			return compareLastAccess;
		case 2:
			return comparePrecision;
		case 3:
			return compare28Days;
		}
		return null;

	}

	private void refresh() {
		apps = lpManager.getApplications();
		for (LocationPrivacyApplication app : apps) {
			Preference pref = screen.findPreference("app_"
					+ app.getPackagename());
			if (pref != null) {
				screen.removePreference(pref);
			}
		}
		Collections.sort(apps, getComparator());
		for (LocationPrivacyApplication app : apps) {
			LocationPrivacyAppPreference pref = new LocationPrivacyAppPreference(
					getActivity(), app, !lpManager.isUseOnlineAlgorithm());
			pref.setKey("app_" + app.getPackagename());
			pref.setOnPreferenceClickListener(this);
			screen.addPreference(pref);
		}

	}

}
