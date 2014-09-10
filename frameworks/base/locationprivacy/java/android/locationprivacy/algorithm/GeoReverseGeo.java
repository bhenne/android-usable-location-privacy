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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.locationprivacy.model.AbstractLocationPrivacyAlgorithm;
import android.locationprivacy.model.Coordinate;
import android.locationprivacy.model.LocationPrivacyAlgorithmValues;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.Pair;

/**
 * 
 * The algorithm GeoReverseGeo first makes reverse geo-coding of the real
 * location and maps it to a postal address. The address detail is reduced as
 * configured by the user (for instance to current city, or just removing street
 * number). The broad address then is geo-coded again to transform it back to
 * coordinates. The algorithm maps a real location to the center of the bounding
 * box of a geo object, such as the current street, postal code region, or city.
 * 
 * @author Christian Kater
 * @author Benjamin Henne
 * 
 */
public class GeoReverseGeo extends AbstractLocationPrivacyAlgorithm {

	/** The Constant NAME. */
	private static final String NAME = "georeversegeo";

	private static final int DECIMALBORDER = 0;

	private List<Pair<Location, Location>> cachedLocationsStreet;
	private List<Pair<Location, Location>> cachedLocationsPostalcode;
	private List<Pair<Location, Location>> cachedLocationsCity;

	/**
	 * Creates new instance of GeoReverseGeo
	 * 
	 */
	public GeoReverseGeo() {
		super(NAME);
	}

	/**
	 * Creates new instance of GeoReverseGeo
	 * 
	 * @param in
	 *            Parcel object containing the configuration of the algorithm
	 */
	public GeoReverseGeo(Parcel in) {
		super(in, NAME);
	}

	private void addCachedLocation(Location original, Location obfuscated) {
		List<Pair<Location, Location>> cachedLocations = getChachedLocationList();
		Pair<Location, Location> newEntry = new Pair<Location, Location>(
				original, obfuscated);
		cachedLocations.add(newEntry);
	}

	/**
	 * Formate address.
	 * 
	 * @param address
	 *            the address
	 * @param detail
	 *            the detail
	 * @return the string
	 */
	private String formateAddress(Address address, String detail) {
		String postal = address.getSubLocality();
		String street = address.getThoroughfare();
		String city = address.getLocality();
		String country = address.getCountryName();
		String addressString = "";

		if (street != null && detail.equals("street")) {
			addressString += street;
			addressString += ", ";
		}
		if (city != null
				&& (detail.equals("street") || detail.equals("postalcode") || detail
						.equals("city"))) {
			if (postal != null
					&& (detail.equals("street") || detail.equals("postalcode"))) {
				addressString += postal + " ";
			}
			addressString += city + ", ";
		}
		if (country != null) {
			addressString += country;
		} else if (addressString.endsWith(", ")) {
			addressString.substring(0, addressString.length() - 2);
		}
		return addressString;
	}

