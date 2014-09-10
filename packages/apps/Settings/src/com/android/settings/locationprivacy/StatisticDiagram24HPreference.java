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

import java.util.Calendar;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Color;
import android.preference.Preference;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.android.settings.R;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.StatisticGraphView;

public class StatisticDiagram24HPreference extends Preference {

	private Context context;
	private HashMap<Calendar, Integer> dates;
	private int maxY;


	public StatisticDiagram24HPreference(Context context, HashMap<Calendar, Integer> dates) {
		super(context);
		this.context = context;
		this.dates = dates;
		this.maxY = 0;
	}

	private GraphViewSeries createData() {
		GraphViewData[] data = new GraphViewData[24];
		this.maxY = 0;
		for (Calendar cal : dates.keySet()) {
			int x = getXValueFromDate(cal);
			int y = dates.get(cal);
			if(x>=0){
				System.out.println("Added Value to 24h statistik: [Cal]: " + cal + " [x]: " + x + " [y]: " + y);
				data[x] = new GraphViewData(x, y);
				
			}
			this.maxY = Math.max(maxY, y);
		}
		for (int i = 0; i < data.length; i++) {
			if(data[i] == null){
				data[i] = new GraphViewData(i,0);
			}
		}
		return new GraphViewSeries(data);
	}

	private GraphView createGraph() {
		StatisticGraphView graphView = new StatisticGraphView(context, getContext()
				.getResources().getString(
						R.string.lp_settings_statistic_acces_last24h), false) {
			@Override
			protected String formatLabel(double value, boolean isValueX) {
				if (isValueX) {
					Calendar cal = getDateFromXValue((int) value);
					cal.set(Calendar.MINUTE, 0);
					
					return DateFormat.getTimeFormat(context).format(
							cal.getTime());//String.format("%02d", cal.get(Calendar.HOUR_OF_DAY)) + ":00";
				}
				return "" + (int) value;
			}
		};
		graphView.setViewPort(0, 23);
		graphView.getGraphViewStyle().setNumVerticalLabels(5);
		graphView.getGraphViewStyle().setNumHorizontalLabels(24);
		graphView.getGraphViewStyle().setGridColor(Color.WHITE);
		graphView.getGraphViewStyle().setTextSize(21);
		graphView.addSeries(createData());
		System.out.println("maxY "+this.maxY);
                int maxY = Math.max(this.maxY, 4);
                maxY = ((int) Math.ceil(maxY / 4.)) * 4;
		System.out.println("_maxY "+maxY);
                graphView.setManualYAxisBounds(maxY, 0);
 		return graphView;

	}

	private Calendar getDateFromXValue(int x) {
		int hoursbetween = 23 - x;
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY)
				- hoursbetween);
		return cal;
	}
	
	private int getXValueFromDate(Calendar cal){
		Calendar now = Calendar.getInstance();
		return 23 - hoursbetween(cal, now);
	}
	
	public int hoursbetween(Calendar d1, Calendar d2) {
		return (int) (Math.abs(d2.getTimeInMillis() - d1.getTimeInMillis()) / (1000 * 60 * 60));
	}

	protected View onCreateView(ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.statistic_diagram, null);
		LinearLayout layout = (LinearLayout) view.findViewById(R.id.layout);
		layout.addView(createGraph());
		return view;
	}

}
