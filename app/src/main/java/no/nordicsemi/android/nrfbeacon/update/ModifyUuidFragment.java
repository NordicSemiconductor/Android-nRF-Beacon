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
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
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

public class ModifyUuidFragment extends DialogFragment implements OnEditorActionListener, View.OnClickListener {
	private static final String PARAM_UUID = "uuid";

	private static final String PREF_PREVIOUS_UUID = "previous_uuid";
	private EditText mUuidView;

	public static ModifyUuidFragment getInstance(final UUID uuid) {
		final ModifyUuidFragment fragment = new ModifyUuidFragment();

		final Bundle args = new Bundle();
		args.putParcelable(PARAM_UUID, new ParcelUuid(uuid));
		fragment.setArguments(args);

		return fragment;
	}

	private View.OnClickListener mCopyUuidListener = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			final CharSequence uuid = ((Button) v).getText();
			mUuidView.setText(uuid);
		}
	};

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle(R.string.update_dialog_uuid_title).setNegativeButton(R.string.cancel, null)
				.setPositiveButton(R.string.ok, null);

		final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_dialog_uuid, null);
		view.findViewById(R.id.uuid1).setOnClickListener(mCopyUuidListener);
		view.findViewById(R.id.uuid2).setOnClickListener(mCopyUuidListener);
		view.findViewById(R.id.uuid3).setOnClickListener(mCopyUuidListener);
		view.findViewById(R.id.uuid4).setOnClickListener(mCopyUuidListener);
		mUuidView = (EditText) view.findViewById(R.id.uuid);

		// Check if there is a previous UUID saved
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		final String previousUuid = preferences.getString(PREF_PREVIOUS_UUID, null);
		if (previousUuid != null) {
			view.findViewById(R.id.last_uuid_title).setVisibility(View.VISIBLE);
			final Button btn = (Button) view.findViewById(R.id.last_uuid);
			btn.setOnClickListener(mCopyUuidListener);
			btn.setText(previousUuid);
			btn.setVisibility(View.VISIBLE);
		}

		// Fill the field with the current value 
		final Bundle args = getArguments();
		final UUID uuid = ((ParcelUuid) args.getParcelable(PARAM_UUID)).getUuid();
		mUuidView.setText(uuid.toString());

		builder.setView(view);

		// The OK button must validate the entered value. We have to overwrite the default button listener.
		final AlertDialog dialog = builder.create();
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(final DialogInterface d) {
				final Button ok = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
				ok.setOnClickListener(ModifyUuidFragment.this);
			}
		});
		return dialog;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Show soft keyboard automatically
		getDialog().getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		mUuidView.setOnEditorActionListener(this);
	}

	/**
	 * Called when OK button has been pressed.
	 */
	@Override
	public void onClick(final View view) {
		final String uuidValue = mUuidView.getText().toString();
		try {
			if (TextUtils.isEmpty(uuidValue) || uuidValue.length() != 36)
				throw new Exception();
			final UUID uuid = UUID.fromString(uuidValue);

			// Save the UUID for future use
			final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
			final SharedPreferences.Editor editor = preferences.edit();
			editor.putString(PREF_PREVIOUS_UUID, uuid.toString());
			editor.apply();

			final UpdateFragment parentFragment = (UpdateFragment) getParentFragment();
			parentFragment.writeNewUuid(uuid);
			dismiss();
		} catch (final Exception e) {
			mUuidView.setError(getText(R.string.update_dialog_uuid_error));
			mUuidView.requestFocus();
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
