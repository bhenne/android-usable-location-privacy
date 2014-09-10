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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.locationprivacy.control.LocationPrivacyManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class LocationPrivacyOfflineObfuscation extends
		SettingsPreferenceFragment implements OnPreferenceClickListener,
		OnPreferenceChangeListener {
	private static final int MAPPICKER_REQUEST = 20;

	private LocationPrivacyManager lpManager;
	private PreferenceScreen screen;

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == MAPPICKER_REQUEST && resultCode == 1) {
			int street = data.getIntExtra("street", -1);
			int city = data.getIntExtra("city", -1);
			int sublocality = data.getIntExtra("sublocality", -1);
			int minDist = data.getIntExtra("minDist", -1);
			if (street >= 0 && city >= 0 && sublocality >= 0 && minDist >= 0) {
				lpManager.setMinDistance(minDist);
				lpManager.setOfflinePresetConfiguration(1, street);
				lpManager.setOfflinePresetConfiguration(2, sublocality);
				lpManager.setOfflinePresetConfiguration(3, city);
				refresh();
			}
		}
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		lpManager = new LocationPrivacyManager(getActivity());
		screen = getPreferenceScreen();
		if (screen != null) {
			screen.removeAll();
		}
		addPreferencesFromResource(R.xml.locationprivacy_offline_obfuscation);
		screen = getPreferenceScreen();

		refresh();

	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals("lp_settings_advanced_setpreset_street")) {
			lpManager.setOfflinePresetConfiguration(1,
					Integer.parseInt((String) newValue));
		} else if (preference.getKey().equals(
				"lp_settings_advanced_setpreset_postalcode")) {
			lpManager.setOfflinePresetConfiguration(2,
					Integer.parseInt((String) newValue));
		} else if (preference.getKey().equals(
				"lp_settings_advanced_setpreset_city")) {
			lpManager.setOfflinePresetConfiguration(3,
					Integer.parseInt((String) newValue));
		} else if (preference.getKey().equals(
				"lp_settings_advanced_setpreset_min")) {
			lpManager.setMinDistance(Integer.parseInt((String) newValue));
		}
		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals(
				"lp_settings_advanced_setpreset_mappicker")) {
			startActivityForResult((new Intent(getActivity(),
					LocationPrivacyMap.class)), MAPPICKER_REQUEST);
		} else if (preference.getKey().equals(
				"lp_settings_advanced_use_recorded_values")) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			Resources res = getResources();
			String[] presets = res.getStringArray(R.array.preconfigs_short);
			String summary = res
					.getString(R.string.lp_settings_advanced_use_recorded_values_dialog_summary)
					+ "\n\n";

			for (int i = 1; i < 4; i++) {
				summary += presets[i] + ": "
						+ lpManager.getRecordedDeviation(i) + "\n";
			}

			builder.setMessage(summary)
					.setTitle(R.string.lp_settings_advanced_use_recorded_values)
					.setPositiveButton(R.string.ok,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									lpManager.useRecordedDeviation();
									refresh();
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

		}
		return true;
	}

	private void refresh() {
		for (int i = 0; i < screen.getPreferenceCount(); i++) {
			Preference pref = screen.getPreference(i);
			if (pref != null) {
				System.out.println(pref);
				String key = pref.getKey();
				if (key.equals("lp_settings_advanced_setpreset_street")) {
					EditTextPreference editTextPref = (EditTextPreference) pref;
					editTextPref.setText(""
							+ lpManager.getPresetConfiguration(1));
				} else if (key
						.equals("lp_settings_advanced_setpreset_postalcode")) {
					EditTextPreference editTextPref = (EditTextPreference) pref;
					editTextPref.setText(""
							+ lpManager.getPresetConfiguration(2));
				} else if (key.equals("lp_settings_advanced_setpreset_city")) {
					EditTextPreference editTextPref = (EditTextPreference) pref;
					editTextPref.setText(""
							+ lpManager.getPresetConfiguration(3));
				} else if (key.equals("lp_settings_advanced_setpreset_min")) {
					EditTextPreference editTextPref = (EditTextPreference) pref;
					editTextPref.setText("" + lpManager.getMinDistance());
				} else if (key
						.equals("lp_settings_advanced_use_recorded_values")) {
					pref.setEnabled(lpManager.getRecordedDeviation(1) > 0
							&& lpManager.getRecordedDeviation(2) > 0
							&& lpManager.getRecordedDeviation(3) > 0);
				}

				pref.setOnPreferenceChangeListener(this);
				pref.setOnPreferenceClickListener(this);
			}
		}

	}

}
