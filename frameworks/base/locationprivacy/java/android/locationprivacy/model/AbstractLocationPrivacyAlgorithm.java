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

import android.content.Context;
import android.location.Location;
import android.locationprivacy.control.LocationPrivacyManager;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Abstract class AbstractLocationPrivacyAlgorithm is template for the 
 * implementation of obfuscation algorithms.
 * 
 * @author Christian Kater
 *
 */
public abstract class AbstractLocationPrivacyAlgorithm implements Parcelable {

	/** The Constant CREATOR. */
	public static final Parcelable.Creator<AbstractLocationPrivacyAlgorithm> CREATOR = new Creator<AbstractLocationPrivacyAlgorithm>() {

		@Override
		public AbstractLocationPrivacyAlgorithm[] newArray(int size) {
			return new AbstractLocationPrivacyAlgorithm[size];
		}

		@Override
		public AbstractLocationPrivacyAlgorithm createFromParcel(Parcel source) {
			String name = source.readString();
			return LocationPrivacyManager.getAlgorithm(name)
					.instanceFromParcel(source);
		}
	};

	/** Configuration of AbstractLocationPrivacyAlgorithm */
	protected LocationPrivacyAlgorithmValues configuration;

	/** Name of Class in the location privacy framework */
	protected String name;

	/** Context the object is running in */
	protected Context context;

	/** Tag used in Logs */
	protected final String TAG;
	

	/**
	 * 
	 * Creates new instance of AbstractLocationPrivacyAlgorithm
	 * 
	 * @param name
	 *            Name of Class in the location privacy framework
	 */
	protected AbstractLocationPrivacyAlgorithm(String name) {
		this.name = name;
		this.TAG = "LP_" + name;
		setConfiguration(getDefaultConfiguration());
	}

	/**
	 * Creates new instance of AbstractLocationPrivacyAlgorithm
	 * 
	 * @param in
	 *            Parcel object containing the configuration of the algorithm
	 * @param name
	 *            Name of Class in the location privacy framework
	 */
	public AbstractLocationPrivacyAlgorithm(Parcel in, String name) {
		this.name = name;
		this.TAG = "LP_" + name;
		configuration = new LocationPrivacyAlgorithmValues(in);
	}

	public String getName() {
		return name;
	}

	public LocationPrivacyAlgorithmValues getConfiguration() {
		return configuration;
	}

	public void setConfiguration(LocationPrivacyAlgorithmValues configuration) {
		this.configuration = configuration;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractLocationPrivacyAlgorithm other = (AbstractLocationPrivacyAlgorithm) obj;
		if (configuration == null) {
			if (other.configuration != null)
				return false;
		} else if (!configuration.equals(other.configuration))
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		configuration.writeToParcel(dest, flags);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.Parcelable#describeContents()
	 */
	public int describeContents() {
		// Auto-generated method stub
		return 0;
	}

	/**
	 * Sets the context.
	 * 
	 * @param context
	 *            the new context
	 */
	public void setContext(Context context) {
		this.context = context;
	}

	/**
	 * Returns an instance of the class
	 * 
	 * @return Instance of AbstractLocationPrivacyAlgorithm
	 */
	public abstract AbstractLocationPrivacyAlgorithm newInstance();

	/**
     * Returns the default configuration of the algorithm. It is assigned to
     * the AbstractLocationPrivacyAlgorithm on instantiation. These values
     * are default values users may change.
	 * 
	 * @return default configuration
	 */
	public abstract LocationPrivacyAlgorithmValues getDefaultConfiguration();

	/**
	 * obfuscates the original location
	 * 
	 * @param location
	 *            original location
	 * @return obfuscated location
	 */
	public abstract Location obfuscate(Location location);

	/**
	 * Returns an instance of AbstractLocationPrivacyAlgorithm
	 * 
	 * @param in
	 *            Parcel object containing the configuration of the algorithm
	 * @return new instance of AbstractLocationPrivacyAlgorithm
	 */
	protected abstract AbstractLocationPrivacyAlgorithm instanceFromParcel(
			Parcel in);
}
