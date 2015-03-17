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
package no.nordicsemi.android.nrfbeacon.beacon;

import java.util.UUID;

import net.dinglisch.android.tasker.TaskerIntent;
import no.nordicsemi.android.beacon.Beacon;
import no.nordicsemi.android.beacon.BeaconRegion;
import no.nordicsemi.android.beacon.BeaconServiceConnection;
import no.nordicsemi.android.beacon.Proximity;
import no.nordicsemi.android.nrfbeacon.R;
import no.nordicsemi.android.nrfbeacon.beacon.adapter.BeaconAdapter;
import no.nordicsemi.android.nrfbeacon.database.BeaconContract;
import no.nordicsemi.android.nrfbeacon.database.DatabaseHelper;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

public class BeaconsListFragment extends ListFragment implements BeaconServiceConnection.BeaconsListener, BeaconServiceConnection.RegionListener {
	private BeaconsFragment mParentFragment;
	private DatabaseHelper mDatabaseHelper;
	private BeaconAdapter mAdapter;

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mParentFragment = (BeaconsFragment) getParentFragment();
		mDatabaseHelper = mParentFragment.getDatabaseHelper();
	}

	@Override
	public void onStart() {
		super.onStart();

		final Cursor cursor = mDatabaseHelper.getAllRegions();
		setListAdapter(mAdapter = new BeaconAdapter(getActivity(), cursor));
	}

	@Override
	public void onResume() {
		super.onResume();
		mParentFragment.startScanning();
	}

	@Override
	public void onPause() {
		super.onPause();
		mParentFragment.stopScanning();
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_beacons_list, container, false);
	}

	@Override
	public void onListItemClick(final ListView l, final View v, final int position, final long id) {
		mParentFragment.onEditRegion(id);
	}

	/**
	 * Registers for monitoring and ranging events for all regions in the database.
	 * 
	 * @param serviceConnection
	 *            the service connection used to bing activity to the service
	 */
	public void startScanning(final BeaconServiceConnection serviceConnection) {
		final Cursor cursor = mDatabaseHelper.getAllRegions();
		while (cursor.moveToNext()) {
			final UUID uuid = UUID.fromString(cursor.getString(2 /* UUID */));
			final int major = cursor.getInt(3 /* MAJOR */);
			final int minor = cursor.getInt(4 /* MINOR */);
			final int event = cursor.getInt(6 /* EVENT */);

			// We must start ranging for all beacons
			serviceConnection.startRangingBeaconsInRegion(uuid, major, minor, this);
			// And additionally start monitoring only for those with these two events set
			if (event == BeaconContract.EVENT_IN_RANGE || event == BeaconContract.EVENT_OUT_OF_RANGE)
				serviceConnection.startMonitoringForRegion(uuid, major, minor, this);
		}
	}

	/**
	 * Unregisters the fragment from receiving monitoring and ranging events.
	 * 
	 * @param serviceConnection
	 *            the service connection used to bind activity with the beacon service
	 */
	public void stopScanning(final BeaconServiceConnection serviceConnection) {
		if (serviceConnection != null) {
			serviceConnection.stopMonitoringForRegion(this);
			serviceConnection.stopRangingBeaconsInRegion(this);
		}
	}

	@Override
	public void onBeaconsInRegion(final Beacon[] beacons, final BeaconRegion region) {
		if (beacons.length > 0) {
			final Cursor cursor = mDatabaseHelper.findRegion(region);
			try {
				if (cursor.moveToNext()) {
					// Check and fire events
					final int event = cursor.getInt(6 /* EVENT */);
					for (final Beacon beacon : beacons) {
						if (event == BeaconContract.EVENT_ON_TOUCH && Proximity.IMMEDIATE.equals(beacon.getProximity()) && Proximity.NEAR.equals(beacon.getPreviousProximity())) {
							fireEvent(cursor);
							break;
						}
						if (event == BeaconContract.EVENT_GET_NEAR && Proximity.NEAR.equals(beacon.getProximity()) && Proximity.FAR.equals(beacon.getPreviousProximity())) {
							fireEvent(cursor);
							break;
						}
					}

					// Update signal strength in the database
					float accuracy = 5;
					for (final Beacon beacon : beacons)
						if (Proximity.UNKNOWN != beacon.getProximity() && beacon.getAccuracy() < accuracy)
							accuracy = beacon.getAccuracy();
					accuracy = -20 * accuracy + 100;
					mDatabaseHelper.updateRegionSignalStrength(cursor.getLong(0 /* _ID */), (int) accuracy);
				}
			} finally {
				cursor.close();
			}
			mAdapter.swapCursor(mDatabaseHelper.getAllRegions());
		}
	}

	@Override
	public void onEnterRegion(final BeaconRegion region) {
		final Cursor cursor = mDatabaseHelper.findRegion(region);
		try {
			if (cursor.moveToNext()) {
				final int event = cursor.getInt(6 /* EVENT */);
				if (event == BeaconContract.EVENT_IN_RANGE) {
					fireEvent(cursor);
				}
			}
		} finally {
			cursor.close();
		}
	}

	@Override
	public void onExitRegion(final BeaconRegion region) {
		final Cursor cursor = mDatabaseHelper.findRegion(region);
		try {
			if (cursor.moveToNext()) {
				final int event = cursor.getInt(6 /* EVENT */);
				if (event == BeaconContract.EVENT_OUT_OF_RANGE) {
					fireEvent(cursor);
				}
			}
		} finally {
			cursor.close();
		}
	}

	/**
	 * Fires the event associated with the region at the current position of the cursor.
	 * 
	 * @param cursor
	 *            the cursor with a region details obtained by f.e. {@link DatabaseHelper#findRegion(BeaconRegion)}. The cursor has to be moved to the proper position.
	 */
	private void fireEvent(final Cursor cursor) {
		final boolean enabled = cursor.getInt(9 /* ENABLED */) == 1;
		if (!enabled)
			return;

		final int action = cursor.getInt(7 /* ACTION */);
		final String actionParam = cursor.getString(8 /* ACTION PARAM */);

		switch (action) {
		case BeaconContract.ACTION_MONA_LISA: {
			mParentFragment.stopScanning();
			final DialogFragment dialog = new MonalisaFragment();
			dialog.show(mParentFragment.getChildFragmentManager(), "monalisa");
			break;
		}
		case BeaconContract.ACTION_SILENT: {
			final AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
			audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE | AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_ALLOW_RINGER_MODES);
			audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
			break;
		}
		case BeaconContract.ACTION_ALARM: {
			final Uri alarm = RingtoneManager.getActualDefaultRingtoneUri(getActivity(), RingtoneManager.TYPE_ALARM);
			final Notification notification = new Notification.Builder(getActivity()).setContentTitle(getString(R.string.alarm_notification_title))
					.setContentText(getString(R.string.alarm_notification_message, cursor.getString(1 /* NAME */))).setSmallIcon(R.drawable.stat_sys_nrf_beacon).setAutoCancel(true)
					.setOnlyAlertOnce(true).setSound(alarm, AudioManager.STREAM_ALARM).build();
			final NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(1, notification);
			break;
		}
		case BeaconContract.ACTION_URL: {
			mParentFragment.stopScanning();
			try {
				final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(actionParam));
				startActivity(intent);
			} catch (final ActivityNotFoundException e) {
				Toast.makeText(getActivity(), R.string.no_application, Toast.LENGTH_SHORT).show();
			}
			break;
		}
		case BeaconContract.ACTION_APP: {
			mParentFragment.stopScanning();
			try {
				final Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.setPackage(actionParam);
				startActivity(intent);
			} catch (final ActivityNotFoundException e) {
				Toast.makeText(getActivity(), R.string.no_given_application, Toast.LENGTH_SHORT).show();
			}
			break;
		}
		case BeaconContract.ACTION_TASKER:
			switch (TaskerIntent.testStatus(getActivity())) {
			case OK:
				final TaskerIntent i = new TaskerIntent(actionParam);
				final BroadcastReceiver br = new BroadcastReceiver() {
					@Override
					public void onReceive(final Context context, final Intent recIntent) {
						if (recIntent.getBooleanExtra(TaskerIntent.EXTRA_SUCCESS_FLAG, false))
							Toast.makeText(getActivity(), R.string.tasker_success, Toast.LENGTH_SHORT).show();
						getActivity().unregisterReceiver(this);
					}
				};
				getActivity().registerReceiver(br, i.getCompletionFilter());
				// Start the task
				getActivity().sendBroadcast(i);
				break;
			case NotEnabled:
				Toast.makeText(getActivity(), R.string.tasker_disabled, Toast.LENGTH_SHORT).show();
				break;
			case AccessBlocked:
				Toast.makeText(getActivity(), R.string.tasker_external_access_denided, Toast.LENGTH_SHORT).show();
				break;
			case NotInstalled:
				Toast.makeText(getActivity(), R.string.tasker_not_installed, Toast.LENGTH_SHORT).show();
				break;
			default:
				Toast.makeText(getActivity(), R.string.tasker_error, Toast.LENGTH_SHORT).show();
				break;
			}
			break;
		}
	}
}
