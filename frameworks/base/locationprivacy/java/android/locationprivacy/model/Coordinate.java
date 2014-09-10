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

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a geographic coordinate (latitude, longitude, altitude)
 * 
 * @author Christian Kater
 * 
 */
public class Coordinate implements Parcelable {
	/** Longitude */
	public double longitude;
	/** Latitude */
	public double latitude;

	/** Altitude */
	public double altitude;

	public static final Parcelable.Creator<Coordinate> CREATOR = new Parcelable.Creator<Coordinate>() {

		@Override
		public Coordinate createFromParcel(Parcel source) {
			return new Coordinate(source);
		}

		@Override
		public Coordinate[] newArray(int size) {
			return new Coordinate[size];
		}
	};

	/**
	 * Creates new instance of Coordinate
	 * 
	 * @param longitude
	 *            the longitude
	 * @param latitude
	 *            the latitude
	 * @param altitude
	 *            the altitude
	 */
	public Coordinate(double longitude, double latitude, double altitude) {
		super();
		this.longitude = longitude;
		this.latitude = latitude;
		this.altitude = altitude;
	}

	/**
	 * Creates new instance of Coordinate with altitude 0.
	 * 
	 * @param longitude
	 *            the longitude
	 * @param latitude
	 *            the latitude
	 */
	public Coordinate(double longitude, double latitude) {
		super();
		this.longitude = longitude;
		this.latitude = latitude;
		this.altitude = 0;
	}

	/**
	 * Creates new instance of Coordinate
	 * 
	 * @param in
	 *            Parcel object containing lat, lon, alt
	 */
	public Coordinate(Parcel in) {
		longitude = in.readDouble();
		latitude = in.readDouble();
		altitude = in.readDouble();
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getAltitude() {
		return altitude;
	}

	public void setAltitude(double altitude) {
		this.altitude = altitude;
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
		Coordinate other = (Coordinate) obj;
		if (Double.doubleToLongBits(altitude) != Double
				.doubleToLongBits(other.altitude))
			return false;
		if (Double.doubleToLongBits(latitude) != Double
				.doubleToLongBits(other.latitude))
			return false;
		if (Double.doubleToLongBits(longitude) != Double
				.doubleToLongBits(other.longitude))
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.Parcelable#describeContents()
	 */
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeDouble(longitude);
		dest.writeDouble(latitude);
		dest.writeDouble(altitude);
	}

	/**
     * Returns a Location with coordinates of the Coordinate object.
     * Other data such as velocity is read from another Location object.
	 * 
	 * @param coordinate
	 *            the Coordinate 
	 * @param location
	 *            the base Location object
	 * @return Location updates with data from Coordinate
	 */
	public static Location getLocation(Coordinate coordinate, Location location) {
		Location newLoc = new Location(location);
		newLoc.setLatitude(coordinate.getLatitude());
		newLoc.setLongitude(coordinate.getLongitude());
		newLoc.setAltitude(coordinate.getAltitude());
		return newLoc;
	}

	/**
     * Returns a Location with coordinates of the Coordinate object.
	 * 
	 * @param coordinate
	 *            the Coordinate 
	 * @return Location with data from Coordinate
	 */
	public static Location getLocation(Coordinate coordinate) {
		Location newLoc = new Location("GPS");
		newLoc.setLatitude(coordinate.getLatitude());
		newLoc.setLongitude(coordinate.getLongitude());
		newLoc.setAltitude(coordinate.getAltitude());
		return newLoc;
	}

	/**
	 * Creates a Coordinate Object from a Location object
	 * 
	 * @param location
	 *            the Location
	 * @return the Coordinate
	 */
	public static Coordinate getCoordinate(Location location) {
		Coordinate coordinate = new Coordinate(location.getLongitude(),
				location.getLatitude(), location.getAltitude());
		return coordinate;
	}

}
