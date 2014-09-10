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

package android.locationprivacy.model;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * LocationPrivacyApplication models an app in the location privacy framework
 * 
 * @author Christian Kater
 * 
 */
public class LocationPrivacyApplication implements Parcelable {

	/** packagename of the app */
	private String packagename;

	private int presetConfig;

	public static final Parcelable.Creator<LocationPrivacyApplication> CREATOR = new Creator<LocationPrivacyApplication>() {

		@Override
		public LocationPrivacyApplication createFromParcel(Parcel source) {
			return new LocationPrivacyApplication(source);
		}

		@Override
		public LocationPrivacyApplication[] newArray(int size) {
			return new LocationPrivacyApplication[size];
		}
	};

	/**
	 * Creates new instance of LocationPrivacyApplication
	 * 
	 * @param in
	 *            Parcel object with attributes for LocationPrivacyApplication
	 *            object
	 */
	public LocationPrivacyApplication(Parcel in) {
		packagename = in.readString();
		presetConfig = in.readInt();
	}

	/**
	 * Creates new instance of LocationPrivacyApplication
	 * 
	 * @param packageName
	 *            packagename of the app
	 * @param config
	 *            presett configuration of location obfuscation of the app
	 */
	public LocationPrivacyApplication(String packageName, int config) {
		this.packagename = packageName;
		this.presetConfig = config;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.Parcelable#describeContents()
	 */
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * application label corresponding to (package) name
	 */
	public String getLabel(PackageManager packageManager) {
		ApplicationInfo appInfo;
		try {
			appInfo = packageManager.getApplicationInfo(this.packagename, 0);
		} catch (final NameNotFoundException e) {
			appInfo = null;
		}
		String appLabel = (String) (appInfo != null ? packageManager
				.getApplicationLabel(appInfo) : this.packagename);
		return appLabel;
	}
	
	public Drawable getIcon(PackageManager packageManager) {
		try {
			return packageManager.getApplicationIcon(packagename);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public String getPackagename() {
		return packagename;
	}

	public int getPresetConfig() {
		return presetConfig;
	}

	public void setPresetConfig(int presetConfig) {
		this.presetConfig = presetConfig;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(packagename);
		dest.writeInt(presetConfig);

	}
	
	public String toString(){
		return "LocationPrivacyApplication[packagename= "+packagename + " ; Presetconfig= " + presetConfig + " ]"; 
	}

}
