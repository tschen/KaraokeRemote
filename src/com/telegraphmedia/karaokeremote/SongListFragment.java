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
import java.util.HashSet;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleAdapter;
import android.widget.SimpleExpandableListAdapter;

public class SongListFragment extends SelectableListFragment {

	// Adds section popup for fast scroll
	class TitleListAdapter extends  SimpleAdapter implements SectionIndexer  {
		HashMap<String, Integer> alphaIndexer;
		private String[] sections;
		private ArrayList<String> sectionList;
		private List<HashMap<String, String>> data = null;

		public TitleListAdapter (Context context, List<HashMap<String, String>> data, 
				int resource, String[] from, int[] to) {
			super(context, data, resource, from, to);
			alphaIndexer = new HashMap<String, Integer>();
			sectionList = new ArrayList<String>();
			this.data = data;
			int size = data.size();

			for (int i = 0; i < size; i++) {
				HashMap<String, String> song = (HashMap <String, String>) data.get(i);
				// Get first character
				if (song.get("filePath").equals("")) {
					song.put("filePath",  "Unknown");
				}

				if (song.get("title").equals("")) {
					song.put("title",  song.get("filePath"));
				}				

				String ch = song.get("title").substring(0, 1);
				// Convert character to upper case
				ch = ch.toUpperCase();

				// Put first char/index into our HashMap
				if (!alphaIndexer.containsKey(ch)) {
					alphaIndexer.put(ch, i);
					sectionList.add(ch);
				}
			}
			sections = new String[sectionList.size()];
			sectionList.toArray(sections);
		}

		@Override
		public int getPositionForSection(int section) {
			if (section >= sections.length) {
				return getCount() - 1;
			}

			return alphaIndexer.get(sections[section]);
		}

		@Override
		public int getSectionForPosition(int pos) {
			
			if (pos > data.size()) {
				return 0;
			}
			
			HashMap<String, String> song = (HashMap <String, String>) data.get(pos);
			String ch = song.get("title").substring(0, 1);
			// Convert character to upper case
			ch = ch.toUpperCase();

			for (int i = 0; i < sectionList.size(); i++) {
				if (sectionList.get(i).equals(ch)) {
					return i;
				}
			}
			return 0;

		}

		@Override
		public Object[] getSections() {
			return sections;
		}
	}

	// Adds section popup for fast scroll
	class ArtistListAdapter extends SimpleExpandableListAdapter implements SectionIndexer  {
		HashMap<String, Integer> alphaIndexer;
		private String[] sections;
		private ArrayList<String> sectionList;
        private List<HashMap<String, String>> groupData;
		
		public ArtistListAdapter(Context context,
				List<HashMap<String, String>> groupData, int groupLayout,
				String[] groupFrom, int[] groupTo,
				List<? extends List<HashMap<String, String>>> childData,
						int childLayout, String[] childFrom, int[] childTo) {
			super(context, groupData, groupLayout, groupFrom, groupTo, childData,
					childLayout, childFrom, childTo);

			alphaIndexer = new HashMap<String, Integer>();
			sectionList = new ArrayList<String>();

			this.groupData = groupData;
			
			int size = groupData.size();
			for (int i = 0; i < size; i++) {
				HashMap<String, String> artist = (HashMap <String, String>) groupData.get(i);
				// Get first character
				String ch = artist.get("artist").substring(0, 1);
				// Convert character to upper case
				ch = ch.toUpperCase();

				// Put first char/index into our HashMap
				if (!alphaIndexer.containsKey(ch)) {
					alphaIndexer.put(ch, i);
					sectionList.add(ch);
				}
			}
			sections = new String[sectionList.size()];
			sectionList.toArray(sections);
		}

		@Override
		public int getPositionForSection(int section) {
			if (section >= sections.length) {
				return getGroupCount() - 1;
			}

			return alphaIndexer.get(sections[section]);
		}

