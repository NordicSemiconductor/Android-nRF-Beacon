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
package no.nordicsemi.android.nrfbeacon.dfu;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;

import no.nordicsemi.android.nrfbeacon.R;
import no.nordicsemi.android.nrfbeacon.dfu.service.DfuService;

/**
 * The dialog fragment which is shows when user press the Cancel button. Two buttons may resume or abort the DFU operation.
 */
public class DfuUploadCancelFragment extends DialogFragment {

	public static DfuUploadCancelFragment getInstance() {
		return new DfuUploadCancelFragment();
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Send broadcast message to the DfuService
		final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getActivity());
		final Intent pauseAction = new Intent(DfuService.BROADCAST_ACTION);
		pauseAction.putExtra(DfuService.EXTRA_ACTION, DfuService.ACTION_PAUSE);
		manager.sendBroadcast(pauseAction);
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		return new AlertDialog.Builder(getActivity()).setTitle(R.string.dfu_confirmation_dialog_title).setMessage(R.string.dfu_upload_dialog_cancel_message).setCancelable(false)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						// Send broadcast message to the DfuService
						final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getActivity());
						final Intent pauseAction = new Intent(DfuService.BROADCAST_ACTION);
						pauseAction.putExtra(DfuService.EXTRA_ACTION, DfuService.ACTION_ABORT);
						manager.sendBroadcast(pauseAction);

						// notify parent fragment
						final DfuFragment parent = (DfuFragment) getParentFragment();
						parent.onCancelUpload();
					}
				}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.cancel();
					}
				}).create();
	}

	@Override
	public void onCancel(final DialogInterface dialog) {
		// Send broadcast message to the DfuService
		final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getActivity());
		final Intent pauseAction = new Intent(DfuService.BROADCAST_ACTION);
		pauseAction.putExtra(DfuService.EXTRA_ACTION, DfuService.ACTION_RESUME);
		manager.sendBroadcast(pauseAction);
	}
}
