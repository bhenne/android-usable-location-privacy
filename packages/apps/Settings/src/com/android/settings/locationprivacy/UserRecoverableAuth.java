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
import android.content.Intent;
import android.locationprivacy.control.LocationPrivacyManager;
import android.os.Bundle;

public class UserRecoverableAuth extends Activity{

	private static final int REQUEST_CODE = 22;
	private LocationPrivacyManager lpManager;
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQUEST_CODE){
			if(resultCode != Activity.RESULT_OK){
				lpManager.setSharePrivacySettings(false);
			}
			this.finish();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		lpManager = new LocationPrivacyManager(this);
		startActivityForResult((Intent) getIntent().getParcelableExtra("authIntent"), REQUEST_CODE);
		super.onCreate(savedInstanceState);
	}
}