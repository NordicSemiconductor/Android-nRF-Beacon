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

import no.nordicsemi.android.beacon.Beacon;
import no.nordicsemi.android.beacon.BeaconRegion;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper {
	/** Database version */
	private static final int DATABASE_VERSION = 2;

	/** Database file name */
	private static final String DATABASE_NAME = "beacons.db";

	private interface Tables {
		/** Sensors table. See {@link BeaconContract.Beacon} for column names */
		public static final String REGIONS = "beacons";
	}

	private static final String ID_SELECTION = BeaconContract.Beacon._ID + "=?";
	private static final String BEACON_PARAMS_SELECTION = BeaconContract.Beacon.UUID + "=? AND " + BeaconContract.Beacon.MAJOR + "=? AND " + BeaconContract.Beacon.MINOR + "=?";
	private static final String[] BEACON_PROJECTION = new String[] { BeaconContract.Beacon._ID, BeaconContract.Beacon.NAME, BeaconContract.Beacon.UUID, BeaconContract.Beacon.MAJOR,
			BeaconContract.Beacon.MINOR, BeaconContract.Beacon.SIGNAL_STRENGTH, BeaconContract.Beacon.EVENT, BeaconContract.Beacon.ACTION, BeaconContract.Beacon.ACTION_PARAM,
			BeaconContract.Beacon.ENABLED };

	private static SQLiteHelper mDatabaseHelper;
	private static SQLiteDatabase mDatabase;
	private String[] mSingleArg = new String[1];
	private String[] mParamsArg = new String[3];

	public DatabaseHelper(final Context context) {
		if (mDatabaseHelper == null) {
			mDatabaseHelper = new SQLiteHelper(context);
			mDatabase = mDatabaseHelper.getWritableDatabase();
		}
	}

	/**
	 * Adds the new beacon to the database
	 * 
	 * @param beacon
	 *            the beacon object
	 * @param name
	 *            the sensor name
	 * @param event
	 *            the event that will trigger the action
	 * @param action
	 *            the action id
	 * @param actionParam
	 *            optional action parameter, may be <code>null</code>
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	public long addRegion(final Beacon beacon, final String name, final int event, final int action, final String actionParam) {
		final ContentValues values = new ContentValues();
		values.put(BeaconContract.Beacon.NAME, name);
		values.put(BeaconContract.Beacon.UUID, beacon.getUuid().toString());
		values.put(BeaconContract.Beacon.MAJOR, beacon.getMajor());
		values.put(BeaconContract.Beacon.MINOR, beacon.getMinor());
		values.put(BeaconContract.Beacon.EVENT, event);
		values.put(BeaconContract.Beacon.ACTION, action);
		values.put(BeaconContract.Beacon.ACTION_PARAM, actionParam);

		return mDatabase.insert(Tables.REGIONS, null, values);
	}

	/**
	 * Removes the region with given ID
	 * 
	 * @param id
	 *            the region id in the database
	 */
	public void deleteRegion(final long id) {
		mSingleArg[0] = String.valueOf(id);

		mDatabase.delete(Tables.REGIONS, ID_SELECTION, mSingleArg);
	}

	/**
	 * Sets the signal strength to 0 for all beacons. Use when exiting from application.
	 * 
	 * @return the number of rows affected
	 */
	public int resetSignalStrength() {
		final ContentValues values = new ContentValues();
		values.put(BeaconContract.Beacon.SIGNAL_STRENGTH, 0);

		return mDatabase.update(Tables.REGIONS, values, null, null);
	}

	/**
	 * Updates the signal strength of a most recent beacon in the region
	 * 
	 * @param id
	 *            the region id in the database
	 * @param accuracy
	 *            the signal strength as a accuracy (distance in meters)
	 * @return number of rows affected
	 */
	public int updateRegionSignalStrength(final long id, final int accuracy) {
		mSingleArg[0] = String.valueOf(id);

		final ContentValues values = new ContentValues();
		values.put(BeaconContract.Beacon.SIGNAL_STRENGTH, accuracy);

		return mDatabase.update(Tables.REGIONS, values, ID_SELECTION, mSingleArg);
	}

	public int updateRegionName(final long id, final String name) {
		mSingleArg[0] = String.valueOf(id);

		final ContentValues values = new ContentValues();
		values.put(BeaconContract.Beacon.NAME, name);

		return mDatabase.update(Tables.REGIONS, values, ID_SELECTION, mSingleArg);
	}

	public int updateRegionEvent(final long id, final int event) {
		mSingleArg[0] = String.valueOf(id);

		final ContentValues values = new ContentValues();
		values.put(BeaconContract.Beacon.EVENT, event);

		return mDatabase.update(Tables.REGIONS, values, ID_SELECTION, mSingleArg);
	}

	public int updateRegionAction(final long id, final int action) {
		mSingleArg[0] = String.valueOf(id);

		final ContentValues values = new ContentValues();
		values.put(BeaconContract.Beacon.ACTION, action);

		return mDatabase.update(Tables.REGIONS, values, ID_SELECTION, mSingleArg);
	}

	public int updateRegionActionParam(final long id, final String param) {
		mSingleArg[0] = String.valueOf(id);

		final ContentValues values = new ContentValues();
		values.put(BeaconContract.Beacon.ACTION_PARAM, param);

		return mDatabase.update(Tables.REGIONS, values, ID_SELECTION, mSingleArg);
	}

	public int setRegionEnabled(final long id, final boolean enabled) {
		mSingleArg[0] = String.valueOf(id);

		final ContentValues values = new ContentValues();
		values.put(BeaconContract.Beacon.ENABLED, enabled ? 1 : 0);

		return mDatabase.update(Tables.REGIONS, values, ID_SELECTION, mSingleArg);
	}

	/**
	 * Searches for a region by matching beacon in the database.
	 * 
	 * @param beacon
	 *            the beacon
	 * @return cursor with the result
	 */
	public Cursor findRegionByBeacon(final Beacon beacon) {
		mParamsArg[0] = beacon.getUuid().toString();
		mParamsArg[1] = String.valueOf(beacon.getMajor());
		mParamsArg[2] = String.valueOf(beacon.getMinor());

		return mDatabase.query(Tables.REGIONS, BEACON_PROJECTION, BEACON_PARAMS_SELECTION, mParamsArg, null, null, null);
	}

	/**
	 * Searches for a region in the database.
	 * 
	 * @param region
	 *            the region to find
	 * @return cursor with the result
	 */
	public Cursor findRegion(final BeaconRegion region) {
		mParamsArg[0] = region.getUuid().toString();
		mParamsArg[1] = String.valueOf(region.getMajor());
		mParamsArg[2] = String.valueOf(region.getMinor());

		return mDatabase.query(Tables.REGIONS, BEACON_PROJECTION, BEACON_PARAMS_SELECTION, mParamsArg, null, null, null);
	}

	/**
	 * Returns the data of a region with specified id.
	 * 
	 * @param id
	 *            the region id
	 * @return cursor with the result
	 */
	public Cursor getRegion(final long id) {
		mSingleArg[0] = String.valueOf(id);

		return mDatabase.query(Tables.REGIONS, BEACON_PROJECTION, ID_SELECTION, mSingleArg, null, null, null);
	}

	public Cursor getAllRegions() {
		return mDatabase.query(Tables.REGIONS, BEACON_PROJECTION, null, null, null, null, null);
	}

	private class SQLiteHelper extends SQLiteOpenHelper {
		/**
		 * The SQL code that creates the beacons table:
		 * 
		 * <pre>
		 * --------------------------------------------------------------------------------------------------------------------------------------------------------------------
		 * |                                                              regions                                                                                             |
		 * --------------------------------------------------------------------------------------------------------------------------------------------------------------------
		 * | _id (int, pk, auto increment) | name (text) | uuid (text) | major (int) | minor (int) | signal_strength (int) | event (int) | action (int) | action_param (text) |
		 * --------------------------------------------------------------------------------------------------------------------------------------------------------------------
		 * </pre>
		 */
		private static final String CREATE_BEACONS = "CREATE TABLE " + Tables.REGIONS + "(" + BeaconContract.Beacon._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + BeaconContract.Beacon.NAME
				+ " TEXT NOT NULL, " + BeaconContract.Beacon.UUID + " TEXT NOT NULL, " + BeaconContract.Beacon.MAJOR + " INTEGER NOT NULL, " + BeaconContract.Beacon.MINOR + " INTEGER NOT NULL, "
				+ BeaconContract.Beacon.SIGNAL_STRENGTH + " INTEGER, " + BeaconContract.Beacon.EVENT + " INTEGER NOT NULL, " + BeaconContract.Beacon.ACTION + " INTEGER NOT NULL, "
				+ BeaconContract.Beacon.ACTION_PARAM + " TEXT, " + BeaconContract.Beacon.ENABLED + " INTEGER NOT NULL DEFAULT(1));";

		public SQLiteHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			db.execSQL(CREATE_BEACONS);
		}

		private static final String ALTER_REGIONS_ADD_ENABLED = "ALTER TABLE " + Tables.REGIONS + " ADD COLUMN " + BeaconContract.Beacon.ENABLED + " INTEGER NOT NULL DEFAULT(1)";

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
			switch (oldVersion) {
			case 1:
				db.execSQL(ALTER_REGIONS_ADD_ENABLED);
				break;
			}
			//			db.execSQL("DROP TABLE IF EXISTS " + Tables.REGIONS);
			//			onCreate(db);
		}

	}
}
