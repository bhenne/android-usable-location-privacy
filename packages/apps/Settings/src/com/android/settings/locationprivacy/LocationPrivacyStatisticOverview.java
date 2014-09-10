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

import android.content.pm.PackageManager;
import android.locationprivacy.control.LocationPrivacyManager;
import android.locationprivacy.model.LocationPrivacyApplication;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class LocationPrivacyStatisticOverview extends
		SettingsPreferenceFragment implements OnPreferenceClickListener {

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
	private PreferenceScreen root;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setHasOptionsMenu(true);
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
		pm = getActivity().getPackageManager();
		lpManager = new LocationPrivacyManager(getActivity());
		apps = lpManager.getApplications();

		addPreferencesFromResource(R.xml.locationprivacy_statistic_overview);
		root = getPreferenceScreen();
		root.setOrderingAsAdded(true);
		comparator = 1;
		refresh();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.location_privacy_statistic_menu, menu);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (root != null) {
			root.removeAll();
		}
		root = null;
		lpManager = null;
		apps = null;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.lp_menu_label_stat:
			comparator = 0;
			refresh();
			break;
		case R.id.lp_menu_last_access_stat:
			comparator = 1;
			refresh();
			break;
		case R.id.lp_menu_number_access_28_days_stat:
			comparator = 3;
			refresh();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		Bundle bundle = new Bundle();
		String key = preference.getKey();
		key = key.substring(9);
		bundle.putString("packagename", key);
		startFragment(getParentFragment(),
				LocationPrivacyStatistic.class.getName(), 0, bundle);
		return false;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (root != null) {
			refresh();
		}
	}

	private void refresh() {
		apps = lpManager.getApplications();
		if (root != null) {
			root.removeAll();

			Collections.sort(apps, getComparator());
			System.out.println("Apps: " + apps);
			root.setOrderingAsAdded(true);
			for (LocationPrivacyApplication app : apps) {
				PreferenceScreen statisticScreen = getPreferenceManager()
						.createPreferenceScreen(getActivity());
				statisticScreen.setTitle(app.getLabel(getPackageManager()));
				String packagename = app.getPackagename();

				statisticScreen.setKey("app_stat_" + packagename);

				Calendar time = lpManager.getLastAccess(packagename);
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(time.getTimeInMillis());
				System.out.println("Cal : " + cal);
				statisticScreen.setSummary(getResources().getString(
						R.string.lp_settings_statistic_lastaccess)
						+ " "
						+ DateFormat.getDateFormat(getActivity()).format(
								cal.getTime())
						+ " "
						+ DateFormat.getTimeFormat(getActivity()).format(
								cal.getTime()));

				statisticScreen.setIcon(app.getIcon(getPackageManager()));
				statisticScreen.setOnPreferenceClickListener(this);

				root.addPreference(statisticScreen);

			}

		}
	}

	private Comparator<LocationPrivacyApplication> getComparator() {
		switch (comparator) {
		case 0:
			return compareLabel;
		case 1:
			return compareLastAccess;
		case 3:
			return compare28Days;
		}
		return null;

	}

}
