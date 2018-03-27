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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;

public class SearchResultsFragment extends SelectableListFragment {

	public interface onDisplayResults {
		public boolean isSearching ();
	}

	protected onDisplayResults mCallback;

	private FilterTaskDB searchTask;
	
	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public SearchResultsFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Set mCallback to the activity. This will throw an exception
		// if Activity does not implement onButtonClickListener.
		try {
			mCallback = (onDisplayResults) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnButtonClickListener");
		}
	}
	
	class FilterParamsDB {
		private CharSequence mFilterText = null;
		private SQLiteDatabase mDB = null;

		public FilterParamsDB (SQLiteDatabase db, CharSequence filterText) {
			mFilterText = filterText;
			mDB = db;
		}

		public CharSequence getFilterText() {
			return mFilterText;
		}

		public SQLiteDatabase getSongDB() {
			return mDB;
		}
	}

	private ArrayList<HashMap<String, String>> filterDB (SQLiteDatabase mDB, 
			CharSequence filterText) {

		Cursor cur = null;
		if (filterText == null || filterText.length() == 0) {
			cur = mDB.rawQuery("SELECT * FROM songs " + 
					"ORDER BY title, artist COLLATE NOCASE", null);
		} else {
			String sources[] = {"'", ".", "(", ")", ","};
			CharSequence destinations[] = {"", "", "", "", ""};
			String filterString = TextUtils.replace(filterText, sources, destinations)
					.toString().toLowerCase();
			filterString = "%" + filterString + "%";
			cur = mDB.rawQuery("SELECT * FROM songs WHERE LOWER(artistNoPunc) " +
					"like LOWER(?) OR LOWER(titleNoPunc) like LOWER(?) " + 
					"ORDER BY title, artist COLLATE NOCASE",
					new String[] {filterString, filterString});
		}
		cur.moveToFirst();
		ArrayList<HashMap<String, String>> songList = 
				new ArrayList<HashMap<String, String>>();
		while (cur.isAfterLast() == false) {
			HashMap <String, String> songInfo = 
					new HashMap <String, String>();
			songInfo.put("artist", cur.getString(cur.getColumnIndex("artist")));
			songInfo.put("title", cur.getString(cur.getColumnIndex("title")));							
			songInfo.put("filePath", cur.getString(cur.getColumnIndex("_id")));
			songList.add(songInfo);
			cur.moveToNext();
		}
		return songList;
	}

	private class FilterTaskDB extends AsyncTask<FilterParamsDB, Void, ArrayList<HashMap<String, String>>> {

		@Override
		protected ArrayList<HashMap<String, String>> doInBackground(
				FilterParamsDB... filterParams) {
			FilterParamsDB curFilterParams = filterParams[0];
			return filterDB(curFilterParams.getSongDB(), curFilterParams.getFilterText());
		}

		@Override
		protected void onPostExecute(final ArrayList<HashMap<String, String>> result) {
			getActivity().runOnUiThread (new Runnable() {
				public void run() {
					// Only display results if we are currently searching
					if (mCallback.isSearching()) {
						String[] from = new String[] {"title", "artist"};
						int[] to = new int[] {android.R.id.text1, android.R.id.text2};

						SimpleAdapter listAdapter = null;

						if (result.size() < 1) {					
							ArrayList<HashMap<String, String>> emptyResult = 
									new ArrayList<HashMap<String, String>>();
							HashMap<String, String> emptySong = new HashMap<String, String>();

							emptySong.put("artist", "");
							emptySong.put("title", "No search results found");

							emptyResult.add(emptySong);

							listAdapter = new DisabledSimpleAdapter (getActivity(), 
									emptyResult,
									android.R.layout.simple_list_item_2, from, to);
						} else {

							listAdapter = new SimpleAdapter (getActivity(), result,
									android.R.layout.simple_list_item_2, from, to);
						}

						myAdapter = listAdapter;
						ProgressBar pb = (ProgressBar) getActivity().findViewById(R.id.progressBar);
						pb.setVisibility(View.INVISIBLE);
						
						ListView lv = (ListView) getActivity().findViewById(R.id.search_results);
						lv.setAdapter(listAdapter);

						// This will be inactive for a DisabledSimpleAdapter
						lv.setOnItemClickListener(SearchResultsFragment.this);
						registerForContextMenu(lv);
					}
				}
			});
		}
	}

	public void displaySearchResultsDB (SQLiteDatabase db, String filterText) {
			isExpandableListView = false;
			ViewGroup curView = (ViewGroup) getView();
			curView.removeAllViews();
			View.inflate(getActivity(), R.layout.search_result_layout, curView);
			FilterParamsDB curFilterParams = new FilterParamsDB (db, filterText);
			if (searchTask != null) {
				searchTask.cancel(true);
			}
			searchTask = new FilterTaskDB();
			searchTask.execute(curFilterParams);
	}

	public void closeSearchResults () {
		ViewGroup curView = (ViewGroup) getView();
		curView.removeAllViews();
	}
}