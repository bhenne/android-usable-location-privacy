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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.locationprivacy.control.LocationPrivacyManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.android.settings.R;
import com.mapquest.android.maps.AnnotationView;
import com.mapquest.android.maps.DefaultItemizedOverlay;
import com.mapquest.android.maps.GeoPoint;
import com.mapquest.android.maps.ItemizedOverlay;
import com.mapquest.android.maps.MapActivity;
import com.mapquest.android.maps.MapView;
import com.mapquest.android.maps.Overlay;
import com.mapquest.android.maps.OverlayItem;

/**
 * LocationPrivacyMap provides a map to select coordinates
 * 
 * @author Christian Kater
 * 
 */
public class LocationPrivacyMap extends MapActivity implements OnClickListener {

	/**
	 * @author Nicolas Klein
	 *         (http://android.foxykeep.com/dev/how-to-add-autocompletion
	 *         -to-an-edittext)
	 */
	private class AutoCompleteAdapter extends ArrayAdapter<Address> implements
			Filterable {

		private Geocoder geocoder;
		private LayoutInflater inflater;

		public AutoCompleteAdapter(Context context) {
			super(context, -1);
			inflater = LayoutInflater.from(context);
			geocoder = new Geocoder(context);
		}

		/**
		 * @author Christian Kater format postal address to typical German
		 *         formatting.
		 * @param address
		 *            address to be formated
		 * @return address as typically used in Germany
		 */
		public String adressToString(Address address) {
			String postal = address.getPostalCode();
			String street = address.getThoroughfare();
			String streetNumber = address.getSubThoroughfare();
			String city = address.getLocality();
			String country = address.getCountryName();
			String addressString = "";
			if (street != null) {
				addressString += street;
				if (streetNumber != null) {
					addressString += " " + streetNumber + ", ";
				} else {
					addressString += ", ";
				}
			}
			if (city != null) {
				if (postal != null) {
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

		@Override
		public Filter getFilter() {
			Filter filter = new Filter() {

				public CharSequence convertResultToString(
						final Object resultValue) {
					return resultValue == null ? ""
							: adressToString(((Address) resultValue));
				}

				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					List<Address> addresses = null;
					if (constraint != null) {
						try {
							addresses = geocoder.getFromLocationName(
									(String) constraint, 10);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (addresses == null) {
						addresses = new ArrayList<Address>();
					}
					FilterResults results = new FilterResults();
					results.values = addresses;
					results.count = addresses.size();
					return results;
				}

				@Override
				protected void publishResults(CharSequence constraint,
						FilterResults results) {
					clear();
					List<Address> addresses = (List<Address>) results.values;
					for (Address address : addresses) {
						add(address);
					}
					if (addresses.size() > 0) {
						notifyDataSetChanged();
					} else {
						notifyDataSetInvalidated();
					}
				}

			};
			return filter;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView row = null;
			row = (TextView) inflater.inflate(
					android.R.layout.simple_dropdown_item_1line, parent, false);
			row.setText(adressToString(getItem(position)));
			return row;
		}

	}

	/**
	 * TapOverlay acts on tapping on the map
	 * 
	 * @author Christian Kater
	 * 
	 */
	private class TapOverlay extends Overlay {

		/**
		 * Update selected coordinate
		 */
		@Override
		public boolean onTap(GeoPoint geo, MapView mapView) {
			LocationPrivacyMap.this.ok.setEnabled(true);
			LocationPrivacyMap.this.setPOI(geo);
			return super.onTap(geo, mapView);
		}

	}
	private static final String TAG = "LocationPrivacyMap";
	/** marker showing select coordinates */
	private AnnotationView annotation;
	private Button cancel;
	private double EARTH_RADIUS = 6371000;
	private Geocoder geocoder;
	private GeoPoint geopoint;
	private TextView infotext;
	private LocationPrivacyManager lpManager;
	private MapView map;
	private double METER_PER_LATITUDE = 111320;
	private NumberPicker minDistancePicker;
	private Button ok;
	private DefaultItemizedOverlay poiOverlay;
	private Button retry;
	private Random rnd = new Random();
	private AutoCompleteTextView searchAddress;
	private boolean showPoiInfo = false;
	private int state = -1;
	private ImageButton submitSearch;
	private double TO_RADIAN = Math.PI / 180;

	private HashMap<String, Location> userLocations;

	private HashMap<String, Integer> values;

	private Location getLocForArea(Location currentPosition, boolean street) {
		Address randomPosition = null;
		int timer = 0;
		
		while (randomPosition == null && timer < 10) {
			float results[] = new float[3];
		
			Location.distanceBetween(currentPosition.getLatitude(), currentPosition.getLongitude(), geopoint.getLatitude(), geopoint.getLongitude(), results);
			double alpha = results[1] * TO_RADIAN;
			double r = results[0] * (0.2 + (rnd.nextDouble() / 10 - 0.05));
						
			double meterPerLong = Math.abs((2 * Math.PI
					* Math.cos(currentPosition.getLatitude() * TO_RADIAN)
					* EARTH_RADIUS / 360));
			double moveLong = r * Math.sin(alpha) / meterPerLong;
			double moveLat = r * Math.cos(alpha) / METER_PER_LATITUDE;
			
			List<Address> adresses;
			try {
				adresses = geocoder.getFromLocation(
						currentPosition.getLatitude() + moveLat,
						currentPosition.getLongitude() + moveLong, 5);
			} catch (IOException e) {
				Log.d(TAG, "Geoservice not aviable");
				return null;
			}
			for (Address address : adresses) {
				if (address.getSubLocality() != null) {
					if (street) {
						if (address.getThoroughfare() != null) {
							randomPosition = address;
							break;
						}
					} else {
						randomPosition = address;
						System.out.println(randomPosition.getSubLocality());
						break;
					}
				}
			}
			
			timer++;
		}
		if (timer == 10) {
			return null;
		}
		Address newPosition = null;
		String newAdd = "";
		if (street) {
			newAdd += randomPosition.getThoroughfare();
		}
		newAdd += " " + randomPosition.getSubLocality() + " "
				+ randomPosition.getLocality() + " "
				+ randomPosition.getCountryName();
		try {
			List<Address> newAdresses = geocoder.getFromLocationName(newAdd, 1);
			if (newAdresses != null && !newAdresses.isEmpty()) {
				newPosition = newAdresses.get(0);
			}
		} catch (IOException e) {
			Log.d(TAG, "Geoservice not aviable");
			return null;
		}
		if (newPosition == null) {
			return null;
		}
		Location loc = new Location("");
		loc.setLatitude(newPosition.getLatitude());
		loc.setLongitude(newPosition.getLongitude());
		return loc;

	}

	@Override
	public boolean isRouteDisplayed() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		if (v == ok) {
			state++;
			if (state == 0) {
				values.put("minDist", Integer.valueOf(minDistancePicker
						.getDisplayedValues()[minDistancePicker.getValue()]));
			}
			if (state == 2) {
				setDistance("city");
				userLocations.put("sublocality",
						getLocForArea(userLocations.get("city"), false));

			} else if (state == 3) {
				setDistance("sublocality");
				userLocations.put("street",
						getLocForArea(userLocations.get("sublocality"), true));

			} else if (state == 4) {
				setDistance("street");

			} else if (state > 4) {
				Intent data = new Intent();
				data.putExtra("street", values.get("street"));
				data.putExtra("sublocality", values.get("sublocality"));
				data.putExtra("city", values.get("city"));
				data.putExtra("minDist", values.get("minDist"));
				setResult(1, data);
				finish();
			}
			setViewElements();

		} else if (v == cancel) {
			state--;
			setViewElements();
			if (state < -1) {
				setResult(0);
				finish();
			}
		} else if (v == submitSearch) {

			String address = searchAddress.getText().toString();
			List<Address> positions = null;
			try {
				positions = geocoder.getFromLocationName(address, 1);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (positions != null && positions.size() > 0) {
				Address newPosition = positions.get(0);
				GeoPoint geo = new GeoPoint(newPosition.getLatitude(),
						newPosition.getLongitude());

				Location city = new Location("");
				city.setLongitude(geo.getLongitude());
				city.setLatitude(geo.getLatitude());
				userLocations.put("city", city);
				map.getController().setCenter(geo);
				setPOI(geo);
				InputMethodManager inputManager = (InputMethodManager) this
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow(this.getCurrentFocus()
						.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				state = 1;
				setViewElements();
			} else {
				infotext.setText(R.string.lp_mappicker_badinput);
			}
		} else if (v == retry) {
			state = 1;
			setViewElements();

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.locationprivacy_coordinate);
		lpManager = new LocationPrivacyManager(this);

		int minDistance = lpManager.getMinDistance();
		int[] nums = { 0, 20, 50, 100, 200, 300, 400, 500, 1000, 2000, 3000,
				4000, 5000 };

		int i = 0;
		for (; i < nums.length; i++) {
			if (minDistance <= nums[i]) {
				break;
			}
		}
		if (i == nums.length) {
			i--;
		}

		String[] numsStr = new String[nums.length];
		for (int j = 0; j < numsStr.length; j++) {
			numsStr[j] = "" + nums[j];
		}

		minDistancePicker = (NumberPicker) findViewById(R.id.np);
		minDistancePicker.setMaxValue(nums.length - 1);
		minDistancePicker.setMinValue(0);
		minDistancePicker.setValue(i);
		minDistancePicker.setWrapSelectorWheel(false);
		minDistancePicker.setDisplayedValues(numsStr);

		searchAddress = (AutoCompleteTextView) findViewById(R.id.lp_mappicker_searchaddress);
		map = (MapView) findViewById(R.id.lp_mappicker_map);
		ok = (Button) findViewById(R.id.lp_mappicker_ok);
		cancel = (Button) findViewById(R.id.lp_mappicker_cancel);
		retry = (Button) findViewById(R.id.lp_mapppicker_retry);
		submitSearch = (ImageButton) findViewById(R.id.lp_mappicker_submitsearch);
		infotext = (TextView) findViewById(R.id.lp_mappicker_infotext);
		Drawable iconPosition = getResources().getDrawable(
				android.R.drawable.ic_menu_search);
		Drawable icon = getResources().getDrawable(R.drawable.point);

		submitSearch.setImageDrawable(iconPosition);

		ok.setOnClickListener(this);
		cancel.setOnClickListener(this);
		submitSearch.setOnClickListener(this);
		retry.setOnClickListener(this);
		searchAddress.setAdapter(new AutoCompleteAdapter(this));

		poiOverlay = new DefaultItemizedOverlay(icon);
		map.getOverlays().add(new TapOverlay());
		map.getOverlays().add(poiOverlay);

		annotation = new AnnotationView(map);
		annotation.getTitle().setMaxLines(2);
		annotation.getTitle().setTextSize(19);
		annotation.getSnippet().setTextSize(19);

		geocoder = new Geocoder(this, Locale.getDefault());
		values = new HashMap<String, Integer>();
		userLocations = new HashMap<String, Location>();

	}

	private void setDistance(String name) {
		Location choosen = new Location("");
		choosen.setLatitude(geopoint.getLatitude());
		choosen.setLongitude(geopoint.getLongitude());
		values.put(name, (int) userLocations.get(name).distanceTo(choosen));
	}

	/**
	 * Set new coordinates. Address information is gathered and shown on the
	 * map.
	 * 
	 * @param geo
	 *            new coordinates
	 */
	public void setPOI(GeoPoint geo) {
		geopoint = geo;
		poiOverlay.clear();
		List<Address> addresses = null;
		try {
			addresses = geocoder.getFromLocation(geo.getLatitude(),
					geo.getLongitude(), 1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Address nextAddress = null;
		String street = "";
		String city = "";
		if (addresses != null && addresses.size() > 0) {
			nextAddress = addresses.get(0);
			if (nextAddress.getThoroughfare() != null) {
				street = nextAddress.getThoroughfare();
				if (street.length() > 18) {
					street = street.substring(0, 18);
				}
				if (nextAddress.getSubThoroughfare() != null) {
					street += " " + nextAddress.getSubThoroughfare();
				}
			}
			if (nextAddress.getPostalCode() != null) {
				city = nextAddress.getPostalCode();
			}
			if (nextAddress.getLocality() != null) {
				if (city.length() > 0) {
					city += " " + nextAddress.getLocality();
				} else {
					city += nextAddress.getLocality();
				}
			}
			if (nextAddress.getCountryName() != null) {
				if (city.length() > 0) {
					city += ", " + nextAddress.getCountryName();
				} else {
					city += nextAddress.getCountryName();
				}
			}

		}

		OverlayItem poi = new OverlayItem(geo, street, city);

		poiOverlay.addItem(poi);
		poiOverlay.setTapListener(new ItemizedOverlay.OverlayTapListener() {
			@Override
			public void onTap(GeoPoint pt, MapView mapView) {
				int lastTouchedIndex = poiOverlay.getLastFocusedIndex();
				if (lastTouchedIndex > -1) {
					showPoiInfo = !showPoiInfo;
					if (showPoiInfo) {
						OverlayItem tapped = poiOverlay
								.getItem(lastTouchedIndex);
						annotation.showAnnotationView(tapped);
					} else {
						annotation.hide();
					}

				}
			}
		});

	}

	private void setViewElements() {
		submitSearch.setVisibility(View.GONE);
		searchAddress.setVisibility(View.GONE);
		minDistancePicker.setVisibility(View.GONE);
		map.setVisibility(View.VISIBLE);

		ok.setText(R.string.lp_mappicker_next);
		ok.setEnabled(true);
		cancel.setText(R.string.lp_mappicker_back);
		cancel.setEnabled(true);
		retry.setVisibility(View.GONE);

		try {
			switch (state) {
			case -1:
				infotext.setText(R.string.lp_mappicker_infotext_mindistance);
				minDistancePicker.setVisibility(View.VISIBLE);
				cancel.setText(android.R.string.cancel);
				map.setVisibility(View.GONE);
				break;
			case 0:
				ok.setText(R.string.lp_mappicker_next);
				ok.setEnabled(false);
				submitSearch.setVisibility(View.VISIBLE);
				searchAddress.setVisibility(View.VISIBLE);
				infotext.setText(R.string.lp_mappicker_infotext0);
				annotation.hide();
				map.setVisibility(View.GONE);
				break;
			case 1:
				ok.setText(R.string.lp_mappicker_next);
				infotext.setText(R.string.lp_mappicker_infotext1);
				ok.setEnabled(false);
				Location city = userLocations.get("city");
				GeoPoint cityGeo = new GeoPoint(city.getLatitude(),
						city.getLongitude());
				annotation.hide();
				map.getController().setZoom(10);
				map.getController().setCenter(cityGeo);
				setPOI(cityGeo);
				break;
			case 2:
				infotext.setText(R.string.lp_mappicker_infotext2);
				annotation.hide();
				ok.setEnabled(false);
				Location sublocality = userLocations.get("sublocality");
				GeoPoint sublocalityGeo = new GeoPoint(
						sublocality.getLatitude(), sublocality.getLongitude());
				map.getController().setZoom(14);
				map.getController().setCenter(sublocalityGeo);
				setPOI(sublocalityGeo);
				break;
			case 3:
				map.setVisibility(View.VISIBLE);
				infotext.setText(R.string.lp_mappicker_infotext3);
				Location street = userLocations.get("street");
				GeoPoint streetGeo = new GeoPoint(street.getLatitude(),
						street.getLongitude());
				annotation.hide();
				ok.setEnabled(false);
				map.getController().setZoom(18);
				map.getController().setCenter(streetGeo);
				setPOI(streetGeo);
				break;
			case 4:
				submitSearch.setVisibility(View.GONE);
				searchAddress.setVisibility(View.GONE);
				ok.setText(android.R.string.ok);
				map.setVisibility(View.GONE);
				Resources r = getResources();
				String intro = r.getString(R.string.lp_mappicker_result)
						+ ":\n\n";
				String minDistanceLbl = r.getString(R.string.lp_offline_min)
						+ ": ";
				String streetLbl = r.getString(R.string.lp_street_offline)
						+ ": ";
				String sublocLbl = r.getString(R.string.lp_postalcode_offline)
						+ ": ";
				String cityLbl = r.getString(R.string.lp_city_offline) + ": ";
				infotext.setText(intro + minDistanceLbl + values.get("minDist")
						+ "\n" + streetLbl + values.get("street") + "m\n"
						+ sublocLbl + values.get("sublocality") + "m\n"
						+ cityLbl + values.get("city") + "m");
				break;
			default:
				break;
			}
		} catch (NullPointerException e) {
			submitSearch.setVisibility(View.GONE);
			searchAddress.setVisibility(View.GONE);
			minDistancePicker.setVisibility(View.GONE);
			map.setVisibility(View.GONE);
			retry.setVisibility(View.VISIBLE);
			ok.setText(R.string.lp_mappicker_next);
			ok.setEnabled(false);
			cancel.setText(android.R.string.cancel);
			cancel.setEnabled(true);
			state = -2;
			infotext.setText(R.string.lp_mappicker_error);
		}

	}

}
