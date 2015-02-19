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

import no.nordicsemi.android.nrfbeacon.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class ModifyMajorMinorFragment extends DialogFragment implements OnEditorActionListener, View.OnClickListener {
	private static final String MAJOR = "major";
	private static final String MINOR = "minor";

	private EditText mMajorView;
	private EditText mMinorView;

	public static ModifyMajorMinorFragment getInstance(final int major, final int minor) {
		final ModifyMajorMinorFragment fragment = new ModifyMajorMinorFragment();

		final Bundle args = new Bundle();
		args.putInt(MAJOR, major);
		args.putInt(MINOR, minor);
		fragment.setArguments(args);

		return fragment;
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle(R.string.update_dialog_major_minor_title).setNegativeButton(R.string.cancel, null)
				.setPositiveButton(R.string.ok, null);

		final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_dialog_major_minor, null);
		mMajorView = (EditText) view.findViewById(R.id.major);
		mMinorView = (EditText) view.findViewById(R.id.minor);

		// Fill the fields with the current values
		final Bundle args = getArguments();
		final int major = args.getInt(MAJOR);
		final int minor = args.getInt(MINOR);
		mMajorView.setText(String.valueOf(major));
		mMinorView.setText(String.valueOf(minor));

		builder.setView(view);

		// The OK button must validate the entered value. We have to overwrite the default button listener.
		final AlertDialog dialog = builder.create();
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(final DialogInterface d) {
				final Button ok = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
				ok.setOnClickListener(ModifyMajorMinorFragment.this);
			}
		});
		return dialog;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Show soft keyboard automatically
		mMajorView.requestFocus();
		getDialog().getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		mMinorView.setOnEditorActionListener(this);
	}

	@Override
	/**
	 * Called when OK button has been pressed.
	 */
	public void onClick(final View view) {
		// Validate major number
		final String majorValue = mMajorView.getText().toString();
		boolean valid = true;
		int major = -1;
		if (TextUtils.isEmpty(majorValue))
			valid = false;
		try {
			major = Integer.parseInt(majorValue);
		} catch (final NumberFormatException e) {
			valid = false;
		}
		if (major < 0 || major > 0xFFFF)
			valid = false;

		if (!valid) {
			mMajorView.setError(getText(R.string.update_dialog_major_minor_error));
			mMajorView.requestFocus();
			return;
		}

		// Validate minor number
		final String minorValue = mMinorView.getText().toString();
		int minor = -1;
		if (TextUtils.isEmpty(minorValue))
			valid = false;
		try {
			minor = Integer.parseInt(minorValue);
		} catch (final NumberFormatException e) {
			valid = false;
		}
		if (minor < 0 || minor > 0xFFFF)
			valid = false;

		if (!valid) {
			mMinorView.setError(getText(R.string.update_dialog_major_minor_error));
			mMinorView.requestFocus();
		} else {
			final UpdateFragment parentFragment = (UpdateFragment) getParentFragment();
			parentFragment.writeNewMajorMinor(major, minor);
			dismiss();
		}
	}

	@Override
	public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
		if (EditorInfo.IME_ACTION_DONE == actionId) {
			// Return input text to activity
			onClick(null);
			return true;
		}
		return false;

	}

}
