/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package no.nordicsemi.android.nrfbeacon.beacon.adapter;

import java.util.List;

import no.nordicsemi.android.nrfbeacon.R;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;

/**
 * Provides the list of all applications with {@link Intent#CATEGORY_LAUNCHER} category.
 */
public class ApplicationAdapter extends BaseAdapter {
	private final PackageManager mPackageManager;
	private final LayoutInflater mInflater;
	private final List<ResolveInfo> mApplications;
	private String mSelectedAppPackage;

	public ApplicationAdapter(final Context context, final String selectedAppPackage) {
		mPackageManager = context.getPackageManager();
		mInflater = LayoutInflater.from(context);
		mSelectedAppPackage = selectedAppPackage;

		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		mApplications = context.getPackageManager().queryIntentActivities(mainIntent, 0);
	}

	@Override
	public int getCount() {
		return mApplications.size();
	}

	@Override
	public Object getItem(int position) {
		return mApplications.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		CheckedTextView view = (CheckedTextView) convertView;
		if (view == null) {
			view = (CheckedTextView) mInflater.inflate(R.layout.icon_list_item, parent, false);
		}
		final ResolveInfo info = (ResolveInfo) getItem(position);
		view.setText(info.loadLabel(mPackageManager));
		view.setCompoundDrawablesWithIntrinsicBounds(info.loadIcon(mPackageManager), null, null, null);
		view.setChecked(mSelectedAppPackage.equals(info.activityInfo.applicationInfo.packageName));
		return view;
	}
}
