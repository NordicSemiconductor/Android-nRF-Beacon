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
package no.nordicsemi.android.nrfbeacon.update;

import java.util.UUID;

import no.nordicsemi.android.nrfbeacon.R;
import no.nordicsemi.android.nrfbeacon.UpdateService;
import no.nordicsemi.android.nrfbeacon.common.BoardHelpFragment;
import no.nordicsemi.android.nrfbeacon.scanner.ScannerFragment;
import no.nordicsemi.android.nrfbeacon.scanner.ScannerFragmentListener;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.SwitchCompat;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

public class UpdateFragment extends Fragment implements ScannerFragmentListener {
	/** The UUID of a service in the beacon advertising packet when in Config mode. This may be <code>null</code> if no filter required. */
	private static final UUID BEACON_CONFIG_ADV_UUID = UUID.fromString("955A1523-0FE2-F5AA-0A094-84B8D4F3E8AD");

	private View mUuidContainer;
	private View mUuidTitleView;
	private TextView mUuidView;
	private View mMajorMinorContainer;
	private View mMajorTitleView;
	private TextView mMajorView;
	private View mMinorTitleView;
	private TextView mMinorView;
	private View mCalibratedRssiContainer;
	private View mCalibratedRssiTitleView;
	private TextView mCalibratedRssiView;
	private View mCalibratedRssiUnitView;
	// Advanced options
	private View mAdvancedTitleView;
	private View mManufacturerIdContainer;
	private View mManufacturerIdTitleView;
	private TextView mManufacturerIdView;
	private View mAdvIntervalContainer;
	private View mAdvIntervalTitleView;
	private TextView mAdvIntervalView;
	private View mAdvIntervalUnitView;
	private SwitchCompat mLedsSwitch;
	private boolean mLedSwitchActionDisabled;
	private Button mConnectButton;

	private UpdateService.ServiceBinder mBinder;
	private boolean mBinded;

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(final ComponentName name, final IBinder service) {
			final UpdateService.ServiceBinder binder = mBinder = (UpdateService.ServiceBinder) service;
			final int state = binder.getState();
			switch (state) {
			case UpdateService.STATE_DISCONNECTED:
				binder.connect();
				break;
			case UpdateService.STATE_CONNECTED:
				mConnectButton.setText(R.string.action_disconnect);
				final UUID uuid = binder.getBeaconUuid();
				final Pair<Integer, Integer> majorAndMinor = binder.getMajorAndMinor();
				final Integer rssi = binder.getCalibratedRssi();
				final boolean advancedSupported = binder.isAdvancedSupported();
				final Integer manufacturerId = binder.getManufacturerId();
				final Integer advInterval = binder.getAdvInterval();
				final Boolean ledOn = binder.getLedStatus();

				if (uuid != null) {
					mUuidView.setText(uuid.toString());
					setUuidControlsEnabled(true);
				}
				if (majorAndMinor != null) {
					mMajorView.setText(String.valueOf(majorAndMinor.first));
					mMinorView.setText(String.valueOf(majorAndMinor.second));
					setMajorMinorControlsEnabled(true);
				}
				if (rssi != null) {
					mCalibratedRssiView.setTag(rssi);
					mCalibratedRssiView.setText(String.valueOf(rssi));
					setRssiControlsEnabled(true);
				}
				mAdvancedTitleView.setEnabled(advancedSupported);
				if (manufacturerId != null) {
					mManufacturerIdView.setText(String.valueOf(manufacturerId));
					setManufacturerIdControlsEnabled(true);
				}
				if (advInterval != null) {
					mAdvIntervalView.setText(String.valueOf(advInterval));
					setAdvIntervalControlsEnabled(true);
				}
				if (ledOn != null) {
					mLedSwitchActionDisabled = true;
					mLedsSwitch.setChecked(ledOn);
					mLedSwitchActionDisabled = false;
					setLedControlsEnabled(true);
				}
				break;
			}
		}

