/*
Copyright (c) 2018 Tim Chen

This file is part of KaraokeRemote.

This file may be used under the terms of the MIT license.

This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Public License along with KaraokeRemote.
*/
package com.telegraphmedia.karaokeremote;

import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public abstract class SelectableListFragment extends Fragment implements OnChildClickListener, OnItemClickListener {

	// Boolean used to differentiate if we are sorted by title or artist
	protected boolean isExpandableListView;
	protected Object myAdapter = null;
	protected SQLiteDatabase mDB = null;

	public interface onSongSelectListener {
		public void onAddToPlaylist(String artist, String title, String filepath);
		public void onPlayNowSelected (String artist, String title, String filepath);
		public void onPlayNextSelected (String artist, String title, String filepath);
	}

	protected onSongSelectListener mCallback;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance (true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_songlist_layout, container, false);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Set mCallback to the activity. This will throw an exception
		// if Activity does not implement onButtonClickListener.
		try {
			mCallback = (onSongSelectListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnButtonClickListener");
		}
	}

	public void confirmSelectedSong (HashMap<String, String> selectedSong) {
		final String artist = selectedSong.get("artist");
		final String title = selectedSong.get("title");
		final String filePath = selectedSong.get("filePath");

		AlertDialog.Builder builder = new AlertDialog.Builder (getActivity());
		TextView msg = new TextView(getActivity());
		msg.setText("Add  [" + artist + " - " + title + "]  to the playlist?");
		//TODO don't use literal, instead set values in res for different screen sizes
		//(e.g. ldpi, mdpi, ldpi)

		msg.setTextSize(18);
		msg.setGravity(Gravity.CENTER_HORIZONTAL);
		builder.setView(msg);

		builder.setTitle("Confirm Song Selection");
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				mCallback.onAddToPlaylist(artist, title, filePath);
			}
		});

		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Do nothing on cancel
			}
		});

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	// For "sort by artist" song click
	@Override
	public boolean onChildClick (ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedSong = 
				(HashMap<String, String>) parent.getExpandableListAdapter().getChild(groupPosition, childPosition);
		confirmSelectedSong (selectedSong);
		return true; // Click was handled
	}

	// For "sort by title" song click
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedSong = 
				(HashMap<String, String>) parent.getAdapter().getItem(position);
		confirmSelectedSong (selectedSong);
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (isExpandableListView) {
			ExpandableListView.ExpandableListContextMenuInfo info = 
					(ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
			// Only show context menu if what was long pressed was a child and not a group
			// (i.e. the child position != -1)
			if (ExpandableListView.getPackedPositionChild (info.packedPosition) != -1) {
				MenuInflater inflater = getActivity().getMenuInflater();
				inflater.inflate(R.menu.context_menu, menu);
			}
		} else {
			MenuInflater inflater = getActivity().getMenuInflater();
			inflater.inflate(R.menu.context_menu, menu);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		HashMap<String, String> selectedSong = null;

		if (isExpandableListView) {
			ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) 
					item.getMenuInfo();
			ExpandableListAdapter adapter = (ExpandableListAdapter) myAdapter;
			int childPosition = ExpandableListView.getPackedPositionChild (info.packedPosition);
			int groupPosition = ExpandableListView.getPackedPositionGroup (info.packedPosition);
			selectedSong = (HashMap<String, String>) adapter.getChild(groupPosition, childPosition);
		} else {
			AdapterView.AdapterContextMenuInfo info = 
					(AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			SimpleAdapter adapter = (SimpleAdapter) myAdapter;
			if (myAdapter != null) {
				selectedSong = (HashMap<String, String>) adapter.getItem(info.position);
			}
		}

		String artist = selectedSong.get("artist");
		String title = selectedSong.get("title");
		String filepath = selectedSong.get("filePath");

		if (item.getItemId() == R.id.play_now) {
			mCallback.onPlayNowSelected(artist, title, filepath);
		} else if (item.getItemId() == R.id.play_next) {
			mCallback.onPlayNextSelected(artist, title, filepath);
		}

		return true;
	}
}
