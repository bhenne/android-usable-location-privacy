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

package android.locationprivacy.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.location.Location;
import android.locationprivacy.model.AbstractLocationPrivacyAlgorithm;
import android.locationprivacy.model.Coordinate;
import android.locationprivacy.model.LocationPrivacyAlgorithmValues;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;

/**
 * The algorithm RadiusDistance maps a real location to a random location 
 * within a given range with minimum difference to real coordinates. 
 * A distance contraint prevents location from frequently jumping on 
 * the map: Only if the device moved x meters, a new location is generated.
 *
 * @author Christian Kater
 *
 */
public class RadiusDistance extends AbstractLocationPrivacyAlgorithm {

	/** The Constant NAME. */
	private static final String NAME = "radiusdistance";

	/** The radius2. */
	public static double radius2;

	/**
	 * Creates new instance of RadiusDistance
	 */
	public RadiusDistance() {
		super(NAME);
	}

	/**
	 * Creates new instance of RadiusDistance
	 * 
	 * @param in
	 *            Parcel object containing the configuration of the algorithm
	 */
	public RadiusDistance(Parcel in) {
		super(in, NAME);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.locationprivacy.model.AbstractLocationPrivacyAlgorithm#writeToParcel
	 * (android.os.Parcel, int)
	 */
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.locationprivacy.model.AbstractLocationPrivacyAlgorithm#newInstance
	 * ()
	 */
	public AbstractLocationPrivacyAlgorithm newInstance() {
		return new RadiusDistance();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.locationprivacy.model.AbstractLocationPrivacyAlgorithm#
	 * getDefaultConfiguration()
	 */
	public LocationPrivacyAlgorithmValues getDefaultConfiguration() {
		HashMap<String, Integer> intValues = new HashMap<String, Integer>();
		intValues.put("radius", 500);
		intValues.put("movement", 50);
		intValues.put("distance", 200);
		Map<String, Coordinate> coordinateValues = new HashMap<String, Coordinate>();
		coordinateValues.put("private_lastlocation", new Coordinate(
				Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
		return new LocationPrivacyAlgorithmValues(intValues,
				new HashMap<String, Double>(), new HashMap<String, String>(),
				new HashMap<String, ArrayList<String>>(),
				new HashMap<String, String>(), coordinateValues,
				new HashMap<String, Boolean>());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.locationprivacy.model.AbstractLocationPrivacyAlgorithm#
	 * obfuscate(android.location.Location)
	 */
	public Location obfuscate(Location location) {
		double EARTH_RADIUS = 6371000;
		double TO_RADIAN = Math.PI / 180;
		double METER_PER_LATITUDE = 111320;

		int movement = configuration.getInt("movement");
		int distance = configuration.getInt("distance");
		Location lastLocation = Coordinate.getLocation(configuration
				.getCoordinate("private_lastlocation"));
		int radius = configuration.getInt("radius");
				
		Location noGPSLocation = location.getExtraLocation("noGPSLocation");
		Location coarseLocation = null;

		if(noGPSLocation != null){
			coarseLocation = noGPSLocation.getExtraLocation("coarseLocation");
		}

		if (lastLocation.getLongitude() == 999.0
				|| location.distanceTo(lastLocation) > movement) {
			Location calcLoc = new Location(location);
			Random rnd = new Random();
			double alpha = rnd.nextDouble() * 360.0 * TO_RADIAN;
			double r = rnd.nextDouble() * Math.max(radius - distance, 0) + distance;
			double meterPerLong = Math.abs((2 * Math.PI
					* Math.cos(location.getLatitude() * TO_RADIAN)
					* EARTH_RADIUS / 360));
			double moveLong = r * Math.cos(alpha) / meterPerLong;
			double moveLat = r * Math.sin(alpha) / METER_PER_LATITUDE;
			calcLoc.setLongitude(location.getLongitude() + moveLong);
			calcLoc.setLatitude(location.getLatitude() + moveLat);
			if(noGPSLocation != null){
				noGPSLocation.setLongitude(noGPSLocation.getLongitude() + + moveLong);
				noGPSLocation.setLatitude(noGPSLocation.getLatitude() + moveLat);
			}
			if(coarseLocation != null){
				coarseLocation.setLongitude(coarseLocation.getLongitude() + moveLong);
				coarseLocation.setLatitude(coarseLocation.getLatitude() + moveLat);
			}
			
			if(noGPSLocation != null){
				noGPSLocation.setLongitude(noGPSLocation.getLongitude() + + moveLong);
				noGPSLocation.setLatitude(noGPSLocation.getLatitude() + moveLat);
			}
			if(coarseLocation != null){
				coarseLocation.setLongitude(coarseLocation.getLongitude() + moveLong);
				coarseLocation.setLatitude(coarseLocation.getLatitude() + moveLat);
			}
			
			if (calcLoc.getLatitude() > 90) {
				calcLoc.setLatitude(180 - calcLoc.getLatitude());
			} else if (calcLoc.getLatitude() < -90) {
				calcLoc.setLatitude(-180 + calcLoc.getLatitude());
			}
			if (calcLoc.getLongitude() > 180) {
				calcLoc.setLongitude(-360 + calcLoc.getLongitude());
			} else if (calcLoc.getLongitude() < -180) {
				calcLoc.setLongitude(360 + calcLoc.getLongitude());
			}
			
			if(coarseLocation != null){
				if (coarseLocation.getLatitude() > 90) {
					coarseLocation.setLatitude(180 - coarseLocation.getLatitude());
				} else if (coarseLocation.getLatitude() < -90) {
					coarseLocation.setLatitude(-180 + coarseLocation.getLatitude());
				}
				if (coarseLocation.getLongitude() > 180) {
					coarseLocation.setLongitude(-360 + coarseLocation.getLongitude());
				} else if (coarseLocation.getLongitude() < -180) {
					coarseLocation.setLongitude(360 + coarseLocation.getLongitude());
				}
			}
			
			if(noGPSLocation != null){
				if (noGPSLocation.getLatitude() > 90) {
					noGPSLocation.setLatitude(180 - noGPSLocation.getLatitude());
				} else if (noGPSLocation.getLatitude() < -90) {
					noGPSLocation.setLatitude(-180 + noGPSLocation.getLatitude());
				}
				if (noGPSLocation.getLongitude() > 180) {
					noGPSLocation.setLongitude(-360 + noGPSLocation.getLongitude());
				} else if (noGPSLocation.getLongitude() < -180) {
					noGPSLocation.setLongitude(360 + noGPSLocation.getLongitude());
				}
				noGPSLocation.setExtraLocation("coarseLocation", coarseLocation);
			}
			calcLoc.setExtraLocation("noGPSLocation", noGPSLocation);
			
			configuration.setCoordinate("private_lastlocation",
					Coordinate.getCoordinate(location));
			configuration.setCoordinate("private_lastcalculatedlocation",
					Coordinate.getCoordinate(calcLoc));
			
			return calcLoc;
		} else {
			return Coordinate.getLocation(configuration
					.getCoordinate("private_lastcalculatedlocation"));
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.locationprivacy.model.AbstractLocationPrivacyAlgorithm#
	 * instanceFromParcel(android.os.Parcel)
	 */
	public AbstractLocationPrivacyAlgorithm instanceFromParcel(Parcel in) {
		return new RadiusDistance(in);
	}

}
