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

package no.nordicsemi.android.nrfbeacon.common;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import no.nordicsemi.android.nrfbeacon.R;

public abstract class BaseFragment extends Fragment implements PermissionRationaleDialogFragment.PermissionDialogListener {
	private static final int REQUEST_PERMISSION_REQ_CODE = 76; // any 8-bit number

	@Override
	public void onRequestPermissionsResult(final int requestCode, final @NonNull String[] permissions, final @NonNull int[] grantResults) {
		switch (requestCode) {
			case REQUEST_PERMISSION_REQ_CODE: {
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					// We have been granted the Manifest.permission.ACCESS_COARSE_LOCATION permission. Now we may proceed with scanning.
					onPermissionGranted();
				} else {
					Toast.makeText(getActivity(), R.string.rationale_permission_denied, Toast.LENGTH_SHORT).show();
				}
				break;
			}
		}
	}

	@Override
	public void onRequestPermission() {
		requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION_REQ_CODE);
	}

	/**
	 * Method called when user has granted the coarse location permission to the application.
	 */
	protected abstract void onPermissionGranted();

	/**
	 * Ensures the required permission has been granted by the user. On Android pre-6.0 it always returns true.
	 * In case the permission has not been granted this method may also show a dialog with rationale.
	 * @return true if the permission required for BLE scanning is granted
	 */
	protected boolean ensurePermission() {
		// Since Android 6.0 we need to obtain either Manifest.permission.ACCESS_COARSE_LOCATION or Manifest.permission.ACCESS_FINE_LOCATION to be able to scan for
		// Bluetooth LE devices. This is related to beacons as proximity devices.
		// On API older than Marshmallow the following code does nothing.
		if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			// When user pressed Deny and still wants to use this functionality, show the rationale
			if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
				final DialogFragment dialog = new PermissionRationaleDialogFragment();
				dialog.show(getChildFragmentManager(), null);
				return false;
			}
			onRequestPermission();
			return false;
		}
		return true;
	}
}
