/*
Copyright (c) 2018 Tim Chen

This file is part of KaraokeRemote.

This file may be used under the terms of the MIT license.

This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Public License along with KaraokeRemote.
*/
package com.telegraphmedia.karaokeremote;

import java.util.ArrayList;

import org.teleal.cling.model.types.UDN;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class KaraokeServerSearchDialog extends DialogFragment {
	private ArrayList<UpnpDevice> mDevices = new ArrayList<UpnpDevice>();
	private ArrayAdapter<UpnpDevice> mAdapter = null;

	private onButtonClickListener mCallback;

	// Interface definition
	public interface onButtonClickListener {
		public void onDeviceClicked (UpnpDevice device);
		public void onRescanClicked ();
	}

	private class DisabledArrayAdapter extends ArrayAdapter<String> {

		public DisabledArrayAdapter (Context context, int layoutResourceId, String[] data) {
			super(context, layoutResourceId, data);
		}

		@Override
		public boolean isEnabled (int position) {
			return false;
		}
	}

	public void stopSearch() {
		AlertDialog myDialog = (AlertDialog) getDialog();

		if (myDialog != null) {
			//Hide the progress bar
			View progressBar = getDialog().findViewById(R.id.progressBar);
			progressBar.setVisibility(View.INVISIBLE);

			// If there are no results, display a no results message
			ListView lv = (ListView) myDialog.findViewById(R.id.deviceList);
			if (mDevices.size() == 0) {
				String[] noServersFound = {getString (R.string.no_servers_found)};

				DisabledArrayAdapter disabledArrayAdapter = new DisabledArrayAdapter(getActivity(),
						android.R.layout.simple_list_item_1, noServersFound);
				lv.setAdapter(disabledArrayAdapter);
			} else {
				getDialog().setTitle(getString(R.string.select_server));
			}

			// Enable the "Rescan" button
			Button myButton = myDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
			if (myButton != null) {
				myButton.setEnabled(true);
			}			
		}

	}

	public UpnpDevice getDevice (long pos) {
		return mDevices.get((int) pos);
	}

	public void addDevice (String friendlyName, UDN udn) {
		UpnpDevice myDevice = new UpnpDevice (friendlyName, udn);
		mDevices.add (myDevice);

		if (getActivity() != null) {
			getActivity().runOnUiThread (new Runnable() {
				public void run() {
					if (mAdapter != null) {
						mAdapter.notifyDataSetChanged();
					}
				}
			});
		}
	}

	@Override
	public void onAttach (Activity activity) {
		super.onAttach(activity);

		// Set mCallback to the activity. This will throw an exception
		// if Activity does not implement onButtonClickListener.
		try {
			mCallback = (onButtonClickListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnButtonClickListener");
		}
	}

	@Override
	public AlertDialog onCreateDialog(Bundle savedInstanceState) {
		mAdapter = new ArrayAdapter<UpnpDevice>(getActivity(),
				android.R.layout.simple_list_item_1, mDevices);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		// Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();

		// Inflate and set the layout for the dialog
		// Pass null as the parent view because it's going in the dialog layout
		builder.setView(inflater.inflate(R.layout.karaoke_server_search_dialog_layout, null))
		.setTitle(R.string.searching_for_karaoke_servers)

		.setNeutralButton(R.string.rescan, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				// Call unScanClicked in MainActivity
				mCallback.onRescanClicked();
			}
		});

		AlertDialog myDialog =  builder.create();

		myDialog.setOnShowListener(new OnShowListener () {

			@Override
			public void onShow(DialogInterface dialog) {
				// Make the dialog modal
				setCancelable (false);

				Button myButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
				myButton.setEnabled(false);
				myButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
				myButton.setEnabled(false);

				// Grab the deviceList ListView
				ListView lv = (ListView) ((AlertDialog) dialog).findViewById(R.id.deviceList);
				lv.setAdapter(mAdapter);

				final AlertDialog myDialog = (AlertDialog) dialog;
				lv.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {

						UpnpDevice connectToDevice = mDevices.get((int) id);
						// Call onConnectClicked in MainAcitvity
						mCallback.onDeviceClicked(connectToDevice);

						myDialog.dismiss();
					}
				});
			}

		});

		return myDialog;
	}

}
