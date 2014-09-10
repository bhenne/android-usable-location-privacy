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

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.TextView;

import com.android.settings.R;

public class LPPresetConfigAdapter extends BaseAdapter {

	private List<Integer> images;
	private String[] labels;
	private OnClickListener listener;
	private int[] stars;

	private int checked;
	private LayoutInflater inflater;

	public LPPresetConfigAdapter(List<Integer> images, String[] labels,
			Context context, OnClickListener listener) {
		super();
		this.images = images;
		this.labels = labels;
		checked = -1;
		inflater = LayoutInflater.from(context);
		this.listener = listener;
		stars = new int[labels.length];

	}

	@Override
	public int getCount() {
		return images.size();
	}

	@Override
	public String getItem(int position) {
		return labels[position];
	}

	public Integer getImage(int position) {
		return images.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder = new Holder();
		
		convertView = inflater.inflate(R.layout.lp_dialog_listview_element, null);
		holder.title = (TextView) convertView.findViewById(R.id.lp_dialog_listview_title);
		holder.check = (RadioButton) convertView.findViewById(R.id.lp_dialog_listview_check);
		holder.icon =  (ImageView) convertView.findViewById(R.id.lp_dialog_listview_icon);
		holder.stars = (RatingBar) convertView.findViewById(R.id.ratingBar1); 
		convertView.setTag(holder);
		
		holder.title.setText(getItem(position));
		holder.check.setChecked(position == checked);
		holder.check.setClickable(false);
		holder.icon.setImageResource(getImage(position));
		
		if(stars[position] > 0){
			holder.stars.setVisibility(View.VISIBLE);
			holder.stars.setNumStars(stars[position]);
		} else {
			holder.stars.setVisibility(View.GONE);
		}
		
		convertView.setOnClickListener(listener);
		convertView.setId(position);
		
		return convertView;
	}

	public int getChecked() {
		return checked;
	}

	public void setChecked(int checked) {
		this.checked = checked;
	}

	public void setStars(int[] stars) {
		this.stars = stars;
	}

	/**
	 * Platzhalterklasse fuer die Elemente der einzelnen Zeilen.
	 */
	
	 static class Holder {

		TextView title;
		ImageView icon;
		RadioButton check;
		RatingBar stars;

	}
	
}

