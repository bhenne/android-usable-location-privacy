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

package android.locationprivacy.control;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;


import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.location.Location;
import android.locationprivacy.algorithm.GeoReverseGeo;
import android.locationprivacy.algorithm.RadiusDistance;
import android.locationprivacy.model.AbstractLocationPrivacyAlgorithm;
import android.locationprivacy.model.LocationPrivacyApplication;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;

/**
 * The LocationPrivacyManager abstracts access to the location privacy framework
 * and configuration. It provides all methods for configuration and usage.
 * 
 * @author Christian Kater
 * 
 */
public class LocationPrivacyManager {

	private class AccessUpdate implements Runnable {
		String packagename;
		int deviation;
		int config;
		boolean online;

		public AccessUpdate(String packagename, int deviation, int config, boolean online) {
			super();
			this.packagename = packagename;
			this.deviation = deviation;
			this.config = config;
			this.online = online;
		}

		public void run() {
			updateLocationAccessStatistic(packagename, deviation, config, online);
		}
	}

	public static final boolean useOnlineAlgorithmDefault = true;
	public static final boolean dialogInUseDefault = false;
	public static final boolean sharePrivacySettingsDefault = true;
	public static final boolean showCommunityAdviceDefault = true;
	public static final boolean showOnlineInfoDefault = true;
	public static final boolean webhostShareSettingsDefault = true;
	public static final boolean webhostShowCommunityAdviceDefault = true;
	public static final boolean useStarsInDialogDefault = true;
	public static final int streetDefault = 250;
	public static final int postalcodeDefault = 1000;
	public static final int cityDefault = 5000;
	public static final int minDistDefault = 200;
	public static final String DATEFORMAT = "yyyyMMddHHmmssSS";
	public static final int orderDefault = 0;

	protected static final String TAG = "LPM";

	public static final String webHostAdressDefault = "ulpa.dcsec.uni-hannover.de:8443";

	/**
	 * Cached LocationPrivacyApplications. Caching data to minimize access to
	 * database.
	 */
	private ArrayList<LocationPrivacyApplication> applications;

	private boolean useOnlineAlgorithm;

	private boolean sharePrivacySettings;

	private boolean showCommunityAdvice;

	private boolean dialogInUse;

	private String webserviceHostAdress;
	private boolean webhostShareSettings;
	private boolean webhostShowCommunityAdvice;

	private boolean useStarsInDialog;

	private boolean showOnlineInfo;
	
	private int order;

	private List<Pair<Location, Location>> cachedLocationsStreet;

	private List<Pair<Location, Location>> cachedLocationsPostalcode;

	private List<Pair<Location, Location>> cachedLocationsCity;

	private HashMap<Integer, AbstractLocationPrivacyAlgorithm> presetAlgorithms;
	/** All location obfuscation algorithms */

	private static HashMap<String, AbstractLocationPrivacyAlgorithm> algorithms;
	private static CryptoDatabase database;

	/**
	 * 
	 * Creates new Instance of obfuscation algorithm with given name
	 */

	public static AbstractLocationPrivacyAlgorithm getAlgorithm(String name) {

		return algorithms.get(name).newInstance();

	}

	/**
	 * 
	 * Returns a list of available obfuscation algorihms
	 */

	public static List<String> getAllAlgorithm() {

		ArrayList<String> sortedKeys = new ArrayList<String>(

		algorithms.keySet());

		Collections.sort(sortedKeys);

		sortedKeys.add(0, "default");

		return sortedKeys;

	}

	private ArrayList<String> configurationNeeded;

	/** Context the location privacy framework is running in */
	private Context context;

	private boolean bootComplete;

