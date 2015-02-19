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

import net.dinglisch.android.tasker.TaskerIntent;
import no.nordicsemi.android.nrfbeacon.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class DownloadTaskerFragment extends DialogFragment {
	private static final String TAG = "DownloadTaskerFragment";
	private DownloadCanceledListener mListener;

	public interface DownloadCanceledListener {
		public void onTaskerNotInstalled();
	}

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);

		try {
			mListener = (DownloadCanceledListener) activity;
		} catch (final ClassCastException e) {
			Log.e(TAG, "Parent activity must implement DownloadCancelListener interface", e);
		}
	}

	public static DownloadTaskerFragment getInstance() {
		final DownloadTaskerFragment fragment = new DownloadTaskerFragment();
		return fragment;
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		return new AlertDialog.Builder(getActivity()).setTitle(R.string.action_tasker_download).setIcon(R.drawable.ic_action_tasker).setMessage(R.string.action_tasker_message).setCancelable(false)
				.setPositiveButton(R.string.download, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						// Go to Google Play
						final Intent intent = TaskerIntent.getTaskerInstallIntent(true);
						startActivity(intent);
					}
				}).setNeutralButton(R.string.website, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Open link in the browser
						final Intent intent = TaskerIntent.getTaskerInstallIntent(false);
						startActivity(intent);
					}
				}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						mListener.onTaskerNotInstalled();
					}
				}).create();
	}

	@Override
	public void onCancel(final DialogInterface dialog) {
		mListener.onTaskerNotInstalled();
	}
}
