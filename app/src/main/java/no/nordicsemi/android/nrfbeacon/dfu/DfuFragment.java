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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import no.nordicsemi.android.error.GattError;
import no.nordicsemi.android.nrfbeacon.R;
import no.nordicsemi.android.nrfbeacon.common.BoardHelpFragment;
import no.nordicsemi.android.nrfbeacon.dfu.adapter.FileBrowserAppsAdapter;
import no.nordicsemi.android.nrfbeacon.dfu.service.DfuService;
import no.nordicsemi.android.nrfbeacon.dfu.settings.DfuSettingsActivity;
import no.nordicsemi.android.nrfbeacon.dfu.settings.DfuSettingsFragment;
import no.nordicsemi.android.nrfbeacon.scanner.ScannerFragment;
import no.nordicsemi.android.nrfbeacon.scanner.ScannerFragmentListener;
import no.nordicsemi.android.nrfbeacon.util.DebugLogger;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class DfuFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, ScannerFragmentListener {
	private static final String TAG = "DfuFragment";

	private static final String PREFS_SAMPLES_VERSION = "no.nordicsemi.android.nrfbeacon.dfu.PREFS_SAMPLES_VERSION";
	private static final int CURRENT_SAMPLES_VERSION = 2;

	private static final String PREFS_DEVICE_NAME = "no.nordicsemi.android.nrfbeacon.dfu.PREFS_DEVICE_NAME";
	private static final String PREFS_FILE_NAME = "no.nordicsemi.android.nrfbeacon.dfu.PREFS_FILE_NAME";
	private static final String PREFS_FILE_TYPE = "no.nordicsemi.android.nrfbeacon.dfu.PREFS_FILE_TYPE";
	private static final String PREFS_FILE_SIZE = "no.nordicsemi.android.nrfbeacon.dfu.PREFS_FILE_SIZE";

	private static final String DATA_DEVICE = "device";
	private static final String DATA_FILE_TYPE = "file_type";
	private static final String DATA_FILE_TYPE_TMP = "file_type_tmp";
	private static final String DATA_FILE_PATH = "file_path";
	private static final String DATA_FILE_STREAM = "file_stream";
	private static final String DATA_INIT_FILE_PATH = "init_file_path";
	private static final String DATA_INIT_FILE_STREAM = "init_file_stream";
	private static final String DATA_STATUS = "status";

	private static final String EXTRA_URI = "uri";

	private static final int SELECT_FILE_REQ = 1;
	private static final int SELECT_INIT_FILE_REQ = 2;

	private TextView mDeviceNameView;
	private TextView mFileNameView;
	private TextView mFileTypeView;
	private TextView mFileSizeView;
	private TextView mFileStatusView;
	private TextView mUploadPercentageView;
	private TextView mUploadStatusView;
	private ProgressBar mProgressBar;

	private Button mSelectFileButton, mUploadButton, mConnectButton;

	private BluetoothDevice mSelectedDevice;
	private String mFilePath;
	private Uri mFileStreamUri;
	private String mInitFilePath;
	private Uri mInitFileStreamUri;
	private int mFileType;
	private int mFileTypeTmp; // This value is being used when user is selecting a file not to overwrite the old value (in case he/she will cancel selecting file)
	private boolean mStatusOk;

	private final BroadcastReceiver mDfuUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			// DFU is in progress or an error occurred 
			final String action = intent.getAction();

			if (DfuService.BROADCAST_PROGRESS.equals(action)) {
				final int progress = intent.getIntExtra(DfuService.EXTRA_DATA, 0);
				updateProgressBar(progress, false);
			} else if (DfuService.BROADCAST_ERROR.equals(action)) {
				final int error = intent.getIntExtra(DfuService.EXTRA_DATA, 0);
				updateProgressBar(error, true);

				// We have to wait a bit before canceling notification. This is called before DfuService creates the last notification.
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						// if this activity is still open and upload process was completed, cancel the notification
						final NotificationManager manager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
						manager.cancel(DfuService.NOTIFICATION_ID);
					}
				}, 200);
			}
		}
	};

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ensureSamplesExist();

		// Restore saved state
		mFileType = DfuService.TYPE_AUTO; // Default
		if (savedInstanceState != null) {
			mFileType = savedInstanceState.getInt(DATA_FILE_TYPE);
			mFileTypeTmp = savedInstanceState.getInt(DATA_FILE_TYPE_TMP);
			mFilePath = savedInstanceState.getString(DATA_FILE_PATH);
			mFileStreamUri = savedInstanceState.getParcelable(DATA_FILE_STREAM);
			mInitFilePath = savedInstanceState.getString(DATA_INIT_FILE_PATH);
			mInitFileStreamUri = savedInstanceState.getParcelable(DATA_INIT_FILE_STREAM);
			mSelectedDevice = savedInstanceState.getParcelable(DATA_DEVICE);
			mStatusOk = savedInstanceState.getBoolean(DATA_STATUS);
		}
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(DATA_FILE_TYPE, mFileType);
		outState.putInt(DATA_FILE_TYPE_TMP, mFileTypeTmp);
		outState.putString(DATA_FILE_PATH, mFilePath);
		outState.putParcelable(DATA_FILE_STREAM, mFileStreamUri);
		outState.putString(DATA_INIT_FILE_PATH, mInitFilePath);
		outState.putParcelable(DATA_INIT_FILE_STREAM, mInitFileStreamUri);
		outState.putParcelable(DATA_DEVICE, mSelectedDevice);
		outState.putBoolean(DATA_STATUS, mStatusOk);
	}

	@Override
	public void onResume() {
		super.onResume();

		// We are using LocalBroadcastReceiver instead of normal BroadcastReceiver for optimization purposes
		final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
		broadcastManager.registerReceiver(mDfuUpdateReceiver, makeDfuUpdateIntentFilter());
	}

	private static IntentFilter makeDfuUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(DfuService.BROADCAST_PROGRESS);
		intentFilter.addAction(DfuService.BROADCAST_ERROR);
		return intentFilter;
	}

	@Override
	public void onPause() {
		super.onPause();

		final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
		broadcastManager.unregisterReceiver(mDfuUpdateReceiver);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_dfu_upload, container, false);

		mDeviceNameView = (TextView) view.findViewById(R.id.device_name);
		mFileNameView = (TextView) view.findViewById(R.id.file_name);
		mFileTypeView = (TextView) view.findViewById(R.id.file_type);
		mFileSizeView = (TextView) view.findViewById(R.id.file_size);
		mFileStatusView = (TextView) view.findViewById(R.id.file_status);
		mUploadPercentageView = (TextView) view.findViewById(R.id.progress_percentage);
		mUploadStatusView = (TextView) view.findViewById(R.id.progress_status);
		mProgressBar = (ProgressBar) view.findViewById(R.id.progress);

		mSelectFileButton = (Button) view.findViewById(R.id.action_select_file);
		mUploadButton = (Button) view.findViewById(R.id.action_upload);
		mUploadButton.setEnabled(mSelectedDevice != null && mStatusOk);
		mConnectButton = (Button) view.findViewById(R.id.action_connect);

		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		if (isDfuServiceRunning()) {
			// Restore image file information
			mDeviceNameView.setText(preferences.getString(PREFS_DEVICE_NAME, ""));
			mFileNameView.setText(preferences.getString(PREFS_FILE_NAME, ""));
			mFileTypeView.setText(preferences.getString(PREFS_FILE_TYPE, ""));
			mFileSizeView.setText(preferences.getString(PREFS_FILE_SIZE, ""));
			mFileStatusView.setText(R.string.dfu_file_status_ok);
			mStatusOk = true;
			showProgressBar();
		}

		// Assign button actions
		mSelectFileButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				onSelectFileClicked(v);
			}
		});
		mUploadButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				onUploadClicked(v);
			}
		});
		mConnectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				onConnectClicked(v);
			}
		});

		// Configure the 'help' button
		view.findViewById(R.id.action_help).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				new AlertDialog.Builder(getActivity()).setTitle(R.string.dfu_help_title).setMessage(R.string.dfu_help_message).setPositiveButton(R.string.ok, null).show();
			}
		});
		return view;
	}

	@Override
	public void onViewCreated(final View view, final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		inflater.inflate(R.menu.settings, menu);
		inflater.inflate(R.menu.about, menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		final int id = item.getItemId();
		switch (id) {
		case R.id.action_settings:
			final Intent intent = new Intent(getActivity(), DfuSettingsActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			startActivity(intent);
			return true;
		case R.id.action_about:
			final BoardHelpFragment helpFragment = BoardHelpFragment.getInstance(BoardHelpFragment.MODE_DFU);
			helpFragment.show(getChildFragmentManager(), null);
			return true;
		}
		return false;
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (resultCode != Activity.RESULT_OK)
			return;

		switch (requestCode) {
		case SELECT_FILE_REQ: {
			// clear previous data
			mFileType = mFileTypeTmp;
			mFilePath = null;
			mFileStreamUri = null;

			// and read new one
			final Uri uri = data.getData();
			/*
			 * The URI returned from application may be in 'file' or 'content' schema.
			 * 'File' schema allows us to create a File object and read details from if directly.
			 * 
			 * Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
			 */
			if (uri.getScheme().equals("file")) {
				// the direct path to the file has been returned
				final String path = uri.getPath();
				final File file = new File(path);
				mFilePath = path;

				updateFileInfo(file.getName(), file.length(), mFileType);
			} else if (uri.getScheme().equals("content")) {
				// an Uri has been returned
				mFileStreamUri = uri;
				// if application returned Uri for streaming, let's us it. Does it works?
				// FIXME both Uris works with Google Drive app. Why both? What's the difference? How about other apps like DropBox?
				final Bundle extras = data.getExtras();
				if (extras != null && extras.containsKey(Intent.EXTRA_STREAM))
					mFileStreamUri = extras.getParcelable(Intent.EXTRA_STREAM);

				// file name and size must be obtained from Content Provider
				final Bundle bundle = new Bundle();
				bundle.putParcelable(EXTRA_URI, uri);
				getLoaderManager().restartLoader(0, bundle, this);
			}
			break;
		}
		case SELECT_INIT_FILE_REQ: {
			mInitFilePath = null;
			mInitFileStreamUri = null;

			// and read new one
			final Uri uri = data.getData();
			/*
			 * The URI returned from application may be in 'file' or 'content' schema. 'File' schema allows us to create a File object and read details from if
			 * directly. Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
			 */
			if (uri.getScheme().equals("file")) {
				// the direct path to the file has been returned
				mInitFilePath = uri.getPath();
				mFileStatusView.setText(R.string.dfu_file_status_ok_with_init);
			} else if (uri.getScheme().equals("content")) {
				// an Uri has been returned
				mInitFileStreamUri = uri;
				// if application returned Uri for streaming, let's us it. Does it works?
				// FIXME both Uris works with Google Drive app. Why both? What's the difference? How about other apps like DropBox?
				final Bundle extras = data.getExtras();
				if (extras != null && extras.containsKey(Intent.EXTRA_STREAM))
					mInitFileStreamUri = extras.getParcelable(Intent.EXTRA_STREAM);
				mFileStatusView.setText(R.string.dfu_file_status_ok_with_init);
			}
			break;
		}
		default:
			break;
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
		final Uri uri = args.getParcelable(EXTRA_URI);
		/*
		 * Some apps, f.e. Google Drive allow to select file that is not on the device. There is no "_data" column handled by that provider. Let's try to obtain all columns and than check
		 * which columns are present.
		 */
		//final String[] projection = new String[] { MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATA };
		return new CursorLoader(getActivity(), uri, null /*all columns, instead of projection*/, null, null, null);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
		mFileNameView.setText(null);
		mFileTypeView.setText(null);
		mFileSizeView.setText(null);
		mFilePath = null;
		mFileStreamUri = null;
		mStatusOk = false;
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
		if (data != null && data.moveToNext()) {
			/*
			 * Here we have to check the column indexes by name as we have requested for all. The order may be different.
			 */
			final String fileName = data.getString(data.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)/* 0 DISPLAY_NAME */);
			final int fileSize = data.getInt(data.getColumnIndex(MediaStore.MediaColumns.SIZE) /* 1 SIZE */);
			String filePath = null;
			final int dataIndex = data.getColumnIndex(MediaStore.MediaColumns.DATA);
			if (dataIndex != -1)
				filePath = data.getString(dataIndex /*2 DATA */);
			if (!TextUtils.isEmpty(filePath))
				mFilePath = filePath;

			updateFileInfo(fileName, fileSize, mFileType);
		} else {
			mFileNameView.setText(null);
			mFileTypeView.setText(null);
			mFileSizeView.setText(null);
			mFilePath = null;
			mFileStreamUri = null;
			mFileStatusView.setText(R.string.dfu_file_status_error);
			mStatusOk = false;
		}
	}

	/**
	 * Updates the file information on UI
	 * 
	 * @param fileName
	 *            file name
	 * @param fileSize
	 *            file length
	 */
	private void updateFileInfo(final String fileName, final long fileSize, final int fileType) {
		mFileNameView.setText(fileName);
		switch (fileType) {
			case DfuService.TYPE_AUTO:
				mFileTypeView.setText(getResources().getStringArray(R.array.dfu_file_type)[0]);
				break;
			case DfuService.TYPE_SOFT_DEVICE:
				mFileTypeView.setText(getResources().getStringArray(R.array.dfu_file_type)[1]);
				break;
			case DfuService.TYPE_BOOTLOADER:
				mFileTypeView.setText(getResources().getStringArray(R.array.dfu_file_type)[2]);
				break;
			case DfuService.TYPE_APPLICATION:
				mFileTypeView.setText(getResources().getStringArray(R.array.dfu_file_type)[3]);
				break;
		}
		mFileSizeView.setText(getString(R.string.dfu_file_size_text, fileSize));
		final String extension = mFileType == DfuService.TYPE_AUTO ? "(?i)ZIP" : "(?i)HEX|BIN"; // (?i) =  case insensitive
		final boolean statusOk = mStatusOk = MimeTypeMap.getFileExtensionFromUrl(fileName).matches(extension);
		mFileStatusView.setText(statusOk ? R.string.dfu_file_status_ok : R.string.dfu_file_status_invalid);
		mUploadButton.setEnabled(mSelectedDevice != null && statusOk);

		// Ask the user for the Init packet file if HEX or BIN files are selected. In case of a ZIP file the Init packets should be included in the ZIP.
		if (statusOk && fileType != DfuService.TYPE_AUTO) {
			new AlertDialog.Builder(getActivity()).setTitle(R.string.dfu_file_init_title).setMessage(R.string.dfu_file_init_message)
					.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							mInitFilePath = null;
							mInitFileStreamUri = null;
						}
					}).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
							intent.setType(DfuService.MIME_TYPE_OCTET_STREAM);
							intent.addCategory(Intent.CATEGORY_OPENABLE);
							startActivityForResult(intent, SELECT_INIT_FILE_REQ);
						}
					}).show();
		}
	}

	@Override
	public void onDeviceSelected(final BluetoothDevice device, final String name) {
		mSelectedDevice = device;
		mUploadButton.setEnabled(mStatusOk);
		mDeviceNameView.setText(name);
	}

	/**
	 * Called when Select File was pressed
	 * 
	 * @param view
	 *            a button that was pressed
	 */
	private void onSelectFileClicked(final View view) {
		mFileTypeTmp = mFileType;
		int index = 0;
		switch (mFileType) {
			case DfuService.TYPE_AUTO:
				index = 0;
				break;
			case DfuService.TYPE_SOFT_DEVICE:
				index = 1;
				break;
			case DfuService.TYPE_BOOTLOADER:
				index = 2;
				break;
			case DfuService.TYPE_APPLICATION:
				index = 3;
				break;
		}
		// Show a dialog with file types
		new AlertDialog.Builder(getActivity()).setTitle(R.string.dfu_file_type_title)
				.setSingleChoiceItems(R.array.dfu_file_type, index, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						switch (which) {
							case 0:
								mFileTypeTmp = DfuService.TYPE_AUTO;
								break;
							case 1:
								mFileTypeTmp = DfuService.TYPE_SOFT_DEVICE;
								break;
							case 2:
								mFileTypeTmp = DfuService.TYPE_BOOTLOADER;
								break;
							case 3:
								mFileTypeTmp = DfuService.TYPE_APPLICATION;
								break;
						}
					}
				}).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						openFileChooser();
					}
				}).setNeutralButton(R.string.dfu_file_info, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						final DfuZipInfoFragment fragment = new DfuZipInfoFragment();
						fragment.show(getFragmentManager(), "help_fragment");
					}
				}).setNegativeButton(R.string.cancel, null).show();
	}

	private void openFileChooser() {
		final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType(mFileTypeTmp == DfuService.TYPE_AUTO ? DfuService.MIME_TYPE_ZIP : DfuService.MIME_TYPE_OCTET_STREAM);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
			// file browser has been found on the device
			startActivityForResult(intent, SELECT_FILE_REQ);
		} else {
			// there is no any file browser app, let's try to download one
			final View customView = getActivity().getLayoutInflater().inflate(R.layout.app_file_browser, null);
			final ListView appsList = (ListView) customView.findViewById(android.R.id.list);
			appsList.setAdapter(new FileBrowserAppsAdapter(getActivity()));
			appsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			appsList.setItemChecked(0, true);
			new AlertDialog.Builder(getActivity()).setTitle(R.string.dfu_alert_no_filebrowser_title).setView(customView)
					.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							dialog.dismiss();
						}
					}).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							final int pos = appsList.getCheckedItemPosition();
							if (pos >= 0) {
								final String query = getResources().getStringArray(R.array.dfu_app_file_browser_action)[pos];
								final Intent storeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(query));
								startActivity(storeIntent);
							}
						}
					}).show();
		}
	}

	/**
	 * Callback of UPDATE/CANCEL button on DfuActivity
	 */
	private void onUploadClicked(final View view) {
		if (isDfuServiceRunning()) {
			showUploadCancelDialog();
			return;
		}

		// check whether the selected file is a HEX file (we are just checking the extension)
		if (!mStatusOk) {
			Toast.makeText(getActivity(), R.string.dfu_file_status_invalid_message, Toast.LENGTH_LONG).show();
			return;
		}

		// Save current state in order to restore it if user quit the Activity
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		final SharedPreferences.Editor editor = preferences.edit();
		editor.putString(PREFS_DEVICE_NAME, mSelectedDevice.getName());
		editor.putString(PREFS_FILE_NAME, mFileNameView.getText().toString());
		editor.putString(PREFS_FILE_TYPE, mFileTypeView.getText().toString());
		editor.putString(PREFS_FILE_SIZE, mFileSizeView.getText().toString());
		editor.apply();

		showProgressBar();

		final boolean keepBond = preferences.getBoolean(DfuSettingsFragment.SETTINGS_KEEP_BOND, false);

		final Intent service = new Intent(getActivity(), DfuService.class);
		service.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, mSelectedDevice.getAddress());
		service.putExtra(DfuService.EXTRA_DEVICE_NAME, mSelectedDevice.getName());
		service.putExtra(DfuService.EXTRA_FILE_MIME_TYPE, mFileType == DfuService.TYPE_AUTO ? DfuService.MIME_TYPE_ZIP : DfuService.MIME_TYPE_OCTET_STREAM);
		service.putExtra(DfuService.EXTRA_FILE_TYPE, mFileType);
		service.putExtra(DfuService.EXTRA_FILE_PATH, mFilePath);
		service.putExtra(DfuService.EXTRA_FILE_URI, mFileStreamUri);
		service.putExtra(DfuService.EXTRA_INIT_FILE_PATH, mInitFilePath);
		service.putExtra(DfuService.EXTRA_INIT_FILE_URI, mInitFileStreamUri);
		service.putExtra(DfuService.EXTRA_KEEP_BOND, keepBond);
		getActivity().startService(service);
	}

	/**
	 * Called when SELECT DEVICE button has been clicked.
	 * 
	 * @param view
	 *            the view that was clicked
	 */
	private void onConnectClicked(final View view) {
		showDeviceScanningDialog();
	}

	private void updateProgressBar(final int progress, final boolean error) {
		switch (progress) {
		case DfuService.PROGRESS_CONNECTING:
			mProgressBar.setIndeterminate(true);
			mUploadPercentageView.setText(R.string.dfu_status_connecting);
			break;
		case DfuService.PROGRESS_STARTING:
			mProgressBar.setIndeterminate(true);
			mUploadPercentageView.setText(R.string.dfu_status_starting);
			break;
		case DfuService.PROGRESS_ENABLING_DFU_MODE:
			mProgressBar.setIndeterminate(true);
			mUploadPercentageView.setText(R.string.dfu_status_switching_to_dfu);
			break;
		case DfuService.PROGRESS_VALIDATING:
			mProgressBar.setIndeterminate(true);
			mUploadPercentageView.setText(R.string.dfu_status_validating);
			break;
		case DfuService.PROGRESS_DISCONNECTING:
			mProgressBar.setIndeterminate(true);
			mUploadPercentageView.setText(R.string.dfu_status_disconnecting);
			break;
		case DfuService.PROGRESS_COMPLETED:
			mUploadPercentageView.setText(R.string.dfu_status_completed);
			// let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					showFileTransferSuccessMessage();

					// if this activity is still open and upload process was completed, cancel the notification
					final NotificationManager manager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
					manager.cancel(DfuService.NOTIFICATION_ID);
				}
			}, 200);
			break;
		case DfuService.PROGRESS_ABORTED:
			mUploadPercentageView.setText(R.string.dfu_status_aborted);
			// let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					onUploadCanceled();

					// if this activity is still open and upload process was completed, cancel the notification
					final NotificationManager manager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
					manager.cancel(DfuService.NOTIFICATION_ID);
				}
			}, 200);
			break;
		default:
			mProgressBar.setIndeterminate(false);
			if (error) {
				showErrorMessage(progress);
			} else {
				mProgressBar.setProgress(progress);
				mUploadPercentageView.setText(getString(R.string.progress, progress));
			}
			break;
		}
	}

	private void showUploadCancelDialog() {
		final DfuUploadCancelFragment fragment = DfuUploadCancelFragment.getInstance();
		fragment.show(getChildFragmentManager(), null);
	}

	private void showDeviceScanningDialog() {
		final ScannerFragment dialog = ScannerFragment.getInstance(getActivity(), null);
		dialog.show(getChildFragmentManager(), null);
	}

	private void showFileTransferSuccessMessage() {
		clearUI(true);
		showToast(R.string.dfu_success);
	}

	public void onUploadCanceled() {
		clearUI(false);
		showToast(R.string.dfu_aborted);
	}

	public void onCancelUpload() {
		mProgressBar.setIndeterminate(true);
		mUploadStatusView.setText(R.string.dfu_status_aborting);
		mUploadPercentageView.setText(null);
	}

	private void showErrorMessage(final int code) {
		clearUI(false);
		showToast("Upload failed: " + GattError.parse(code) + " (" + (code & ~(DfuService.ERROR_MASK | DfuService.ERROR_REMOTE_MASK)) + ")");
	}

	private void clearUI(final boolean clearDevice) {
		mProgressBar.setVisibility(View.INVISIBLE);
		mUploadPercentageView.setVisibility(View.INVISIBLE);
		mUploadStatusView.setVisibility(View.INVISIBLE);
		mConnectButton.setEnabled(true);
		mSelectFileButton.setEnabled(true);
		mUploadButton.setEnabled(false);
		mUploadButton.setText(R.string.dfu_action_upload);
		if (clearDevice) {
			mSelectedDevice = null;
			mDeviceNameView.setText(R.string.dfu_default_name);
		}
		// Application may have lost the right to these files if Activity was closed during upload (grant uri permission). Clear file related values.
		mFileNameView.setText(null);
		mFileTypeView.setText(null);
		mFileSizeView.setText(null);
		mFileStatusView.setText(R.string.dfu_file_status_no_file);
		mFilePath = null;
		mFileStreamUri = null;
		mInitFilePath = null;
		mInitFileStreamUri = null;
		mStatusOk = false;
	}

	private void showProgressBar() {
		mProgressBar.setVisibility(View.VISIBLE);
		mUploadPercentageView.setVisibility(View.VISIBLE);
		mUploadStatusView.setText(R.string.dfu_uploading_label);
		mUploadStatusView.setVisibility(View.VISIBLE);
		mConnectButton.setEnabled(false);
		mSelectFileButton.setEnabled(false);
		mUploadButton.setEnabled(true);
		mUploadButton.setText(R.string.dfu_action_upload_cancel);
	}

	/**
	 * Copies example HEX files to the external storage.
	 */
	private void ensureSamplesExist() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		final int version = preferences.getInt(PREFS_SAMPLES_VERSION, 0);
		if (version == CURRENT_SAMPLES_VERSION)
			return;

		final File root = new File(Environment.getExternalStorageDirectory(), "Nordic Semiconductor");
		if (!root.exists()) {
			root.mkdir();
		}
		final File board = new File(root, "Board");
		if (!board.exists()) {
			board.mkdir();
		}
		final File pca20006 = new File(board, "pca20006");
		if (!pca20006.exists()) {
			pca20006.mkdir();
		}

		// Remove old files. Those will be moved to a new folder structure
		new File(root, "ble_app_beacon.hex").delete();

		boolean oldCopied = false;
		boolean newCopied = false;
		File f = new File(pca20006, "ble_app_beacon_v1_0_1_s110_v6_0_0.hex");
		if (!f.exists()) {
			copyRawResource(R.raw.ble_app_beacon_v1_0_1_s110_v6_0_0, f);
			oldCopied = true;
		}
		f = new File(pca20006, "ble_app_beacon_v1_1_0_s110_v7_1_0.zip");
		if (!f.exists()) {
			copyRawResource(R.raw.ble_app_beacon_v1_1_0_s110_v7_1_0, f);
			newCopied = true;
		}
		if (oldCopied)
			Toast.makeText(getActivity(), R.string.dfu_example_files_created, Toast.LENGTH_SHORT).show();
		else if (newCopied)
			Toast.makeText(getActivity(), R.string.dfu_example_files_added, Toast.LENGTH_SHORT).show();

		// Save the current version
		preferences.edit().putInt(PREFS_SAMPLES_VERSION, CURRENT_SAMPLES_VERSION).apply();
	}

	/**
	 * Copies the file from res/raw with given id to given destination file. If dest does not exist it will be created.
	 * 
	 * @param rawResId
	 *            the resource id
	 * @param dest
	 *            destination file
	 */
	private void copyRawResource(final int rawResId, final File dest) {
		try {
			final InputStream is = getResources().openRawResource(rawResId);
			final FileOutputStream fos = new FileOutputStream(dest);

			final byte[] buf = new byte[1024];
			int read;
			try {
				while ((read = is.read(buf)) > 0)
					fos.write(buf, 0, read);
			} finally {
				is.close();
				fos.close();
			}
		} catch (final IOException e) {
			DebugLogger.e(TAG, "Error while copying HEX file " + e.toString());
		}
	}

	private void showToast(final int messageId) {
		Toast.makeText(getActivity(), messageId, Toast.LENGTH_SHORT).show();
	}

	private void showToast(final String message) {
		Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
	}

	private boolean isDfuServiceRunning() {
		ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (DfuService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
}
