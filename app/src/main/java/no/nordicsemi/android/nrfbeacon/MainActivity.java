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
package no.nordicsemi.android.nrfbeacon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import no.nordicsemi.android.nrfbeacon.beacon.BeaconsFragment;
import no.nordicsemi.android.nrfbeacon.dfu.DfuFragment;
import no.nordicsemi.android.nrfbeacon.update.UpdateFragment;

public class MainActivity extends AppCompatActivity {
	public static final String NRF_BEACON_SERVICE_URL = "market://details?id=no.nordicsemi.android.beacon.service";
	public static final String OPENED_FROM_LAUNCHER = "no.nordicsemi.android.nrfbeacon.extra.opened_from_launcher";
	public static final String EXTRA_OPEN_DFU = "no.nordicsemi.android.nrfbeacon.extra.open_dfu";

	private static final int REQUEST_ENABLE_BT = 1;

	private BeaconsFragment mBeaconsFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Ensure that Bluetooth exists
		if (!ensureBleExists())
			finish();

		// Setup the custom toolbar
		final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// Setup the FloatingActionButton (FAB)
		final FloatingActionButton fabAdd = (FloatingActionButton) findViewById(R.id.fab_add);
		fabAdd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (mBeaconsFragment != null)
					mBeaconsFragment.onAddOrEditRegion();
			}
		});

		// Prepare the sliding tab layout and the view pager
		final TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
		final ViewPager pager = (ViewPager) findViewById(R.id.view_pager);
		pager.setOffscreenPageLimit(2);
		pager.setAdapter(new FragmentAdapter(getSupportFragmentManager()));

		tabLayout.setupWithViewPager(pager);
		pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageSelected(final int position) {
				if (position == 0) {
					fabAdd.show();
					mBeaconsFragment.onFragmentResumed();
				} else {
					fabAdd.hide();
					if (mBeaconsFragment != null)
						mBeaconsFragment.onFragmentPaused();
				}
			}

			@Override
			public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
				// empty
			}

			@Override
			public void onPageScrollStateChanged(int state) {
				if (state != ViewPager.SCROLL_STATE_IDLE)
					fabAdd.hide();
				else if (pager.getCurrentItem() == 0)
					fabAdd.show();
			}
		});

		if (savedInstanceState == null) {
			if (getIntent().getBooleanExtra(EXTRA_OPEN_DFU, false))
				pager.setCurrentItem(2 /* DFU */);
		}
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		return false;
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!isBleEnabled())
			enableBle();

		// we are in main fragment, show 'home up' if entered from Launcher (splash screen activity)
		final boolean openedFromLauncher = getIntent().getBooleanExtra(MainActivity.OPENED_FROM_LAUNCHER, false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(!openedFromLauncher);
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if (resultCode == RESULT_OK) {
				// empty?
			} else
				finish();
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	public void setBeaconsFragment(final BeaconsFragment fragment) {
		if (fragment == null && mBeaconsFragment != null)
			mBeaconsFragment.onFragmentPaused();
		if (fragment != null)
			fragment.onFragmentResumed();
		mBeaconsFragment = fragment;
	}

	/**
	 * Checks whether the device supports Bluetooth Low Energy communication
	 * 
	 * @return <code>true</code> if BLE is supported, <code>false</code> otherwise
	 */
	private boolean ensureBleExists() {
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}

	/**
	 * Checks whether the Bluetooth adapter is enabled.
	 */
	private boolean isBleEnabled() {
		final BluetoothManager bm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		final BluetoothAdapter ba = bm.getAdapter();
		return ba != null && ba.isEnabled();
	}

	/**
	 * Tries to start Bluetooth adapter.
	 */
	private void enableBle() {
		final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	}

	private class FragmentAdapter extends FragmentPagerAdapter {

		public FragmentAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				return new BeaconsFragment();
			case 1:
				return new UpdateFragment();
			default:
			case 2:
				return new DfuFragment();
			}
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return getResources().getStringArray(R.array.tab_title)[position];
		}

	}
}