	/**
	 * Creates new instance of LocationPrivacyManager
	 * 
	 * @param oContext
	 *            Context the location privacy framework is running in
	 */
	public LocationPrivacyManager(Context oContext) {
		try {
			this.context = oContext.createPackageContext(
					"com.android.settings", Context.CONTEXT_INCLUDE_CODE);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
		cachedLocationsStreet = new LinkedList<Pair<Location, Location>>();
		cachedLocationsPostalcode = new LinkedList<Pair<Location, Location>>();
		cachedLocationsCity = new LinkedList<Pair<Location, Location>>();
		initialize();
	}

	/**
	 * Adds new application to the location privacy framework
	 */
	private LocationPrivacyApplication addApplication(String packagename) {
		ContentValues values = new ContentValues();
		values.put("packagename", packagename);
		values.put("config", -1);
		database.insert("APPLICATION", null, values);

		Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
		ContentValues lastaccess = new ContentValues();
		lastaccess.put("day", now.get(Calendar.DAY_OF_MONTH));
		lastaccess.put("month", now.get(Calendar.MONTH));
		lastaccess.put("year", now.get(Calendar.YEAR));
		lastaccess.put("hour", now.get(Calendar.HOUR_OF_DAY));
		lastaccess.put("minute", now.get(Calendar.MINUTE));
		lastaccess.put("packagename", packagename);
		database.insert("LASTACCESS", null, lastaccess);
		return new LocationPrivacyApplication(packagename, -1);
	}

	public void addPackageForWebserviceUpdate(String packagename) {
		Cursor c = database.query("WEBSERVICEDATA", null, "packagename = ?",
				new String[] { packagename }, null, null, null);
		if (c.getCount() == 0) {
			ContentValues values = new ContentValues();
			values.put("packagename", packagename);
			database.insert("WEBSERVICEDATA", null, values);
		}
	}

	public void checkForDialog() {
		while (configurationNeeded.size() > 0 && !dialogInUse && bootComplete) {
			System.out.println("ConfigSize=" + configurationNeeded.size()
					+ "; dialogInUse=" + dialogInUse + "; App="
					+ configurationNeeded.get(0));
			LocationPrivacyApplication app = getApplication(configurationNeeded
					.remove(0));
			if (app.getPresetConfig() == -1) {
				Intent i;
				try {
					setDialogInUse(true);
					i = new Intent();
					i.setComponent(new ComponentName("com.android.settings",
							"com.android.settings.locationprivacy.LocationPrivacyDialog"));
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					i.putExtra("app", app);
					context.startActivity(i);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void cleanDatabase() {
		List<String> installedApps = getInstalledApps();
		List<LocationPrivacyApplication> apps = getApplications();
		List<String> appsToRemove = new LinkedList<String>();
		for (LocationPrivacyApplication app : apps) {
			if (!installedApps.contains(app.getPackagename())) {
				appsToRemove.add(app.getPackagename());
			}
		}

		database.beginTransaction();
		try {
			for (String remove : appsToRemove) {
				database.delete("APPLICATION", "packagename = ?",
						new String[] { remove });
				database.delete("LASTACCESS", "packagename = ?",
						new String[] { remove });
				database.delete("STATISTICDEVIATION", "packagename = ?",
						new String[] { remove });
				database.delete("STATISTICACCESS", "packagename = ?",
						new String[] { remove });
				database.delete("WEBSERVICEDATA", "packagename = ?",
						new String[] { remove });
			}
			Calendar date = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
			String deleteDates = "NOT((day = ? AND month = ? AND year = ?)";
			String[] dates = new String[84];
			dates[0] = "" + date.get(Calendar.DAY_OF_MONTH);
			dates[1] = "" + date.get(Calendar.MONTH);
			dates[2] = "" + date.get(Calendar.YEAR);
			for (int i = 1; i < 28; i++) {
				date = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
				date.set(Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH)
						- i);
				deleteDates += " OR (day = ? AND month = ? AND year = ?)";
				dates[i*3] = "" + date.get(Calendar.DAY_OF_MONTH);
				dates[i*3+1] = "" + date.get(Calendar.MONTH);
				dates[i*3+2] = "" + date.get(Calendar.YEAR);
			}
			deleteDates += ")";
			
			Cursor c = database.query("STATISTICACCESS", null, null, null, null, null, null);
			c.moveToFirst();
			while(!c.isAfterLast()){
				System.out.println(c.getInt(0) + ";" + c.getInt(1) + ";" + c.getInt(2) + ";" + c.getInt(3) + ";" + c.getString(4) + ";" +  c.getInt(5));
				c.move(1);
			}
			c.close();
			
			System.out.println("Delete Dates: " + deleteDates);
			database.delete("STATISTICACCESS", deleteDates, dates);
			database.setTransactionSuccessful();

		} catch (Exception e) {
			// Error in between database transaction
		} finally {
			database.endTransaction();
		}

	}

	/**
	 * Send broadcast on data change for updating configurations
	 */
	private void dataChanged() {
		dataChanged(new Bundle());
	}

	private void dataChanged(Bundle extras) {
		Intent i = new Intent(
				"com.android.server.LocationManagerService.locationprivacy");
		i.putExtras(extras);
		context.sendBroadcast(i);
		updateData();
	}

	public void deleteAppToSend(String packagename) {
		database.delete("WEBSERVICEDATA", "packagename = ?",
				new String[] { packagename });
	}

	private String generateRandomString() {
		SecureRandom random = new SecureRandom();
		String randomString = new BigInteger(128, random).toString(32);
		return randomString;
	}

	/**
	 * Returns LocationPrivacyApplication corresponding to an app (uid)
	 * 
	 * @param uid
	 * @return LocationPrivacyApplication. null if uid not known to framework
	 */
	public LocationPrivacyApplication getApplication(String packagename) {
		Cursor cursor = database.query("APPLICATION", null, "packagename = ?",
				new String[] { packagename }, null, null, null);
		LocationPrivacyApplication app = null;
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			int config = cursor.getInt(1);
			app = new LocationPrivacyApplication(packagename, config);
		}
		cursor.close();
		return app;
	}

	/**
	 * Returns a List of all apps known by the location privacy framework
	 */
	public List<LocationPrivacyApplication> getApplications() {
		ArrayList<LocationPrivacyApplication> list = new ArrayList<LocationPrivacyApplication>();
		Cursor cursor = database.query("APPLICATION", null, null, null, null,
				null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			String packageName = cursor.getString(0);
			int config = cursor.getInt(1);
			list.add(new LocationPrivacyApplication(packageName, config));
			cursor.move(1);
		}
		cursor.close();
		return list;
	}

	public ArrayList<String> getAppsToSend() {
		Cursor appsToSendC = database.query("WEBSERVICEDATA", null, null, null,
				null, null, null);
		ArrayList<String> appsToSend = new ArrayList<String>();
		appsToSendC.moveToFirst();
		while (!appsToSendC.isAfterLast()) {
			appsToSend.add(appsToSendC.getString(0));
			appsToSendC.move(1);
		}
		appsToSendC.close();
		return appsToSend;
	}

	private String getConfiguration(String key) {
		Cursor cConfiguration = database.query("CONFIGURATION",
				new String[] { "value" }, "key = ?", new String[] { key },
				null, null, null);
		cConfiguration.moveToFirst();
		if (!cConfiguration.isAfterLast()) {
			return cConfiguration.getString(0);
		}
		return null;
	}

	private ArrayList<String> getInstalledApps() {
		ArrayList<String> res = new ArrayList<String>();
		List<PackageInfo> packs = context.getPackageManager()
				.getInstalledPackages(0);
		for (int i = 0; i < packs.size(); i++) {
			PackageInfo p = packs.get(i);
			res.add(p.packageName);
		}
		return res;
	}

	public Calendar getLastAccess(String packagename) {
		Cursor cLastAccess = database.query("LASTACCESS",
				new String[] { "day, month, year, hour, minute" },
				"packagename = ?", new String[] { packagename }, null, null,
				null);
		cLastAccess.moveToFirst();
		int day = cLastAccess.getInt(0);
		int month = cLastAccess.getInt(1);
		int year = cLastAccess.getInt(2);
		int hour = cLastAccess.getInt(3);
		int minute = cLastAccess.getInt(4);

		cLastAccess.close();
		Calendar date = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
		date.setTimeInMillis(0);
		date.set(year, month, day);
		date.set(Calendar.MINUTE, minute);
		date.set(Calendar.HOUR_OF_DAY, hour);
		return date;

	}

	public HashMap<Calendar, Integer> getLocationAccessStatistic(
			String packagename) {
		HashMap<Calendar, Integer> statistic = new HashMap<Calendar, Integer>();
		Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
		Calendar border = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
		border.set(Calendar.DAY_OF_MONTH,
				border.get(Calendar.DAY_OF_MONTH) - 28);
		border.set(Calendar.HOUR_OF_DAY, 0);
		border.set(Calendar.MINUTE, 0);
		border.set(Calendar.SECOND, 0);
		border.set(Calendar.MILLISECOND, 0);
		Cursor cStatistic = database
				.query("STATISTICACCESS",
						new String[] { "day, month, year, hour, count" },
						"packagename = ? AND (month = ? OR month = ?) AND (year = ? OR year = ?)",
						new String[] { packagename,
								"" + now.get(Calendar.MONTH),
								"" + border.get(Calendar.MONTH),
								"" + now.get(Calendar.YEAR),
								"" + border.get(Calendar.YEAR) }, null, null,
						"day asc");
		if (cStatistic.getCount() > 0) {
			cStatistic.moveToFirst();
			while (!cStatistic.isAfterLast()) {
				int day = cStatistic.getInt(0);
				int month = cStatistic.getInt(1);
				int year = cStatistic.getInt(2);
				int hour = cStatistic.getInt(3);
				int count = cStatistic.getInt(4);
				Calendar date = Calendar.getInstance(TimeZone
						.getTimeZone("GMT+0"));
				date.setTimeInMillis(0);
				date.set(year, month, day);
				date.set(Calendar.HOUR_OF_DAY, hour);
				if (date.after(border)) {
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(date.getTimeInMillis());
					cal.set(Calendar.HOUR_OF_DAY, 0);
					cal.set(Calendar.MINUTE, 0);
					cal.set(Calendar.SECOND, 0);
					cal.set(Calendar.MILLISECOND, 0);
					if (statistic.containsKey(cal)) {
						count += statistic.get(cal);
					}
					statistic.put(cal, count);
				}
				cStatistic.move(1);
			}
		}
		cStatistic.close();
		return statistic;
	}

	public HashMap<Calendar, Integer> getLocationAccessStatistic24H(
			String packagename) {
		HashMap<Calendar, Integer> statistic = new HashMap<Calendar, Integer>();
		Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
		Calendar border = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
		border.set(Calendar.DAY_OF_MONTH, border.get(Calendar.DAY_OF_MONTH) - 1);
		border.set(Calendar.MINUTE, 0);
		border.set(Calendar.SECOND, 0);
		border.set(Calendar.MILLISECOND, 0);
		Cursor cStatistic = database
				.query("STATISTICACCESS",
						new String[] { "day, month, year, hour, count" },
						"packagename = ? AND (day = ? OR day = ?) AND month = ? AND year = ?",
						new String[] { packagename,
								"" + now.get(Calendar.DAY_OF_MONTH),
								"" + border.get(Calendar.DAY_OF_MONTH),
								"" + now.get(Calendar.MONTH),
								"" + now.get(Calendar.YEAR), }, null, null,
						"day asc");
		if (cStatistic.getCount() > 0) {
			cStatistic.moveToFirst();
			while (!cStatistic.isAfterLast()) {
				int day = cStatistic.getInt(0);
				int month = cStatistic.getInt(1);
				int year = cStatistic.getInt(2);
				int hour = cStatistic.getInt(3);
				int count = cStatistic.getInt(4);
				Calendar date = Calendar.getInstance(TimeZone
						.getTimeZone("GMT+0"));
				date.set(year, month, day);
				date.set(Calendar.HOUR_OF_DAY, hour);
				date.set(Calendar.MINUTE, 0);
				date.set(Calendar.SECOND, 0);
				date.set(Calendar.MILLISECOND, 0);
				if (date.after(border)) {
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(date.getTimeInMillis());

					statistic.put(cal, count);
				}
				cStatistic.move(1);
			}
		}
		cStatistic.close();
		return statistic;
	}

	public int getMinDistance() {
		return Integer.parseInt(getConfiguration("minDist"));
	}

	public int getObfuscationDeviation(String packagename) {
		Cursor cDeviation = database.query("STATISTICDEVIATION", new String[] {
				"deviationsum", "count" }, "packagename = ?",
				new String[] { packagename }, null, null, null);
		cDeviation.moveToFirst();
		if (cDeviation.getCount() == 0) {
			return -1;
		}
		double value = cDeviation.getDouble(0);
		double count = cDeviation.getInt(1);
		cDeviation.close();
		return (int) (value / count);
	}

	public int getRecordedDeviation(int config){
		int sum = 0;
		int count = 0;
		Cursor cOffline = database.query("OFFLINEPARAMETER", new String[]{"sum", "count"},"config = ?", new String[]{"" + config}, null, null, null);
		cOffline.moveToFirst();
		if(!cOffline.isAfterLast()){
			sum = cOffline.getInt(0);
			count = cOffline.getInt(1);
		}
		if(count > 0){
			return sum / count;
		}
		return -1;
	}
	
	public void useRecordedDeviation(){
		setOfflinePresetConfiguration(1, getRecordedDeviation(1));
		setOfflinePresetConfiguration(2, getRecordedDeviation(2));
		setOfflinePresetConfiguration(3, getRecordedDeviation(3));
	}

	public int getOrder() {
		return order;
	}

	public HashMap<Integer, AbstractLocationPrivacyAlgorithm> getPresetAlgorithms() {
		return presetAlgorithms;
	}

	public int getPresetConfiguration(int preset) {
		switch (preset) {
		case 1:
			return Integer.parseInt(getConfiguration("street"));
		case 2:
			return Integer.parseInt(getConfiguration("postalcode"));
		case 3:
			return Integer.parseInt(getConfiguration("city"));
		}
		return -1;
	}

	public String getWebserviceHostAdress() {
		return webserviceHostAdress;
	}

	private void initialize() {
		SharedPreferences sharedPreference = PreferenceManager
				.getDefaultSharedPreferences(context);
		String password = sharedPreference.getString("password", "");
		if (password.equals("")) {
			password = generateRandomString();
			sharedPreference.edit().putString("password", password).commit();
		}
		String salt = sharedPreference.getString("salt", "");
		if (salt.equals("")) {
			salt = generateRandomString();
			sharedPreference.edit().putString("salt", salt).commit();
		}
		int iterationCount = sharedPreference.getInt("iterationCount", -1);
		if (iterationCount == -1) {
			iterationCount = (int) (Math.random() * 50.0) + 50;
			sharedPreference.edit().putInt("iterationCount", iterationCount)
					.commit();
		}
		if (database == null) {
			database = new CryptoDatabase(password, salt, iterationCount,
					context);
		}
		configurationNeeded = new ArrayList<String>();
		updateData();
	}

	public boolean isDialogInUse() {
		return dialogInUse;
	}

	public boolean isSharePrivacySettings() {
		return sharePrivacySettings;
	}

	public boolean isShowCommunityAdvice() {
		return showCommunityAdvice;
	}

	public boolean isShowOnlineInfo() {
		return showOnlineInfo;
	}

	public boolean isUseOnlineAlgorithm() {
		return useOnlineAlgorithm;
	}

	public boolean isUseStarsInDialog() {
		return useStarsInDialog;
	}

	public boolean isWebhostShareSettings() {
		return webhostShareSettings;
	}

	public boolean isWebhostShowCommunityAdvice() {
		return webhostShowCommunityAdvice;
	}

	/**
	 * Obfuscates location. Based on the uid the corresponding algorithm is
	 * used. If uid is new to framework, it is added with default values.
	 * 
	 * @param location
	 *            original location
	 * @param uid
	 *            uid app is running as
	 * @param name
	 *            app name
	 * @return obfuscated location
	 */
	public Location obfuscateLocation(Location location, String packagename) {
		Location obfuscatedLocation = null;
		int presetConfig = -1;
		int deviation = -1;
		if (location != null) {
			LocationPrivacyApplication app = null;
			for (LocationPrivacyApplication application : applications) {
				if (application.getPackagename().equals(packagename)) {
					app = application;
					break;
				}
			}
			if (app == null) {
				app = getApplication(packagename);
				if (app == null) {
					app = addApplication(packagename);
					Log.d(TAG, "added " + packagename);
				}
				applications.add(app);
			}

			Log.d(TAG, "obfuscateLocation for " + app);
			Bundle extras = location.getExtras();
			if (extras != null) {
				for (String key : extras.keySet()) {
					Log.d(TAG, "key: " + key + " ; value: " + extras.get(key));
				}
			}
			presetConfig = app.getPresetConfig();
			if (presetConfig == 0) {
				obfuscatedLocation = new Location(location);
				deviation = 0;
			} else if (presetConfig == 4) {
				obfuscatedLocation = null;
				deviation = -1;
			} else if (presetConfig > -1) {
				AbstractLocationPrivacyAlgorithm algorithm = presetAlgorithms
						.get(presetConfig);
				algorithm.setContext(context);
				if (algorithm instanceof GeoReverseGeo) {
					GeoReverseGeo geoAlg = (GeoReverseGeo) algorithm;
					geoAlg.setCachedLocationsStreet(cachedLocationsStreet);
					geoAlg.setCachedLocationsPostalcode(cachedLocationsPostalcode);
					geoAlg.setCachedLocationsCity(cachedLocationsCity);
					obfuscatedLocation = geoAlg
							.obfuscate(new Location(location));
				} else {
					obfuscatedLocation = algorithm.obfuscate(new Location(
							location));
				}
				if(location != null && obfuscatedLocation != null){
					deviation = (int) location.distanceTo(obfuscatedLocation);
				}
			} else {
				if (!configurationNeeded.contains(app.getPackagename())) {
					configurationNeeded.add(app.getPackagename());
				}
				checkForDialog();
			}
		} else {
			deviation = -1;
		}

		new Thread(new AccessUpdate(packagename, deviation, presetConfig, isUseOnlineAlgorithm())).start();
		return obfuscatedLocation;
	}

	/**
	 * Removes apps/configuration that have been deinstalled
	 */
	public void removeOldApplications() {

		List<ApplicationInfo> packages = context.getPackageManager()
				.getInstalledApplications(0);
		List<LocationPrivacyApplication> apps = getApplications();
		List<LocationPrivacyApplication> removedApps = new ArrayList<LocationPrivacyApplication>();
		for (LocationPrivacyApplication app : apps) {
			boolean remove = true;
			for (ApplicationInfo packageInfo : packages) {
				if (("" + packageInfo.packageName).equals(app.getPackagename())) {
					remove = false;
					break;
				}
			}
			if (remove) {
				removedApps.add(app);
			}
		}
		database.beginTransaction();
		try {
			for (LocationPrivacyApplication app : removedApps) {
				String packagename = app.getPackagename();
				database.delete("APPLICATION", "packagename = ?",
						new String[] { packagename });
			}
			database.setTransactionSuccessful();
		} finally {
			database.endTransaction();
		}
		dataChanged();
	}

	public void resetToFactoryDefaults() {
		database.delete("APPLICATION", null, null);
		database.delete("LASTACCESS", null, null);
		database.delete("STATISTICDEVIATION", null, null);
		database.delete("STATISTICACCESS", null, null);
		database.delete("WEBSERVICEDATA", null, null);
		database.delete("OFFLINEPARAMETER", null, null);

		setUseOnlineAlgorithm(useOnlineAlgorithmDefault);
		setDialogInUse(dialogInUseDefault);
		setShowCommunityAdvice(showCommunityAdviceDefault);
		setSharePrivacySettings(sharePrivacySettingsDefault);
		setShowOnlineInfo(showOnlineInfoDefault);
		setOfflinePresetConfiguration(1, streetDefault);
		setOfflinePresetConfiguration(2, postalcodeDefault);
		setOfflinePresetConfiguration(3, cityDefault);
		dataChanged();
	}

	public void setApplication(LocationPrivacyApplication app) {
		ContentValues values = new ContentValues();
		values.put("config", app.getPresetConfig());
		database.update("APPLICATION", values, "packagename = ?",
				new String[] { app.getPackagename() });
		database.delete("STATISTICDEVIATION", "packagename = ?",
				new String[] { app.getPackagename() });
		dataChanged();
		checkForDialog();
	}

	public void setBootComplete() {
		setConfiguration("bootComplete", "" + true);
		bootComplete = true;
	}

	private void setConfiguration(String key, String value) {
		ContentValues values = new ContentValues();
		values.put("value", value);
		database.update("Configuration", values, "key = ?",
				new String[] { key });
		dataChanged();
	}

	public void setDialogInUse(boolean dialogInUse) {
		setConfiguration("dialogInUse", "" + dialogInUse);
		this.dialogInUse = dialogInUse;
		Bundle extra = new Bundle();
		extra.putBoolean("checkForDialog", true);
		dataChanged(extra);
	}

	public void setMinDistance(int minDist) {
		setConfiguration("minDist", "" + minDist);
	}

	public void setOfflinePresetConfiguration(int preset, int radius) {
		switch (preset) {
		case 1:
			setConfiguration("street", "" + radius);
			break;
		case 2:
			setConfiguration("postalcode", "" + radius);
			break;
		case 3:
			setConfiguration("city", "" + radius);
			break;
		}
	}

	public void setOrder(int order) {
		setConfiguration("order", "" + order);
	}

	public void setSharePrivacySettings(boolean sharePrivacySettings) {
		setConfiguration("sharePrivacySettings", "" + sharePrivacySettings);
	}

	public void setShowCommunityAdvice(boolean showCommunityAdvice) {
		setConfiguration("showCommunityAdvice", "" + showCommunityAdvice);
	}

	public void setShowOnlineInfo(boolean showOnlineInfo) {
		setConfiguration("showOnlineInfo", "" + showOnlineInfo);
	}

	public void setUseOnlineAlgorithm(boolean useOnlineAlgorithm) {
		setConfiguration("useOnlineAlgorithm", "" + useOnlineAlgorithm);
	}

	public void setUseStarsInDialog(boolean useStarsInDialog) {
		setConfiguration("useStarsInDialog", "" + useStarsInDialog);
	}

	public void setWebhostShareSettings(boolean webhostShareSettings) {
		setConfiguration("webhostShareSettings", "" + webhostShareSettings);
	}
	
	public void setWebhostShowCommunityAdvice(boolean webhostShowCommunityAdvice) {
		setConfiguration("webhostShowCommunityAdvice", ""
				+ webhostShowCommunityAdvice);
	}

	public void setWebserviceHostAdress(String host) {
		setConfiguration("webserviceHostAdress", host);

	}

	/**
	 * Cleans cached data of LocationPrivacyApplication re-reads it from
	 * database
	 */
	public void updateData() {
		applications = new ArrayList<LocationPrivacyApplication>();
		presetAlgorithms = new HashMap<Integer, AbstractLocationPrivacyAlgorithm>();
		showCommunityAdvice = Boolean
				.parseBoolean(getConfiguration("showCommunityAdvice"));
		sharePrivacySettings = Boolean
				.parseBoolean(getConfiguration("sharePrivacySettings"));
		useOnlineAlgorithm = Boolean
				.parseBoolean(getConfiguration("useOnlineAlgorithm"));
		dialogInUse = Boolean.parseBoolean(getConfiguration("dialogInUse"));
		showOnlineInfo = Boolean
				.parseBoolean(getConfiguration("showOnlineInfo"));
		bootComplete = Boolean.parseBoolean(getConfiguration("bootComplete"));
		webserviceHostAdress = getConfiguration("webserviceHostAdress");
		webhostShareSettings = Boolean
				.parseBoolean(getConfiguration("webhostShareSettings"));
		webhostShowCommunityAdvice = Boolean
				.parseBoolean(getConfiguration("webhostShowCommunityAdvice"));

		useStarsInDialog = (Boolean
				.parseBoolean(getConfiguration("useStarsInDialog")));
		order = Integer.parseInt(getConfiguration("order"));
		if (useOnlineAlgorithm) {
			GeoReverseGeo geo1 = new GeoReverseGeo();
			geo1.getConfiguration().setEnumChoosen("detail", "street");
			presetAlgorithms.put(1, geo1);
			GeoReverseGeo geo2 = new GeoReverseGeo();
			geo2.getConfiguration().setEnumChoosen("detail", "postalcode");
			presetAlgorithms.put(2, geo2);
			GeoReverseGeo geo3 = new GeoReverseGeo();
			geo3.getConfiguration().setEnumChoosen("detail", "city");
			presetAlgorithms.put(3, geo3);
		} else {
			int street = Integer.parseInt(getConfiguration("street"));
			int postalcode = Integer.parseInt(getConfiguration("postalcode"));
			int city = Integer.parseInt(getConfiguration("city"));
			int minDist = Integer.parseInt(getConfiguration("minDist"));
			RadiusDistance r1 = new RadiusDistance();
			r1.getConfiguration().setInt("radius", street);
			r1.getConfiguration().setInt("distance", minDist);
			presetAlgorithms.put(1, r1);
			RadiusDistance r2 = new RadiusDistance();
			r2.getConfiguration().setInt("radius", postalcode);
			r2.getConfiguration().setInt("distance", minDist);
			presetAlgorithms.put(2, r2);
			RadiusDistance r3 = new RadiusDistance();
			r3.getConfiguration().setInt("radius", city);
			r3.getConfiguration().setInt("distance", minDist);
			presetAlgorithms.put(3, r3);
		}
	}

	private void updateLocationAccessStatistic(String packagename, int distance, int config, boolean online) {
		ContentValues values = new ContentValues();
		Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));

		Cursor c = database
				.query("STATISTICACCESS",
						new String[] { "count" },
						"packagename = ? AND day = ? AND month = ? AND year = ? AND hour = ?",
						new String[] { packagename,
								"" + now.get(Calendar.DAY_OF_MONTH),
								"" + now.get(Calendar.MONTH),
								"" + now.get(Calendar.YEAR),
								"" + now.get(Calendar.HOUR_OF_DAY) }, null,
						null, null);
		if (c.getCount() == 0) {
			values.put("packagename", packagename);
			values.put("day", now.get(Calendar.DAY_OF_MONTH));
			values.put("month", now.get(Calendar.MONTH));
			values.put("year", now.get(Calendar.YEAR));
			values.put("hour", now.get(Calendar.HOUR_OF_DAY));
			values.put("count", 1);
			database.insert("STATISTICACCESS", null, values);
		} else {
			c.moveToFirst();
			ContentValues updateValues = new ContentValues();
			updateValues.put("count", c.getInt(0) + 1);
			database.update(
					"STATISTICACCESS",
					updateValues,
					"packagename = ? AND day = ? AND month = ? AND year = ? AND hour = ?",
					new String[] { packagename,
							"" + now.get(Calendar.DAY_OF_MONTH),
							"" + now.get(Calendar.MONTH),
							"" + now.get(Calendar.YEAR),
							"" + now.get(Calendar.HOUR_OF_DAY) });
			c.close();
		}
		
		if(distance >= 0){
			Cursor distanceC = database.query("STATISTICDEVIATION",
					new String[] { "deviationsum", "count" },
					"packagename = ?", new String[] { packagename }, null,
					null, null);
			if (distanceC.getCount() == 0) {
				ContentValues devValues = new ContentValues();
				devValues.put("packagename", packagename);
				devValues.put("deviationsum", distance);
				devValues.put("count", 1);
				database.insert("STATISTICDEVIATION", null, devValues);
			} else {
				distanceC.moveToFirst();
				ContentValues updateDevValues = new ContentValues();
				updateDevValues.put("deviationsum", distanceC.getDouble(0)
						+ distance);
				updateDevValues.put("count", distanceC.getInt(1) + 1);
				database.update("STATISTICDEVIATION", updateDevValues,
						"packagename = ?", new String[] { packagename });
				distanceC.close();
			}
		}
		
		
		Cursor lastAccessC = database.query("LASTACCESS", null, "packagename = ?",
				new String[] { packagename }, null, null, null);
		
		
		ContentValues lastaccess = new ContentValues();
		lastaccess.put("day", now.get(Calendar.DAY_OF_MONTH));
		lastaccess.put("month", now.get(Calendar.MONTH));
		lastaccess.put("year", now.get(Calendar.YEAR));
		lastaccess.put("hour", now.get(Calendar.HOUR_OF_DAY));
		lastaccess.put("minute", now.get(Calendar.MINUTE));
		lastaccess.put("packagename", packagename);
		if(lastAccessC.getCount() == 0){
			database.insert("LASTACCESS", null, lastaccess);
		} else {
			database.update("LASTACCESS", lastaccess, "packagename = ?",
					new String[] { packagename });
		}

		
		if(online){
			int sum = 0;
			int count = 0;
			Cursor cOffline = database.query("OFFLINEPARAMETER", new String[]{"sum", "count"},"config = ?", new String[]{"" + config}, null, null, null);
			cOffline.moveToFirst();
			if(!cOffline.isAfterLast()){
				sum = cOffline.getInt(0);
				count = cOffline.getInt(1);
			}
			
			sum += distance;
			count++;
			cOffline.close();
			
			ContentValues offline = new ContentValues();
			offline.put("sum", sum);
			offline.put("count", count);
			if(count == 1){
				offline.put("config", config);
				database.insert("OFFLINEPARAMETER", null, offline);
			} else {
				database.update("OFFLINEPARAMETER", offline, "config = ?", new String[]{"" + config});

			}
		}
	}
}
