/*
Copyright (c) 2018 Tim Chen

This file is part of KaraokeRemote.

This file may be used under the terms of the MIT license.

This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Public License along with KaraokeRemote.
*/
package com.telegraphmedia.karaokeremote;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;

public class CenteredProgressDialog extends DialogFragment {
	public CenteredProgressDialog () {}

	@Override
	public AlertDialog onCreateDialog(Bundle savedInstanceState) {
		Bundle bundle = this.getArguments();
		String title = bundle.getString("title");

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		// Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();

		// Inflate and set the layout for the dialog
		// Pass null as the parent view because it's going in the dialog layout
		builder.setView(inflater.inflate(R.layout.centered_progress_bar_layout, null))
		.setTitle(title);

		return builder.create();
	}
}