		@Override
		public int getSectionForPosition(int pos) {
			
			if (pos >= groupData.size()) {
				return 0;
			}
			
			HashMap<String, String> artist = (HashMap <String, String>) groupData.get(pos);
			// Get first character
			String ch = artist.get("artist").substring(0, 1);
			// Convert character to upper case
			ch = ch.toUpperCase();
			
			for (int i = 0; i < sectionList.size(); i++) {
				if (sectionList.get(i).equals(ch)) {
					return i;
				}
			}
			return 0;
		}

		@Override
		public Object[] getSections() {
			return sections;
		}
	}

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public SongListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	public void displaySongList (final ArrayList<HashMap<String, String>> songList, 
			final boolean isSortedByArtist) {

		getActivity().runOnUiThread(new Runnable() {
			public void run() {
				if (isSortedByArtist) {
					isExpandableListView = true;
					ViewGroup curView = (ViewGroup) getView();
					curView.removeAllViews();
					View.inflate(getActivity(), R.layout.songlist_by_artist_layout, curView);
					List<HashMap<String, String>> groupData = new ArrayList<HashMap<String, String>>();
					List<List<HashMap<String, String>>> childData = new ArrayList<List<HashMap<String, String>>>();
					HashSet<String> artistSet = new HashSet<String>();
					List<HashMap<String, String>> children = null;
					for (HashMap<String, String> item: songList) {
						String artist = item.get("artist");
						String title = item.get("title");
						String filePath = item.get("filePath");

						if (artist.equals("")) {
							artist = "Unknown";
						}

						if (filePath.equals("")) {
							filePath = "Unknown";
						}

						if (title.equals("")) {
							title = filePath;
						}						

						if (!artistSet.contains(artist)) {
							// Create artist map and add to groupData
							HashMap<String, String> artistGroup = new HashMap<String, String>();
							artistGroup.put("artist", artist);
							groupData.add(artistGroup);

							// Add artist to the set
							artistSet.add(artist);

							// Create children array and add current title to array
							children = new ArrayList<HashMap<String, String>>();
							childData.add(children);
							HashMap<String, String> curChildMap = new HashMap<String, String>();
							curChildMap.put("artist",artist);
							curChildMap.put("title",title);
							curChildMap.put("filePath", filePath);

							children.add(curChildMap);
						} else {
							// If artist exists, just add current title to children array
							HashMap<String, String> curChildMap = new HashMap<String, String>();
							curChildMap.put("artist", artist);
							curChildMap.put("title", title);
							curChildMap.put("filePath", filePath);
							children.add(curChildMap);
						}
					}

					ExpandableListView elv = 
							(ExpandableListView) getActivity().findViewById(R.id.songlist_by_artist);

					ArtistListAdapter mAdapter = new ArtistListAdapter(
							getActivity(),
							groupData,
							android.R.layout.simple_expandable_list_item_1,
							new String[] {"artist"},
							new int[] { android.R.id.text1},
							childData,
							android.R.layout.simple_expandable_list_item_2,
							new String[] {"title"},
							new int[] { android.R.id.text1}
							);

					// Save the adapter to myAdapter
					myAdapter = mAdapter;

					elv.setAdapter(mAdapter);
					elv.setOnChildClickListener(SongListFragment.this);

					registerForContextMenu(elv);

				} else {//isSortedByArtist == false
					isExpandableListView = false;
					ViewGroup curView = (ViewGroup) getView();
					curView.removeAllViews();
					View.inflate(getActivity(), R.layout.songlist_by_title_layout, curView);
					ListView lv = (ListView) getActivity().findViewById(R.id.songlist_by_title);
					String[] from = new String[] {"title", "artist"};
					int[] to = new int[] {android.R.id.text1, android.R.id.text2};

					TitleListAdapter listAdapter = new TitleListAdapter (getActivity(), songList,
							android.R.layout.simple_list_item_2, from, to);

					// Save the adapter to myAdapter
					myAdapter = listAdapter;

					lv.setAdapter(listAdapter);
					lv.setOnItemClickListener(SongListFragment.this);
					registerForContextMenu(lv);
				}
			}
		});
	}
}
