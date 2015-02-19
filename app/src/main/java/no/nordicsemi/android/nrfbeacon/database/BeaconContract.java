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
package no.nordicsemi.android.nrfbeacon.database;

import android.provider.BaseColumns;

public class BeaconContract {
	public final static int EVENT_OUT_OF_RANGE = 0;
	public final static int EVENT_IN_RANGE = 1;
	public final static int EVENT_GET_NEAR = 2;
	public final static int EVENT_ON_TOUCH = 3;

	public final static int ACTION_MONA_LISA = 0;
	public final static int ACTION_URL = 1;
	public final static int ACTION_APP = 2;
	public final static int ACTION_ALARM = 3;
	public final static int ACTION_SILENT = 4;
	public final static int ACTION_TASKER = 5;

	protected interface BeaconColumns {
		/** The user defined sensor name */
		public final static String NAME = "name";
		/** The beacon service uuid */
		public final static String UUID = "uuid";
		/** The beacon major number */
		public final static String MAJOR = "major";
		/** The beacon minor */
		public final static String MINOR = "minor";
		/** The last signal strength in percentage */
		public final static String SIGNAL_STRENGTH = "signal_strength";
		/** The event that triggers the action */
		public final static String EVENT = "event";
		/** The action assigned to the beacon */
		public final static String ACTION = "action";
		/** The optional parameter for the action (URL, application package, etc) */
		public final static String ACTION_PARAM = "action_param";
		/** 1 if beacon notifications are enabled, 0 if disabled */
		public final static String ENABLED = "enabled";
	}

	public final class Beacon implements BaseColumns, BeaconColumns {
		private Beacon() {
			// empty
		}
	}
}
