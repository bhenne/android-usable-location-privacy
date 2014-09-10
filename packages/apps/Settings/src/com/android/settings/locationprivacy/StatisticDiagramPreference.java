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
import java.util.Date;
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
import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.jjoe64.graphview.StatisticGraphView;

public class StatisticDiagramPreference extends Preference {

	private Context context;
	private HashMap<Calendar, Integer> dates;
	private int maxY;

	public StatisticDiagramPreference(Context context, HashMap<Calendar, Integer> dates) {
		super(context);
		this.context = context;
		this.dates = dates;
		this.maxY = 0;

	}

	private GraphViewSeries createData() {
		GraphViewData[] data = new GraphViewData[29];
		this.maxY = 0;
		for (Calendar cal : dates.keySet()) {
			int x = getXValueFromDate(cal);
			int y = dates.get(cal);
			if (x >= 0) {
				System.out.println("Added Value to statistik: [Date]: " + cal
						+ " [x]: " + x + " [y]: " + y);
				data[x] = new GraphViewData(x, y);

			}
                        this.maxY = Math.max(maxY, y);
		}
		for (int i = 0; i < data.length; i++) {
			if (data[i] == null) {
				data[i] = new GraphViewData(i, 0);
			}
		}
		return new GraphViewSeries(data);
	}

	private GraphView createGraph() {
		StatisticGraphView graphView = new StatisticGraphView(context, getContext()
				.getResources().getString(
						R.string.lp_settings_statistic_acces_lastmonth), true) {
			@Override
			protected String formatLabel(double value, boolean isValueX) {
				if (isValueX) {
					Calendar cal = getDateFromXValue((int) value);
					return "" + String.format("%02d", cal.get(Calendar.DAY_OF_MONTH)) + "." + String.format("%02d", (cal.get(Calendar.MONTH) + 1))
							+ ".";
				}
				return "" + (int) value;
			}
		};
		graphView.setViewPort(0, 27);
		graphView.getGraphViewStyle().setNumVerticalLabels(5);
		graphView.getGraphViewStyle().setNumHorizontalLabels(28);
		graphView.getGraphViewStyle().setGridColor(Color.WHITE);
		graphView.getGraphViewStyle().setTextSize(21); 
		
		graphView.addSeries(createData());
		int maxY = Math.max(this.maxY, 4);
		maxY = ((int) Math.ceil(maxY / 4.)) * 4;
		graphView.setManualYAxisBounds(maxY, 0);
		return graphView;

	}

	public int daysBetween(Calendar d1, Calendar d2) {
		return (int) (Math.abs(d2.getTimeInMillis() - d1.getTimeInMillis()) / (1000 * 60 * 60 * 24));
	}

	private Calendar getDateFromXValue(int x) {
		int daysbetween = 28 - x;
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH)
				- daysbetween);
		return cal;
	}

	private int getXValueFromDate(Calendar cal) {
		int daysBetween = daysBetween(cal, Calendar.getInstance());
		return 28 - daysBetween;
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
