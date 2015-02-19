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
import no.nordicsemi.android.nrfbeacon.database.BeaconContract;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Provides the list of all saved regions.
 */
public class BeaconAdapter extends CursorAdapter {
	private final PackageManager mPackageManager;
	private final LayoutInflater mInflater;

	public BeaconAdapter(final Context context, final Cursor c) {
		super(context, c, 0);

		mPackageManager = context.getPackageManager();
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
		final View view = mInflater.inflate(R.layout.fragment_beacons_item, parent, false);

		final ViewHolder holder = new ViewHolder();
		holder.name = (TextView) view.findViewById(R.id.name);
		holder.signal = (ProgressBar) view.findViewById(R.id.progress);
		holder.event = (ImageView) view.findViewById(R.id.event);
		holder.action = (ImageView) view.findViewById(R.id.action);
		view.setTag(holder);
		return view;
	}

	@Override
	public void bindView(final View view, final Context context, final Cursor cursor) {
		final boolean enabled = cursor.getInt(9 /* ENABLED */) == 1;

		final ViewHolder holder = (ViewHolder) view.getTag();
		holder.name.setText(cursor.getString(1 /* NAME */));
		holder.signal.setProgress(cursor.getInt(5 /* SIGNAL_STRENGTH */));
		holder.event.setImageLevel(cursor.getInt(6 /* EVENT */));

		final int action = cursor.getInt(7 /* ACTION */);
		switch (action) {
		case BeaconContract.ACTION_APP:
			final String application = cursor.getString(8 /* ACTION_PARAM */);

			final PackageManager pm = mPackageManager;
			final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
			mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			mainIntent.setPackage(application);
			final List<ResolveInfo> applications = pm.queryIntentActivities(mainIntent, 0);
			if (applications.size() == 1) {
				final ResolveInfo info = applications.get(0);
				holder.action.setImageDrawable(info.loadIcon(pm));
			} else {
				holder.action.setImageResource(R.drawable.ic_action);
				holder.action.setImageLevel(action);
			}
			break;
		default:
			holder.action.setImageResource(R.drawable.ic_action);
			holder.action.setImageLevel(action);
			break;
		}
		holder.name.setAlpha(enabled ? 1.0f : 0.5f);
		holder.event.setAlpha(enabled ? 1.0f : 0.5f);
		holder.action.setAlpha(enabled ? 1.0f : 0.5f);
	}

	private class ViewHolder {
		private TextView name;
		private ProgressBar signal;
		private ImageView event;
		private ImageView action;
	}

}