	private List<Pair<Location, Location>> getChachedLocationList() {
		String detail = configuration.getEnumChoosen("detail");
		if (detail.equals("street")) {
			return cachedLocationsStreet;
		} else if (detail.equals("postalcode")) {
			return cachedLocationsPostalcode;
		} else {
			return cachedLocationsCity;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.locationprivacy.model.AbstractLocationPrivacyAlgorithm#
	 * getDefaultConfiguration()
	 */
	@Override
	public LocationPrivacyAlgorithmValues getDefaultConfiguration() {
		HashMap<String, ArrayList<String>> enumValues = new HashMap<String, ArrayList<String>>();
		ArrayList<String> detail = new ArrayList<String>();
		detail.add("street");
		detail.add("postalcode");
		detail.add("city");
		detail.add("country");
		enumValues.put("detail", detail);
		HashMap<String, String> enumChoosen = new HashMap<String, String>();
		enumChoosen.put("detail", "city");
		return new LocationPrivacyAlgorithmValues(
				new HashMap<String, Integer>(), new HashMap<String, Double>(),
				new HashMap<String, String>(), enumValues, enumChoosen,
				new HashMap<String, Coordinate>(),
				new HashMap<String, Boolean>());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.locationprivacy.model.AbstractLocationPrivacyAlgorithm#
	 * instanceFromParcel(android.os.Parcel)
	 */
	@Override
	public AbstractLocationPrivacyAlgorithm instanceFromParcel(Parcel in) {
		return new GeoReverseGeo(in);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.locationprivacy.model.AbstractLocationPrivacyAlgorithm#newInstance
	 * ()
	 */
	@Override
	public AbstractLocationPrivacyAlgorithm newInstance() {
		return new GeoReverseGeo();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.locationprivacy.model.AbstractLocationPrivacyAlgorithm#
	 * obfuscate(android.location.Location)
	 */
	@Override
	public Location obfuscate(Location location) {
		Bundle extra = location.getExtras();
		if (extra != null) {
			for (String key : location.getExtras().keySet()) {
				try {
					Object obj = extra.get(key);
					if (obj != null && obj instanceof Location) {
						Location extraLoc = (Location) obj;
						extraLoc = obfuscate(extraLoc);
						extra.putParcelable(key, extraLoc);
					}
				} catch (Exception e) {
					Log.d(TAG, e.getMessage());
				}
			}
		}
		Location newLocation = searchCachedLocation(location.getLongitude(),
				location.getLatitude());
		if (newLocation != null) {
			Log.d(TAG, "Use cached location");
			return newLocation;
		}
		String detail = configuration.getEnumChoosen("detail");
		Geocoder geocoder = new Geocoder(context);
		List<Address> nextAddressList = null;
		try {
			nextAddressList = geocoder.getFromLocation(location.getLatitude(),
					location.getLongitude(), 1);
		} catch (IOException e) {
			Log.d(TAG, "Error: Could not read from Geocoder");
			Log.d(TAG, e.getMessage());
			return null;
		}
		if (nextAddressList != null && nextAddressList.size() > 0) {
			Address nextAddress = nextAddressList.get(0);
			String addressString = formateAddress(nextAddress, detail);
			List<Address> nextStreetList = null;
			try {
				nextStreetList = geocoder.getFromLocationName(addressString, 1);
			} catch (IOException e) {
				Log.d(TAG, "Error: Could not read from Geocoder");
				Log.d(TAG, e.getMessage());
				return null;
			}
			if (nextAddressList != null && nextAddressList.size() > 0) {
				Address nextModifiedAddress = nextStreetList.get(0);
				newLocation = new Location(location);
				newLocation.setLatitude(nextModifiedAddress.getLatitude());
				newLocation.setLongitude(nextModifiedAddress.getLongitude());
			}
		}
		Location cacheLocation = new Location(newLocation);
		cacheLocation.setLongitude(roundDouble(cacheLocation.getLongitude(),
				DECIMALBORDER));
		cacheLocation.setLatitude(roundDouble(cacheLocation.getLatitude(),
				DECIMALBORDER));
		addCachedLocation(cacheLocation, newLocation);
		return newLocation;
	}

	private double roundDouble(double number, int precision) {
		int roundFactor = (int) Math.pow(10, precision);
		return Math.round(number * roundFactor) / roundFactor;
	}

	private Location searchCachedLocation(double longitude, double latidude) {
		List<Pair<Location, Location>> cachedLocations = getChachedLocationList();
		for (Pair<Location, Location> element : cachedLocations) {
			Location original = element.first;
			if (roundDouble(longitude, DECIMALBORDER) == original
					.getLongitude()
					&& roundDouble(latidude, DECIMALBORDER) == original
							.getLatitude()) {
				return element.second;
			}
		}
		return null;
	}

	public void setCachedLocationsCity(
			List<Pair<Location, Location>> cachedLocationsCity) {
		this.cachedLocationsCity = cachedLocationsCity;
	}

	public void setCachedLocationsPostalcode(
			List<Pair<Location, Location>> cachedLocationsPostalcode) {
		this.cachedLocationsPostalcode = cachedLocationsPostalcode;
	}

	public void setCachedLocationsStreet(
			List<Pair<Location, Location>> cachedLocationsStreet) {
		this.cachedLocationsStreet = cachedLocationsStreet;
	}

}
