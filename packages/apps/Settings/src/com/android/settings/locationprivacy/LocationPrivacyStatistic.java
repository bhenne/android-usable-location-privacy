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
import java.util.Comparator;
import java.util.HashMap;

import android.app.ActionBar;
import android.content.res.Resources;
import android.locationprivacy.control.LocationPrivacyManager;
import android.locationprivacy.model.LocationPrivacyApplication;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class LocationPrivacyStatistic extends SettingsPreferenceFragment {

	private LocationPrivacyManager lpManager;
	private String packagename;
	private PreferenceScreen screen;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		lpManager = new LocationPrivacyManager(getActivity());
		packagename = getArguments().getString("packagename");

		String appName = lpManager.getApplication(packagename).getLabel(
				getPackageManager());

		addPreferencesFromResource(R.xml.locationprivacy_statistic);
		screen = getPreferenceScreen();
		screen.setTitle(appName);
		ActionBar actionBar = getActivity().getActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(appName);
		actionBar.setDisplayHomeAsUpEnabled(true);
		refresh();
	}

	@Override
	public void onResume() {
		super.onResume();
		refresh();
	}

	private void refresh() {
		if (screen != null) {
			screen.removeAll();
			Resources res = getResources();

			HashMap<Calendar, Integer> lastAccesses = lpManager
					.getLocationAccessStatistic(packagename);
			HashMap<Calendar, Integer> last24H = lpManager
					.getLocationAccessStatistic24H(packagename);
			Preference deviation = new Preference(getActivity());
			String obfuscation = lpManager.getObfuscationDeviation(packagename) >= 0 ? ""
					+ lpManager.getObfuscationDeviation(packagename) + " m"
					: getResources().getString(
							R.string.lp_settings_statistic_na);
			deviation.setTitle(res
					.getString(R.string.lp_settings_statistic_deviation)
					+ " "
					+ obfuscation);
			screen.addPreference(deviation);

			screen.addPreference(new StatisticDiagram24HPreference(getActivity(),
					last24H));

			screen.addPreference(new StatisticDiagramPreference(getActivity(),
					lastAccesses));

		}

	}
}
