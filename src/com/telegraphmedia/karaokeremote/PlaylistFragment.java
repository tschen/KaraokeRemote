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
import java.util.HashMap;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class PlaylistFragment extends Fragment {

	private DisabledSimpleAdapter listAdapter;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public PlaylistFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_playlist_layout, container, false);
	}

	@Override
	public void onActivityCreated (Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Bundle bundle = this.getArguments();
		if (bundle != null) {
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<String, String>> playlist = 
					(ArrayList<HashMap<String, String>>) bundle.getSerializable("myPlaylist");
			this.displayPlaylist (getActivity(), playlist);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	public SimpleAdapter getArrayAdapter () {
		return listAdapter;
	}

	public void displayPlaylist (final Activity activity, final ArrayList<HashMap<String, String>> playlist) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				ViewGroup curView = (ViewGroup) getView();
				curView.removeAllViews();
				View.inflate(activity, R.layout.playlist_layout, curView);
				String[] from = null;
				ListView lv = (ListView) activity.findViewById(R.id.playlist_by_title);

				// Number each playlist entry
				for (int i = 0; i < playlist.size(); i++) {
					HashMap<String, String> curSong = playlist.get(i);
					String curSongTitle = curSong.get("title");
					curSong.put("numberedTitle", Integer.toString(i + 1) // i starts from 0 
							+ ". " + curSongTitle);			
				}
				
				from = new String[] {"numberedTitle", "artist"};
				int[] to = new int[] {android.R.id.text1, android.R.id.text2};
				
				listAdapter = new DisabledSimpleAdapter (activity, playlist,
						android.R.layout.simple_list_item_2, from, to);

				lv.setAdapter(listAdapter);
			}
		});
	}
}
