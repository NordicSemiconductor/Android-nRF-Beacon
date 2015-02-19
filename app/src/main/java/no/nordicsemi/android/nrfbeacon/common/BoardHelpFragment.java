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

import no.nordicsemi.android.nrfbeacon.R;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class BoardHelpFragment extends DialogFragment {
	public static final int MODE_DFU = 1;
	public static final int MODE_UPDATE = 2;

	private static final String MODE = "mode";

	public static BoardHelpFragment getInstance(final int mode) {
		final BoardHelpFragment fragment = new BoardHelpFragment();

		final Bundle args = new Bundle();
		args.putInt(MODE, mode);
		fragment.setArguments(args);

		return fragment;
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final Bundle bundle = getArguments();
		final int mode = bundle.getInt(MODE);

		getDialog().setTitle(R.string.update_about_title);
		final TextView aboutView = (TextView) inflater.inflate(R.layout.fragment_dialog_help, container, false);
		switch (mode) {
		case MODE_DFU:
			aboutView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.beacon_sw1, 0, 0, 0);
			aboutView.setText(R.string.update_about_message_dfu);
			break;
		case MODE_UPDATE:
		default:
			aboutView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.beacon_sw2, 0, 0, 0);
			aboutView.setText(R.string.update_about_message_update);
			break;
		}
		return aboutView;
	}
}