		@Override
		public void onServiceDisconnected(final ComponentName name) {
			mBinder = null;
		}
	};

	private BroadcastReceiver mServiceBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final Activity activity = getActivity();
			if (activity == null || !isResumed())
				return;

			final String action = intent.getAction();

			if (UpdateService.ACTION_STATE_CHANGED.equals(action)) {
				final int state = intent.getIntExtra(UpdateService.EXTRA_DATA, UpdateService.STATE_DISCONNECTED);

				switch (state) {
				case UpdateService.STATE_DISCONNECTED:
					mConnectButton.setText(R.string.action_connect);
					mConnectButton.setEnabled(true);
					setUuidControlsEnabled(false);
					setMajorMinorControlsEnabled(false);
					setRssiControlsEnabled(false);
					setManufacturerIdControlsEnabled(false);
					setAdvIntervalControlsEnabled(false);
					setLedControlsEnabled(false);
					mAdvancedTitleView.setEnabled(true);

					final Intent service = new Intent(activity, UpdateService.class);
					activity.unbindService(mServiceConnection);
					activity.stopService(service);
					mBinder = null;
					mBinded = false;
					break;
				case UpdateService.STATE_CONNECTED:
					mConnectButton.setText(R.string.action_disconnect);
					mConnectButton.setEnabled(true);
					break;
				case UpdateService.STATE_DISCONNECTING:
				case UpdateService.STATE_CONNECTING:
					mConnectButton.setEnabled(false);
					break;
				}
			} else if (UpdateService.ACTION_UUID_READY.equals(action)) {
				final UUID uuid = ((ParcelUuid) intent.getParcelableExtra(UpdateService.EXTRA_DATA)).getUuid();
				mUuidView.setText(uuid.toString());
				setUuidControlsEnabled(true);
			} else if (UpdateService.ACTION_MAJOR_MINOR_READY.equals(action)) {
				final int major = intent.getIntExtra(UpdateService.EXTRA_MAJOR, 0);
				final int minor = intent.getIntExtra(UpdateService.EXTRA_MINOR, 0);
				mMajorView.setText(String.valueOf(major));
				mMinorView.setText(String.valueOf(minor));
				setMajorMinorControlsEnabled(true);
			} else if (UpdateService.ACTION_RSSI_READY.equals(action)) {
				final int rssi = intent.getIntExtra(UpdateService.EXTRA_DATA, 0);
				mCalibratedRssiView.setTag(rssi);
				mCalibratedRssiView.setText(String.valueOf(rssi));
				setRssiControlsEnabled(true);
			} else if (UpdateService.ACTION_MANUFACTURER_ID_READY.equals(action)) {
				final int manufacturerId = intent.getIntExtra(UpdateService.EXTRA_DATA, 0);
				mManufacturerIdView.setText(String.valueOf(manufacturerId));
				setManufacturerIdControlsEnabled(true);
			} else if (UpdateService.ACTION_ADV_INTERVAL_READY.equals(action)) {
				final int interval = intent.getIntExtra(UpdateService.EXTRA_DATA, 0);
				mAdvIntervalView.setText(String.valueOf(interval));
				setAdvIntervalControlsEnabled(true);
			} else if (UpdateService.ACTION_LED_STATUS_READY.equals(action)) {
				final boolean on = intent.getBooleanExtra(UpdateService.EXTRA_DATA, false);
				mLedSwitchActionDisabled = true;
				mLedsSwitch.setChecked(on);
				mLedSwitchActionDisabled = false;
				setLedControlsEnabled(true);
			} else if (UpdateService.ACTION_DONE.equals(action)) {
				final boolean advanced = intent.getBooleanExtra(UpdateService.EXTRA_DATA, false);
				mAdvancedTitleView.setEnabled(advanced);
				mBinder.read();
			} else if (UpdateService.ACTION_GATT_ERROR.equals(action)) {
				final int error = intent.getIntExtra(UpdateService.EXTRA_DATA, 0);

				switch (error) {
				case UpdateService.ERROR_UNSUPPORTED_DEVICE:
					Toast.makeText(activity, R.string.update_error_device_not_supported, Toast.LENGTH_SHORT).show();
					break;
				default:
					Toast.makeText(activity, getString(R.string.update_error_other, error), Toast.LENGTH_SHORT).show();
					break;
				}
				mBinder.disconnectAndClose();
			}
		}
	};

	@Override
	public void onStart() {
		super.onStart();

		// This will connect to the service only if it's already running
		final Activity activity = getActivity();
		final Intent service = new Intent(activity, UpdateService.class);
		mBinded = activity.bindService(service, mServiceConnection, 0);
	}

	@Override
	public void onStop() {
		super.onStop();

		if (mBinded)
			getActivity().unbindService(mServiceConnection);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (getActivity().isFinishing()) {
			final Activity activity = getActivity();
			final Intent service = new Intent(activity, UpdateService.class);
			activity.stopService(service);
		}
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_update, container, false);

		mUuidTitleView = view.findViewById(R.id.uuid_title);
		mUuidView = (TextView) view.findViewById(R.id.uuid);
		final View uuidContainer = mUuidContainer = view.findViewById(R.id.uuid_container);
		uuidContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				final UUID uuid = mBinder.getBeaconUuid();
				final ModifyUuidFragment fragment = ModifyUuidFragment.getInstance(uuid);
				fragment.show(getChildFragmentManager(), null);
			}
		});

		mMajorTitleView = view.findViewById(R.id.major_title);
		mMajorView = (TextView) view.findViewById(R.id.major);
		mMinorTitleView = view.findViewById(R.id.minor_title);
		mMinorView = (TextView) view.findViewById(R.id.minor);
		final View majorMinorContainer = mMajorMinorContainer = view.findViewById(R.id.major_minor_container);
		majorMinorContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				final Pair<Integer, Integer> numbers = mBinder.getMajorAndMinor();
				final ModifyMajorMinorFragment fragment = ModifyMajorMinorFragment.getInstance(numbers.first, numbers.second);
				fragment.show(getChildFragmentManager(), null);
			}
		});

		mCalibratedRssiTitleView = view.findViewById(R.id.rssi_title);
		mCalibratedRssiView = (TextView) view.findViewById(R.id.rssi);
		mCalibratedRssiUnitView = view.findViewById(R.id.rssi_unit);
		final View calibratedRssiContainer = mCalibratedRssiContainer = view.findViewById(R.id.rssi_container);
		calibratedRssiContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				final int currentRssi = mBinder.getCalibratedRssi();
				final ModifyRssiFragment fragment = ModifyRssiFragment.getInstance(currentRssi);
				fragment.show(getChildFragmentManager(), null);
			}
		});
		setUuidControlsEnabled(false);
		setMajorMinorControlsEnabled(false);
		setRssiControlsEnabled(false);

		mAdvancedTitleView = view.findViewById(R.id.advanced_title);
		mManufacturerIdTitleView = view.findViewById(R.id.manufacturer_id_title);
		mManufacturerIdView = (TextView) view.findViewById(R.id.manufacturer_id);
		final View manufacturerIdContainer = mManufacturerIdContainer = view.findViewById(R.id.manufacturer_id_container);
		manufacturerIdContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				final int currentManufacturerId = mBinder.getManufacturerId();
				final ModifyManufacturerIdFragment fragment = ModifyManufacturerIdFragment.getInstance(currentManufacturerId);
				fragment.show(getChildFragmentManager(), null);
			}
		});

		mAdvIntervalTitleView = view.findViewById(R.id.adv_interval_title);
		mAdvIntervalView = (TextView) view.findViewById(R.id.adv_interval);
		mAdvIntervalUnitView = view.findViewById(R.id.adv_interval_unit);
		final View advIntervalContainer = mAdvIntervalContainer = view.findViewById(R.id.adv_interval_container);
		advIntervalContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				final int currentInterval = mBinder.getAdvInterval();
				final ModifyAdvIntervalFragment fragment = ModifyAdvIntervalFragment.getInstance(currentInterval);
				fragment.show(getChildFragmentManager(), null);
			}
		});

		mLedsSwitch = (SwitchCompat) view.findViewById(R.id.leds);
		mLedsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
				if (mLedSwitchActionDisabled)
					return;

				setLedControlsEnabled(false);
				mBinder.setLedStatus(isChecked);
			}
		});
		setManufacturerIdControlsEnabled(false);
		setAdvIntervalControlsEnabled(false);
		setLedControlsEnabled(false);

		// Configure the CONNECT / DISCONNECT button
		final Button connectButton = mConnectButton = (Button) view.findViewById(R.id.action_connect);
		connectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (mBinder == null) {
					final ScannerFragment scannerFragment = ScannerFragment.getInstance(BEACON_CONFIG_ADV_UUID);
					scannerFragment.show(getChildFragmentManager(), null);
				} else
					mBinder.disconnectAndClose();
			}
		});

		return view;
	}

	@Override
	public void onViewCreated(final View view, final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setHasOptionsMenu(true);

		final IntentFilter filter = new IntentFilter();
		filter.addAction(UpdateService.ACTION_STATE_CHANGED);
		filter.addAction(UpdateService.ACTION_DONE);
		filter.addAction(UpdateService.ACTION_UUID_READY);
		filter.addAction(UpdateService.ACTION_MAJOR_MINOR_READY);
		filter.addAction(UpdateService.ACTION_RSSI_READY);
		filter.addAction(UpdateService.ACTION_MANUFACTURER_ID_READY);
		filter.addAction(UpdateService.ACTION_ADV_INTERVAL_READY);
		filter.addAction(UpdateService.ACTION_LED_STATUS_READY);
		filter.addAction(UpdateService.ACTION_GATT_ERROR);
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mServiceBroadcastReceiver, filter);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mServiceBroadcastReceiver);
	}

	@Override
	public void onDeviceSelected(final BluetoothDevice device, final String name) {
		final Activity activity = getActivity();
		final Intent service = new Intent(activity, UpdateService.class);
		service.putExtra(UpdateService.EXTRA_DATA, device);
		activity.startService(service);
		mBinded = true;
		activity.bindService(service, mServiceConnection, 0);
	}

	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		inflater.inflate(R.menu.about, menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		final int id = item.getItemId();
		switch (id) {
		case R.id.action_about:
			final BoardHelpFragment helpFragment = BoardHelpFragment.getInstance(BoardHelpFragment.MODE_UPDATE);
			helpFragment.show(getChildFragmentManager(), null);
			return true;
		}
		return false;
	}

	public void writeNewUuid(final UUID uuid) {
		setUuidControlsEnabled(false);
		mBinder.setBeaconUuid(uuid);
	}

	public void writeNewMajorMinor(final int major, final int minor) {
		setMajorMinorControlsEnabled(false);
		mBinder.setMajorAndMinor(major, minor);
	}

	public void writeNewRssi(final int rssi) {
		setRssiControlsEnabled(false);
		mBinder.setCalibratedRssi(rssi);
	}

	public void writeNewManufacturerId(final int id) {
		setManufacturerIdControlsEnabled(false);
		mBinder.setManufacturerId(id);
	}

	public void writeNewAdvInterval(final int interval) {
		setAdvIntervalControlsEnabled(false);
		mBinder.setAdvInterval(interval);
	}

	public void writeNewLedStatus(final boolean on) {
		setLedControlsEnabled(false);
		mBinder.setLedStatus(on);
	}

	private void setUuidControlsEnabled(final boolean enabled) {
		mUuidContainer.setEnabled(enabled);
		mUuidTitleView.setEnabled(enabled);
		mUuidView.setEnabled(enabled);
	}

	private void setMajorMinorControlsEnabled(final boolean enabled) {
		mMajorMinorContainer.setEnabled(enabled);
		mMajorTitleView.setEnabled(enabled);
		mMinorTitleView.setEnabled(enabled);
		mMajorView.setEnabled(enabled);
		mMinorView.setEnabled(enabled);
	}

	private void setRssiControlsEnabled(final boolean enabled) {
		mCalibratedRssiContainer.setEnabled(enabled);
		mCalibratedRssiTitleView.setEnabled(enabled);
		mCalibratedRssiView.setEnabled(enabled);
		mCalibratedRssiUnitView.setEnabled(enabled);
	}

	private void setManufacturerIdControlsEnabled(final boolean enabled) {
		mManufacturerIdContainer.setEnabled(enabled);
		mManufacturerIdTitleView.setEnabled(enabled);
		mManufacturerIdView.setEnabled(enabled);
	}

	private void setAdvIntervalControlsEnabled(final boolean enabled) {
		mAdvIntervalContainer.setEnabled(enabled);
		mAdvIntervalTitleView.setEnabled(enabled);
		mAdvIntervalView.setEnabled(enabled);
		mAdvIntervalUnitView.setEnabled(enabled);
	}

	private void setLedControlsEnabled(final boolean enabled) {
		mLedsSwitch.setEnabled(enabled);
	}
}
