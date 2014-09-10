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

import android.app.Activity;
import android.locationprivacy.control.LocationPrivacyManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.android.settings.R;

public class LocationPrivacyOnlineInfoActivity extends Activity implements OnClickListener{

	private LocationPrivacyManager lpManager;
	private Button ok;

	@Override
	public void onClick(View v) {
		lpManager.setShowOnlineInfo(false);
		this.finish();	
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.online_info);
		lpManager = new LocationPrivacyManager(this);
		ok = (Button) findViewById(R.id.info_ok);
		ok.setOnClickListener(this);
	}

}
