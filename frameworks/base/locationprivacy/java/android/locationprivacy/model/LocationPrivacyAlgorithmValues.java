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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * The LocationPrivacyAlgorithmValues consists of HashMaps with String keys and
 * different datatype values. The HashMaps store different configuration
 * parameters. These are used by an AbstractLocationPrivacyAlgorithm.
 * 
 * @author Christian Kater
 * 
 */
public class LocationPrivacyAlgorithmValues implements Parcelable {

	/** Integer parameters */
	private Map<String, Integer> intValues;
	/** Double parameters */
	private Map<String, Double> doubleValues;
	/** String parameters */
	private Map<String, String> stringValues;
	/**
	 * Enum parameters: For each key a list of Strings is defined that users can select.
	 */
	private Map<String, ArrayList<String>> enumValues;
	/** User-selected Strings from enumValues */
	private Map<String, String> enumChoosen;
	/** Coordinate parameters */
	private Map<String, Coordinate> coordinateValues;
	/** Boolean parameters */
	private Map<String, Boolean> booleanValues;

	public static final Parcelable.Creator<LocationPrivacyAlgorithmValues> CREATOR = new Parcelable.Creator<LocationPrivacyAlgorithmValues>() {

		@Override
		public LocationPrivacyAlgorithmValues createFromParcel(Parcel source) {
			return new LocationPrivacyAlgorithmValues(source);
		}

		@Override
		public LocationPrivacyAlgorithmValues[] newArray(int size) {
			// Auto-generated method stub
			return new LocationPrivacyAlgorithmValues[size];
		}
	};

	/**
	 * Creates new instance of LocationPrivacyConfiguration
	 * 
	 * @param intValues Integer parameters
	 * @param doubleValues Double parameters
	 * @param stringValues String parameters
	 * @param enumValues Enum parameters
	 * @param enumChoosen Selected Enum values
	 * @param coordinateValues Coordinates
	 * @param booleanValues Boolean parameters
	 */
	public LocationPrivacyAlgorithmValues(Map<String, Integer> intValues,
			Map<String, Double> doubleValues, Map<String, String> stringValues,
			Map<String, ArrayList<String>> enumValues,
			Map<String, String> enumChoosen,
			Map<String, Coordinate> coordinateValues,
			Map<String, Boolean> booleanValues) {
		super();
		this.intValues = intValues;
		this.doubleValues = doubleValues;
		this.stringValues = stringValues;
		this.enumValues = enumValues;
		this.enumChoosen = enumChoosen;
		this.coordinateValues = coordinateValues;
		this.booleanValues = booleanValues;
	}

	/**
	 * Creates new instance of LocationPrivacyConfiguration
     *
	 * @param in Parcel object containing HashMaps
	 */
	public LocationPrivacyAlgorithmValues(Parcel in) {
		readFromParcel(in);
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocationPrivacyAlgorithmValues other = (LocationPrivacyAlgorithmValues) obj;
		if (booleanValues == null) {
			if (other.booleanValues != null)
				return false;
		} else if (!booleanValues.equals(other.booleanValues))
			return false;
		if (coordinateValues == null) {
			if (other.coordinateValues != null)
				return false;
		} else if (!coordinateValues.equals(other.coordinateValues))
			return false;
		if (doubleValues == null) {
			if (other.doubleValues != null)
				return false;
		} else if (!doubleValues.equals(other.doubleValues))
			return false;
		if (enumChoosen == null) {
			if (other.enumChoosen != null)
				return false;
		} else if (!enumChoosen.equals(other.enumChoosen))
			return false;
		if (enumValues == null) {
			if (other.enumValues != null)
				return false;
		} else if (!enumValues.equals(other.enumValues))
			return false;
		if (intValues == null) {
			if (other.intValues != null)
				return false;
		} else if (!intValues.equals(other.intValues))
			return false;
		if (stringValues == null) {
			if (other.stringValues != null)
				return false;
		} else if (!stringValues.equals(other.stringValues))
			return false;
		return true;
	}

	public boolean getBoolean(String key) {
		return booleanValues.get(key);
	}

	public Map<String, Boolean> getBooleanValues() {
		return booleanValues;
	}

	public Coordinate getCoordinate(String key) {
		return coordinateValues.get(key);
	}

	public Map<String, Coordinate> getCoordinateValues() {
		return coordinateValues;
	}

	public double getDouble(String key) {
		return doubleValues.get(key);
	}

	public Map<String, Double> getDoubleValues() {
		return doubleValues;
	}

	public ArrayList<String> getEnum(String key) {
		return enumValues.get(key);
	}

	public Map<String, String> getEnumChoosen() {
		return enumChoosen;
	}

	public String getEnumChoosen(String key) {
		return enumChoosen.get(key);
	}

