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

import no.nordicsemi.android.beacon.Beacon;
import no.nordicsemi.android.beacon.BeaconRegion;
import no.nordicsemi.android.beacon.BeaconServiceConnection;
import no.nordicsemi.android.beacon.ServiceProxy;
import no.nordicsemi.android.nrfbeacon.MainActivity;
import no.nordicsemi.android.nrfbeacon.R;
import no.nordicsemi.android.nrfbeacon.database.BeaconContract;
import no.nordicsemi.android.nrfbeacon.database.DatabaseHelper;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class BeaconsFragment extends Fragment {
	private static final String BEACONS_FRAGMENT = "beaconsFragment";
	private static final String SCANNER_FRAGMENT = "scannerFragment";

	private DatabaseHelper mDatabaseHelper;
	private BeaconsListFragment mBeaconsListFragment;
	private BeaconScannerFragment mScannerFragment;

	private boolean mServiceConnected;
	private boolean mFragmentResumed;
	private BeaconServiceConnection mServiceConnection = new BeaconServiceConnection() {
		@Override
		public void onServiceConnected() {
			mServiceConnected = true;

			final BeaconScannerFragment scannerFragment = mScannerFragment;
			if (scannerFragment != null) {
				startRangingBeaconsInRegion(BeaconRegion.ANY_UUID, scannerFragment);
			} else {
				final FragmentManager fm = getChildFragmentManager();
				if (fm.getBackStackEntryCount() == 0) {
					// Start scan only if there is no any other fragment (Mona Lisa) open
					startScanning();
				}
			}
		}

		@Override
		public void onServiceDisconnected() {
			mServiceConnected = false;
		}
	};

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Check if beacons list fragment already exists
		BeaconsListFragment fragment = (BeaconsListFragment) getChildFragmentManager().findFragmentByTag(BEACONS_FRAGMENT);
		if (fragment == null) {
			fragment = new BeaconsListFragment();
			final FragmentTransaction ft = getChildFragmentManager().beginTransaction();
			ft.add(R.id.content, fragment, BEACONS_FRAGMENT);
			ft.commit();
		}
		mBeaconsListFragment = fragment;

		// Restore the scanner fragment if it was opened
		mScannerFragment = (BeaconScannerFragment) getChildFragmentManager().findFragmentByTag(SCANNER_FRAGMENT);
	}

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);

		final MainActivity parent = (MainActivity) activity;
		parent.setBeaconsFragment(this);
	}

	/**
	 * As the fragment is in the ViewPager the onResume() and onPause() methods are called when it is added to the cache, even if it is not visible.
	 * By using {@link #onFragmentResumed()} and {@link #onFragmentPaused()}, which are called by the {@link ViewPager.OnPageChangeListener} we ensure
	 * that the fragment is visible.
	 */
	public void onFragmentResumed() {
		if (mFragmentResumed)
			return;

		mFragmentResumed = true;
		bindService();
	}

	/**
	 * @see #onFragmentResumed()
	 */
	public void onFragmentPaused() {
		if (!mFragmentResumed)
			return;

		mFragmentResumed = false;
		unbindService();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		final MainActivity parent = (MainActivity) getActivity();
		parent.setBeaconsFragment(null);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mDatabaseHelper = new DatabaseHelper(getActivity());
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_container, container, false);
	}

	@Override
	public void onViewCreated(final View view, final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setHasOptionsMenu(true);
	}

	/**
	 * Opens the configuration screen for the region with given id.
	 * 
	 * @param id
	 *            the id of the region in the DB
	 */
	public void onEditRegion(final long id) {
		final Intent intent = new Intent(getActivity(), BeaconsDetailsActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		intent.putExtra(BeaconsDetailsActivity.ID, id);
		startActivity(intent);
	}

	/**
	 * Opens the scanned fragment. Starts ranging for any beacon in immediate position.
	 */
	public void onAddOrEditRegion() {
		stopScanning();

		final BeaconScannerFragment fragment = mScannerFragment = new BeaconScannerFragment();
		fragment.show(getChildFragmentManager(), SCANNER_FRAGMENT);

		mServiceConnection.startRangingBeaconsInRegion(BeaconRegion.ANY_UUID, fragment);
	}

	/**
	 * Stops ranging for any beacon in immediate position. Opens configuration for selected beacon. It's being added if didn't exist before.
	 * 
	 * @param beacon
	 *            the beacon that was tapped
	 */
	public void onScannerClosedWithResult(final Beacon beacon) {
		mServiceConnection.stopRangingBeaconsInRegion(mScannerFragment);
		mScannerFragment = null;

		final Cursor cursor = mDatabaseHelper.findRegionByBeacon(beacon);
		try {
			long id;
			if (cursor.moveToNext()) {
				// Update beacon
				id = cursor.getLong(0 /* _ID */);
			} else {
				// Add new beacon
				id = mDatabaseHelper.addRegion(beacon, getString(R.string.default_beacon_name), BeaconContract.EVENT_GET_NEAR, BeaconContract.ACTION_MONA_LISA, null);
			}
			onEditRegion(id);
		} finally {
			cursor.close();
		}
	}

	/**
	 * Stops ranging for any beacons in immediate position and starts for added beacons.
	 */
	public void onScannerClosed() {
		mServiceConnection.stopRangingBeaconsInRegion(mScannerFragment);
		mScannerFragment = null;

		startScanning();
	}

	/**
	 * Starts scanning for added beacons if the service is connected.
	 */
	public void startScanning() {
		if (mServiceConnected) {
			mBeaconsListFragment.startScanning(mServiceConnection);
		}
	}

	/**
	 * Stops scanning for added beacons if the service is connected.
	 */
	public void stopScanning() {
		if (mServiceConnected) {
			mBeaconsListFragment.stopScanning(mServiceConnection);
		}
	}

	/**
	 * Binds the app with the beacons service. If it's not installed on Android 4.3 or 4.4 it asks for download. On Android 5+ the service is built into the beacon application.
	 */
	private void bindService() {
		final boolean success = ServiceProxy.bindService(getActivity(), mServiceConnection);
		if (!success) {
			new AlertDialog.Builder(getActivity()).setTitle(R.string.service_required_title).setMessage(R.string.service_required_message)
					.setPositiveButton(R.string.service_required_store, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							final Intent playIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(MainActivity.NRF_BEACON_SERVICE_URL));
							startActivity(playIntent);
						}
					}).setOnCancelListener(new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(final DialogInterface dialog) {
							dialog.dismiss();
							getActivity().finish();
						}
					}).show();
		}
	}

	private void unbindService() {
		// Unbinding service will stop all active scanning listeners
		ServiceProxy.unbindService(getActivity(), mServiceConnection);
		mDatabaseHelper.resetSignalStrength();
	}

	/**
	 * Returns the database helper object.
	 * 
	 * @return the database helper which may be used to access DB
	 */
	public DatabaseHelper getDatabaseHelper() {
		return mDatabaseHelper;
	}
}
