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

import android.content.Context;
import android.content.pm.PackageManager;
import android.locationprivacy.model.LocationPrivacyApplication;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

public class LocationPrivacyAppPreference extends Preference implements
		OnPreferenceClickListener {
	public static final int[] presetsDrawables = { R.drawable.lp_on,
			R.drawable.lp_street, R.drawable.lp_postalcode, R.drawable.lp_city,
			R.drawable.lp_off, R.drawable.lp_unset };

	private LocationPrivacyApplication app;

	private PackageManager packageManager;

	private TextView presetConfiguration;

	private ImageView presetConfigurationImage;
	public LocationPrivacyAppPreference(Context context,
			LocationPrivacyApplication app, boolean offline) {
		super(context);
		// TODO Auto-generated constructor stub
		this.app = app;
		this.packageManager = context.getPackageManager();
		setOnPreferenceClickListener(this);
		setTitle(app.getLabel(packageManager));
		setIcon(app.getIcon(packageManager));
		setKey("app_" + app.getPackagename());

	}

	public LocationPrivacyApplication getApp() {
		return app;
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.lp_apppreference, null);
		presetConfiguration = (TextView) view.findViewById(R.id.presetConf);
		presetConfigurationImage = (ImageView) view
				.findViewById(R.id.presetConfImage);
		String[] presetconfigs = getContext().getResources().getStringArray(
				R.array.preconfigs_short);
		int preset = app.getPresetConfig();
		preset = preset == -1 ? 5 : preset;
		presetConfiguration.setText(presetconfigs[preset]);
		presetConfigurationImage.setImageResource(presetsDrawables[preset]);
		return view;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		return false;
	}

	public void setApp(LocationPrivacyApplication app) {
		this.app = app;
		setTitle(app.getLabel(packageManager));
		setIcon(app.getIcon(packageManager));
		setKey("app_" + app.getPackagename());
	}

	public void setPreset(int preset) {
		app.setPresetConfig(preset);
		String[] presetconfigs = getContext().getResources().getStringArray(
				R.array.preconfigs_short);
		if (app.getPresetConfig() >= 0) {
			presetConfiguration.setText(presetconfigs[app.getPresetConfig()]);
			presetConfigurationImage.setImageResource(presetsDrawables[app
					.getPresetConfig()]);
		}

	}

}