	public Map<String, ArrayList<String>> getEnumValues() {
		return enumValues;
	}

	public int getInt(String key) {
		return intValues.get(key);
	}

	public Map<String, Integer> getIntValues() {
		return intValues;
	}

	public String getString(String key) {
		return stringValues.get(key);
	}

	public Map<String, String> getStringValues() {
		return stringValues;
	}

	private void readFromParcel(Parcel in) {
		intValues = new HashMap<String, Integer>();
		int intValuesSize = in.readInt();
		for (int i = 0; i < intValuesSize; i++) {
			String key = in.readString();
			int value = in.readInt();
			intValues.put(key, value);
		}
		doubleValues = new HashMap<String, Double>();
		int doubleValueSize = in.readInt();
		for (int i = 0; i < doubleValueSize; i++) {
			String key = in.readString();
			double value = in.readDouble();
			doubleValues.put(key, value);
		}
		stringValues = new HashMap<String, String>();
		int stringValueSize = in.readInt();
		for (int i = 0; i < stringValueSize; i++) {
			String key = in.readString();
			String value = in.readString();
			stringValues.put(key, value);
		}
		enumValues = new HashMap<String, ArrayList<String>>();
		int enumValuesSize = in.readInt();
		for (int i = 0; i < enumValuesSize; i++) {
			ArrayList<String> enumList = new ArrayList<String>();
			String key = in.readString();
			int enumListSize = in.readInt();
			for (int j = 0; j < enumListSize; j++) {
				String value = in.readString();
				enumList.add(value);
			}
			enumValues.put(key, enumList);
		}
		enumChoosen = new HashMap<String, String>();
		int enumChoosenSize = in.readInt();
		for (int i = 0; i < enumChoosenSize; i++) {
			String key = in.readString();
			String value = in.readString();
			enumChoosen.put(key, value);
		}
		coordinateValues = new HashMap<String, Coordinate>();
		int coordinateValuesSize = in.readInt();
		for (int i = 0; i < coordinateValuesSize; i++) {
			String key = in.readString();
			coordinateValues.put(key, new Coordinate(in));
		}
		booleanValues = new HashMap<String, Boolean>();
		int booleanValuesSize = in.readInt();
		for (int i = 0; i < booleanValuesSize; i++) {
			String key = in.readString();
			boolean value = Boolean.parseBoolean(in.readString());
			booleanValues.put(key, value);
		}
	}

	public void setBoolean(String key, boolean value) {
		booleanValues.put(key, value);
	}

	public void setBooleanValues(Map<String, Boolean> booleanValues) {
		this.booleanValues = booleanValues;
	}

	public void setCoordinate(String key, Coordinate value) {
		coordinateValues.put(key, value);
	}

	public void setCoordinateValues(Map<String, Coordinate> coordinateValues) {
		this.coordinateValues = coordinateValues;
	}

	public void setDouble(String key, double value) {
		doubleValues.put(key, value);
	}

	public void setEnum(String key, ArrayList<String> value) {
		enumValues.put(key, value);
	}

	public void setEnumChoosen(String key, String value) {
		enumChoosen.put(key, value);
	}

	public void setInt(String key, int value) {
		intValues.put(key, value);
	}

	public void setString(String key, String value) {
		stringValues.put(key, value);
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(intValues.size());
		for (String key : intValues.keySet()) {
			dest.writeString(key);
			dest.writeInt(intValues.get(key));
		}
		dest.writeInt(doubleValues.size());
		for (String key : doubleValues.keySet()) {
			dest.writeString(key);
			dest.writeDouble(doubleValues.get(key));
		}
		dest.writeInt(stringValues.size());
		for (String key : stringValues.keySet()) {
			dest.writeString(key);
			dest.writeString(stringValues.get(key));
		}
		dest.writeInt(enumValues.size());
		for (String key : enumValues.keySet()) {
			ArrayList<String> enumList = enumValues.get(key);
			int length = enumList.size();
			dest.writeString(key);
			dest.writeInt(length);
			for (int i = 0; i < length; i++) {
				dest.writeString(enumList.get(i));
			}
		}
		dest.writeInt(enumChoosen.size());
		for (String key : enumChoosen.keySet()) {
			dest.writeString(key);
			dest.writeString(enumChoosen.get(key));
		}
		dest.writeInt(coordinateValues.size());
		for (String key : coordinateValues.keySet()) {
			dest.writeString(key);
			coordinateValues.get(key).writeToParcel(dest, flags);
		}
		dest.writeInt(booleanValues.size());
		for (String key : booleanValues.keySet()) {
			dest.writeString(key);
			dest.writeString("" + booleanValues.get(key));
		}
	}

	@Override
	public int describeContents() {
		// Auto-generated method stub
		return 0;
	}
}
