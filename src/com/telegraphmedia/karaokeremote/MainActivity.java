/*
Copyright (c) 2018 Tim Chen

This file is part of KaraokeRemote.

This file may be used under the terms of the MIT license.

This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Public License along with KaraokeRemote.
*/
package com.telegraphmedia.karaokeremote;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.zip.InflaterInputStream;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.gena.RemoteGENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.types.InvalidValueException;
import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.registry.RegistryListener;
import org.xmlpull.v1.XmlPullParserException;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;

public class MainActivity extends Activity 
implements KaraokeServerSearchDialog.onButtonClickListener, 
SelectableListFragment.onSongSelectListener, SearchResultsFragment.onDisplayResults {

	// Constants
	private static final int SEARCH_TIMEOUT = 5000; //5 second timeout

	// KaraokeRemote UI Variables
	private static ArrayList <HashMap<String, String>> playlist = null;
	private static SQLiteDatabase mDB = null;
	private boolean isSortedByArtist = true;
	private int retryAttempts = 0;
	private Fragment searchResults = null;
	private boolean isSearching = false;
	private SearchView searchView = null;

	// UPnP Variables
	private AndroidUpnpService upnpService = null;
	private Registry mRegistry = null;
	private RegistryListener registryListener = null;
	private Device mKaraokeServer = null;
	private Service mService = null;
	private String mUUID = null;
	private GENASubscription mSubscription = null;
	private ServiceConnection serviceConnection = null;
	private ServiceConnection defaultServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {

			// Set up UPnP service
			upnpService = (AndroidUpnpService) service;
			mRegistry = upnpService.getRegistry();

			// Show search dialog
			searchForKaraokeServers();
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			upnpService = null;
			mRegistry = null;
		}
	};

	private ServiceConnection restoreServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {

			// Set up UPnP service
			upnpService = (AndroidUpnpService) service;
			mRegistry = upnpService.getRegistry();

			registryListener =  new KaraokeServerRegistryListener(null);
			mRegistry.addListener(registryListener);

			// Search asynchronously for all devices
			upnpService.getControlPoint().search();

			// Try to reconnect to device
			Runnable StopSearch = new Runnable() {
				public void run() {
					if (retryAttempts < 5) {
						UDN mUDN = new UDN(mUUID);
						Collection<Device> devices =  mRegistry.getDevices();
						for (Device d : devices) {
							if (d.getIdentity().getUdn().
									getIdentifierString().equals(mUUID)) {
								mKaraokeServer = d;
							}
						}
						retryAttempts++;
						if (mKaraokeServer == null) {
							Handler myHandler = new Handler();

							// One second delay
							myHandler.postDelayed(this, 1000);
						} else {
							mService = 
									mKaraokeServer.findService(new ServiceId(getString(R.string.service_namespace), 
											getString(R.string.serviceID)));
							executeGetPlaylistAction ();
							executePlaylistModSubscription();
						}
					} else {
						resetActivity();
					}

				}
			};

			Handler myHandler = new Handler();
			myHandler.postDelayed(StopSearch, 1000);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			upnpService = null;
			mRegistry = null;
		}
	};

	// Nested class definitions
	private class KaraokeServerRegistryListener extends DefaultRegistryListener {
		private KaraokeServerSearchDialog mDialog = null;

		public KaraokeServerRegistryListener (KaraokeServerSearchDialog dialog) {
			super();
			mDialog = dialog;
		}

		@Override
		public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
			super.remoteDeviceAdded(registry, device);
			if (mDialog != null) {
				mDialog.addDevice (device.getDetails().getFriendlyName(), device.getIdentity().getUdn());
			}
		}

		@Override
		public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
			super.remoteDeviceRemoved(registry, device);
			showFailureDialog(getString(R.string.connection_failed_title), 
					getString (R.string.connection_failed_text), 
					new Callable<Void>() {

				@Override
				public Void call() throws Exception {
					resetActivity();
					return null;
				}

			}, null);
		}
	}

	private static class TabListener<T extends Fragment> implements ActionBar.TabListener {
		private Fragment mFragment;
		private final Activity mActivity;
		private final String mTag;
		private final Class<T> mClass;

		/* Constructor used each time a new tab is created.*/
		public TabListener(Activity activity, String tag, Class<T> clz) {
			mActivity = activity;
			mTag = tag;
			mClass = clz;
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			// Check if the fragment is already initialized
			if (mFragment == null) {
				// If not, instantiate and add it to the activity
				mFragment = Fragment.instantiate(mActivity, mClass.getName());
				ft.add(android.R.id.content, mFragment, mTag);

				// We can't be sure that the karaoke player has just started, and
				// there may be songs already in the playlist. So, when the MainActivity
				// starts and the user clicks on the "playlist" tab for the first time,
				// send the PlaylistFragment the current playlist.
				if (mTag.equals("playlistFrag")) {
					Bundle bundle = new Bundle();
					bundle.putSerializable("myPlaylist", playlist);
					mFragment.setArguments(bundle);
				}
			} else {
				// If it exists, simply attach it in order to show it
				ft.show(mFragment);
			}
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			if (mFragment != null) {
				ft.hide(mFragment);
			}
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// User selected the already selected tab.
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// private methods
	private void searchForKaraokeServers() {

		// Create KaraokeServerSearchDialog
		final KaraokeServerSearchDialog dialog = new KaraokeServerSearchDialog();
		dialog.show(getFragmentManager(), "KaraokeServerSearchDialog");

		// Add listener for future device advertisements
		registryListener =  new KaraokeServerRegistryListener(dialog);
		mRegistry.addListener(registryListener);

		// Search asynchronously for all devices
		upnpService.getControlPoint().search();

		Runnable StopSearch = new Runnable() {
			public void run() {
				// Stop listening for devices
				mRegistry.removeListener(registryListener);
				if (dialog != null) {
					dialog.stopSearch();
				}
			}
		};

		// Set timeout for search
		Handler myHandler = new Handler();
		myHandler.postDelayed(StopSearch, SEARCH_TIMEOUT);
	}

	private ArrayList<HashMap<String, String>> createSongList(SQLiteDatabase db) {
		Cursor cur = null;
		if (isSortedByArtist) {
			cur = db.rawQuery("SELECT * FROM songs " +
					"ORDER BY LOWER (artist), LOWER (title)", null);
		} else {
			cur = db.rawQuery("SELECT * FROM songs " + 
					"ORDER BY LOWER (title), LOWER (artist)", null);
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

	// Karaoke Server Actions
	private void executeBrowseAction () {
		CenteredProgressDialog mProgressDialog = new CenteredProgressDialog ();
		Bundle bundle = new Bundle();
		bundle.putString("title", getString(R.string.download_songbook));
		mProgressDialog.setArguments(bundle);
		mProgressDialog.setCancelable(false);
		mProgressDialog.show(getFragmentManager(), "connectingDialog");

		ActionInvocation browseAction = new ActionInvocation (mService.getAction("Browse"));

		// Invoke the action
		upnpService.getControlPoint().execute(
				new ActionCallback (browseAction) {
					@Override
					public void success(ActionInvocation invocation) {

						// Grab the songlist
						String result = invocation.getOutput ("Result").toString();						
						byte[] decodedString = null;
						//TODO
						// Implement individual try/catch
						try {
							decodedString = 
									android.util.Base64.decode(result, android.util.Base64.DEFAULT);

							InflaterInputStream ifis = 
									new InflaterInputStream (new ByteArrayInputStream (decodedString));
							FileOutputStream fos = null;
							fos = openFileOutput ("songDB.db", MODE_PRIVATE);

							int bytesread = 0;
							byte[] buffer = new byte[1048576];
							while ((bytesread = ifis.read (buffer, 0, buffer.length)) > 0) {
								fos.write(buffer, 0, bytesread);
							}
							fos.flush();
							fos.close();
							ifis.close();

							File file = getFileStreamPath ("songDB.db");

							mDB = SQLiteDatabase.openDatabase
									(getFileStreamPath ("songDB.db").getAbsolutePath(), null, MODE_PRIVATE);

							SongListFragment songListFrag = 
									(SongListFragment) getFragmentManager().findFragmentByTag("songlistFrag");
							ArrayList<HashMap<String, String>> songList = 
									createSongList (mDB);

							songListFrag.displaySongList(songList, isSortedByArtist);

							CenteredProgressDialog mProgressDialog = 
									(CenteredProgressDialog) getFragmentManager().findFragmentByTag("connectingDialog");
							if (mProgressDialog != null) {
								mProgressDialog.dismiss();
							}
						} catch (Exception e) {
							CenteredProgressDialog mProgressDialog = 
									(CenteredProgressDialog) getFragmentManager().findFragmentByTag("connectingDialog");
							if (mProgressDialog != null) {
								mProgressDialog.dismiss();
							}

							showFailureDialog(getString(R.string.browse_failed_title),
									getString(R.string.browse_failed_text),
									new Callable<Void>() {

								@Override
								public Void call() throws Exception {
									executeBrowseAction();
									return null;
								}

							}, null);
						}
					}

					@Override
					public void failure(ActionInvocation invocation,
							UpnpResponse operation, String defaultMsg) {
						showFailureDialog(getString(R.string.connection_failed_title), 
								getString (R.string.connection_failed_text), 
								new Callable<Void>() {

							@Override
							public Void call() throws Exception {
								resetActivity();
								return null;
							}
						}, null);
					}
				});
	}

	private void executeGetPlaylistAction () {
		ActionInvocation getPlaylistAction = new ActionInvocation (mService.getAction("GetPlaylist"));

		upnpService.getControlPoint().execute(
				new ActionCallback (getPlaylistAction) {

					@Override
					public void success(ActionInvocation invocation) {
						// Grab the playlist
						String result = invocation.getOutput ("Playlist").toString();
						SongListParser parser = new SongListParser();
						try {
							playlist = parser.parseSongList (result);
						} catch (XmlPullParserException e) {
							System.err.println("XmlPullParserException: " + e.getMessage());
						} catch (IOException e) {
							System.err.println("Caught IOException: " + e.getMessage());
						}

						// Update playlist fragment if it exists
						final PlaylistFragment playlistFrag = 
								(PlaylistFragment) getFragmentManager().findFragmentByTag("playlistFrag");
						if (playlistFrag != null) {
							runOnUiThread (new Runnable() {
								public void run() {
									playlistFrag.displayPlaylist(MainActivity.this, playlist);
								}
							});
						}
					}

					@Override
					public void failure(ActionInvocation invocation,
							UpnpResponse operation, String defaultMsg) {
						showFailureDialog(getString(R.string.connection_failed_title), 
								getString (R.string.connection_failed_text), 
								new Callable<Void>() {

							@Override
							public Void call() throws Exception {
								resetActivity();
								return null;
							}
						}, null);
					}
				});
	}

	private void executePlaylistModSubscription() {

		// Keep this subscription for 24 hours
		SubscriptionCallback playlistEventSubscriptionCallback = 
				new SubscriptionCallback(mService, 86400) {

			@Override
			public void established(GENASubscription sub) {
				mSubscription = sub;
			}

			@Override
			protected void failed(GENASubscription subscription,
					UpnpResponse responseStatus,
					Exception exception,
					String defaultMsg) {

			}

			@Override
			public void ended(GENASubscription sub,
					CancelReason reason,
					UpnpResponse response) {
			}

			public void eventReceived(GENASubscription sub) {

				Map<String, StateVariableValue> values = sub.getCurrentValues();
				StateVariableValue playlistMod = values.get("Playlist");

				PlaylistEventParser parser = new PlaylistEventParser();
				try {
					playlist = parser.modifyPlaylist (playlist, playlistMod.toString());
				} catch (XmlPullParserException e) {
					System.err.println("XmlPullParserException: " + e.getMessage());
				} catch (IOException e) {
					System.err.println("Caught IOException: " + e.getMessage());
				}

				final PlaylistFragment playlistFrag = 
						(PlaylistFragment) getFragmentManager().findFragmentByTag("playlistFrag");

				if (playlistFrag != null) {
					runOnUiThread (new Runnable() {
						public void run() {
							playlistFrag.getArrayAdapter().notifyDataSetChanged();
						}
					});
				}
			}

			public void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {
				// If we miss an event, request the whole playlist
				executeGetPlaylistAction();
			}
		};
		// Request subscription
		upnpService.getControlPoint().execute(playlistEventSubscriptionCallback);
	}

	private void executeAddToPlaylistAction (String artist, String title, String filePath) {
		ActionInvocation addToPlaylistAction = new ActionInvocation (mService.getAction("AddToPlaylist"));
		try {
			addToPlaylistAction.setInput("Artist", artist);
			addToPlaylistAction.setInput("Title", title);
			addToPlaylistAction.setInput("FilePath", filePath);
		} catch (InvalidValueException e) {
			System.err.println ("InvalidValueException: " + e.getMessage());
		}

		// Executes asynchronously in the background
		upnpService.getControlPoint().execute(
				new ActionCallback (addToPlaylistAction) {
					@Override
					public void success(ActionInvocation invocation) {
					}

					@Override
					public void failure(ActionInvocation invocation,
							UpnpResponse operation, String defaultMsg) {
						showFailureDialog(getString(R.string.connection_failed_title), 
								getString (R.string.connection_failed_text), 
								new Callable<Void>() {

							@Override
							public Void call() throws Exception {
								resetActivity();
								return null;
							}
						}, null);
					}
				});
	}

	private void executePlayNowAction (String artist, String title, String filePath) {
		ActionInvocation playNowAction = new ActionInvocation (mService.getAction("PlayNow"));
		try {
			playNowAction.setInput("Artist", artist);
			playNowAction.setInput("Title", title);
			playNowAction.setInput("FilePath", filePath);
		} catch (InvalidValueException e) {
			System.err.println ("InvalidValueException: " + e.getMessage());
		}

		// Executes asynchronously in the background
		upnpService.getControlPoint().execute(
				new ActionCallback (playNowAction) {
					@Override
					public void success(ActionInvocation invocation) {
					}

					@Override
					public void failure(ActionInvocation invocation,
							UpnpResponse operation, String defaultMsg) {
						showFailureDialog(getString(R.string.connection_failed_title), 
								getString (R.string.connection_failed_text), 
								new Callable<Void>() {

							@Override
							public Void call() throws Exception {
								resetActivity();
								return null;
							}
						}, null);
					}
				});
	}

	private void executePlayNextAction (String artist, String title, String filePath) {
		ActionInvocation playNextAction = new ActionInvocation (mService.getAction("PlayNext"));
		try {
			playNextAction.setInput("Artist", artist);
			playNextAction.setInput("Title", title);
			playNextAction.setInput("FilePath", filePath);
		} catch (InvalidValueException e) {
			System.err.println ("InvalidValueException: " + e.getMessage());
		}

		// Executes asynchronously in the background
		upnpService.getControlPoint().execute(
				new ActionCallback (playNextAction) {
					@Override
					public void success(ActionInvocation invocation) {
					}

					@Override
					public void failure(ActionInvocation invocation,
							UpnpResponse operation, String defaultMsg) {
						showFailureDialog(getString(R.string.connection_failed_title), 
								getString (R.string.connection_failed_text), 
								new Callable<Void>() {

							@Override
							public Void call() throws Exception {
								resetActivity();
								return null;
							}
						}, null);
					}
				});
	}


	private void executePlaylistStartAction () {
		ActionInvocation playlistStartAction = new ActionInvocation (mService.getAction("PlaylistStart"));
		// Executes asynchronously in the background
		upnpService.getControlPoint().execute(
				new ActionCallback (playlistStartAction) {
					@Override
					public void success(ActionInvocation invocation) {
					}

					@Override
					public void failure(ActionInvocation invocation,
							UpnpResponse operation, String defaultMsg) {
						showFailureDialog(getString(R.string.connection_failed_title), 
								getString (R.string.connection_failed_text), 
								new Callable<Void>() {

							@Override
							public Void call() throws Exception {
								resetActivity();
								return null;
							}
						}, null);
					}
				});
	}

	private void executePlaylistSkipSongAction () {
		ActionInvocation playlistSkipSongAction = new ActionInvocation (mService.getAction("PlaylistSkipSong"));
		// Executes asynchronously in the background
		upnpService.getControlPoint().execute(
				new ActionCallback (playlistSkipSongAction) {
					@Override
					public void success(ActionInvocation invocation) {
					}

					@Override
					public void failure(ActionInvocation invocation,
							UpnpResponse operation, String defaultMsg) {
						showFailureDialog(getString(R.string.connection_failed_title), 
								getString (R.string.connection_failed_text), 
								new Callable<Void>() {

							@Override
							public Void call() throws Exception {
								resetActivity();
								return null;
							}
						}, null);
					}
				});
	}

	private void executePlaylistStopAction () {
		ActionInvocation playlistStopAction = new ActionInvocation (mService.getAction("PlaylistStop"));
		// Executes asynchronously in the background
		upnpService.getControlPoint().execute(
				new ActionCallback (playlistStopAction) {
					@Override
					public void success(ActionInvocation invocation) {
					}

					@Override
					public void failure(ActionInvocation invocation,
							UpnpResponse operation, String defaultMsg) {
						showFailureDialog(getString(R.string.connection_failed_title), 
								getString (R.string.connection_failed_text), 
								new Callable<Void>() {

							@Override
							public Void call() throws Exception {
								resetActivity();
								return null;
							}
						}, null);
					}
				});
	}

	///////////////////////////////////////////////////////////////////////////////////////////////

	private class SortTask extends AsyncTask<Void, Void, ArrayList<HashMap<String, String>>> {
		private TransparentProgressDialog mProgressDialog = null;
		@Override
		protected ArrayList<HashMap<String, String>> doInBackground(
				Void... params) {
			runOnUiThread (new Runnable() {
				public void run() {
					mProgressDialog = new TransparentProgressDialog(MainActivity.this);
					mProgressDialog.setCancelable(false);
					mProgressDialog = mProgressDialog.show(MainActivity.this, null, null);
				}
			});
			return createSongList (mDB);
		}

		@Override
		protected void onPostExecute(ArrayList<HashMap<String, String>>result) {
			Button sortButton = (Button) findViewById(R.id.sortBtn);
			SongListFragment songListFrag = null;
			songListFrag = (SongListFragment) getFragmentManager().findFragmentByTag("songlistFrag");

			songListFrag.displaySongList(result, isSortedByArtist);

			if (isSortedByArtist) {
				sortButton.setText(R.string.sortedByArtist);
			} else {
				sortButton.setText(R.string.sortedByTitle);
			}
			mProgressDialog.dismiss();
		}
	}

	public void sortClicked(View view) {
		isSortedByArtist = !isSortedByArtist;
		new SortTask().execute();
	}

	public void playClicked (View view) {
		executePlaylistStartAction ();
	}

	public void skipSongClicked(View view) {
		executePlaylistSkipSongAction ();
	}

	public void stopClicked(View view) {
		executePlaylistStopAction();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	protected void onStop() {
		super.onStop();
		if (mRegistry != null) {
			mRegistry.pause();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mRegistry != null) {
			mRegistry.resume();

			if (mSubscription != null) {
				mRegistry.removeRemoteSubscription((RemoteGENASubscription) mSubscription);
			}

			// Subscribe to playlist changes
			if (mService != null) {
				executePlaylistModSubscription();
				
				//Grab the playlist
				executeGetPlaylistAction();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.karaoke_remote_layout);

		// Set up the tabs
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Enable app name as title
		actionBar.setDisplayShowTitleEnabled(true);

		Tab tab = actionBar.newTab()
				.setText(R.string.song_book)
				.setTabListener(new TabListener<SongListFragment>(
						this, "songlistFrag", SongListFragment.class));
		actionBar.addTab(tab);

		tab = actionBar.newTab()
				.setText(R.string.playlist)
				.setTabListener(new TabListener<PlaylistFragment>(
						this, "playlistFrag", PlaylistFragment.class));
		actionBar.addTab(tab);

		if (savedInstanceState != null) {
			mUUID = savedInstanceState.getString("UUID");
			if (mUUID != null) { // We were "connected" to a server
				serviceConnection = restoreServiceConnection;
			} else {
				serviceConnection = defaultServiceConnection;
			}
		} else {
			serviceConnection = defaultServiceConnection;
		}	

		// Start the Cling UPnP service
		getApplicationContext().bindService(
				new Intent(this, KaraokeRemoteUpnpService.class),
				serviceConnection,
				Context.BIND_AUTO_CREATE
				);
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.reset:
			resetActivity();
			return true;
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate search menu
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_search, menu);

		// Get the SearchView and set the searchable configuration
		searchView = (SearchView) menu.findItem(R.id.searchBox).getActionView();

		// Assumes current activity is the searchable activity
		searchView.setIconifiedByDefault(true);

		// Called when we start searching
		searchView.setOnQueryTextFocusChangeListener(new OnFocusChangeListener () {
			@Override
			public void onFocusChange(View view, boolean hasFocus) {
				if (searchResults == null) {
					// If not, instantiate and add it to the activity
					searchResults = new SearchResultsFragment();
					FragmentTransaction tr = getFragmentManager().beginTransaction();
					tr.add(android.R.id.content, searchResults, "search_results_frag");
					tr.commit();
				}

				if (hasFocus) {
					isSearching = true;
					FragmentTransaction tr = getFragmentManager().beginTransaction();
					getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
					tr.attach(searchResults);
					tr.commit();
				}
			}
		});

		searchView.setOnQueryTextListener(new OnQueryTextListener () {
			@Override
			public boolean onQueryTextChange(String text) {
				SearchResultsFragment searchResults = 
						(SearchResultsFragment) getFragmentManager().findFragmentByTag("search_results_frag");
				if (searchResults != null) {
					// Send songListByTitle array and search text to the SongListFragment
					// searchResults.displaySearchResults (songListByTitle, text);
					searchResults.displaySearchResultsDB (mDB, text);
				} else {
					// Search results is null
				}
				return true;
			}

			@Override
			public boolean onQueryTextSubmit(String arg0) {
				if (searchView != null) {
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
					searchView.clearFocus();
				}
				return false;
			}

		});

		searchView.setOnCloseListener(new OnCloseListener () {
			@Override
			public boolean onClose() {
				SearchResultsFragment searchResults = 
						(SearchResultsFragment) getFragmentManager().findFragmentByTag("search_results_frag");
				searchResults.closeSearchResults();

				FragmentTransaction tr = getFragmentManager().beginTransaction();
				tr.detach(searchResults);
				getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
				isSearching = false;
				return false;
			}

		});	    
		return true;
	}

	public void showExitDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Press BACK again to exit Karaoke Remote")
		.setOnKeyListener(new Dialog.OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					dialog.dismiss();
					finish();
				}
				return false;
			}
		});

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	@Override
	public void onBackPressed() {
		if (isSearching) {
			SearchResultsFragment searchResults = 
					(SearchResultsFragment) getFragmentManager().findFragmentByTag("search_results_frag");
			searchResults.closeSearchResults();

			FragmentTransaction tr = getFragmentManager().beginTransaction();
			tr.detach(searchResults);
			if (searchView != null) {
				searchView.setQuery("", false);
				searchView.setIconified(true);
			}
			getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			isSearching = false;
		} else {
			showExitDialog();
		}
	}

	@Override
	public void onAddToPlaylist(String artist, String title, String filepath) {
		executeAddToPlaylistAction (artist, title, filepath);
	}

	@Override
	public void onPlayNowSelected (String artist, String title, String filepath) {
		executePlayNowAction (artist, title, filepath);
	}

	@Override
	public void onPlayNextSelected (String artist, String title, String filepath) {
		executePlayNextAction (artist, title, filepath);
	}

	@Override
	public boolean isSearching() {
		return isSearching;
	}

	@Override
	public void onDeviceClicked(UpnpDevice device) {
		UDN mUDN = device.getUDN();

		// Save uuid to recreate activity
		mUUID =  mUDN.getIdentifierString();

		try {
			// Get the device from the registry
			mKaraokeServer = mRegistry.getDevice(mUDN, true);
			mService = mKaraokeServer.findService(new ServiceId("telegraphmedia-com", "KaraokeManager"));

			// Stop the registry listener
			mRegistry.removeListener(registryListener);

			// Subscribe to playlist updates
			// This is the most prone to change
			executePlaylistModSubscription();

			//Grab the playlist
			executeGetPlaylistAction();

			//Browse for songbook
			executeBrowseAction();
		} catch (Exception e) {
			runOnUiThread (new Runnable() {
				public void run() {
					showFailureDialog(getString(R.string.connection_failed_title), 
							getString (R.string.connection_failed_text), 
							new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							resetActivity();
							return null;
						}
					}, null);
				}});
		}
	}

	@Override
	public void onRescanClicked() {
		mRegistry.removeAllRemoteDevices();
		mRegistry.removeAllLocalDevices();
		mRegistry.removeListener(registryListener);
		searchForKaraokeServers();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Only unbind service if activity is being destroyed,
		// not on an orientation change
		if (serviceConnection != null) {
			getApplicationContext().unbindService(serviceConnection);
		}
		new Thread(new Runnable(){

			@Override
			public void run() {
				if (upnpService != null) {
					upnpService.getRegistry().shutdown();
				}
			} }).start();
	}

	private void showFailureDialog (String title, String msg, 
			final Callable<Void> posFunc, final Callable<Void> negFunc) {
		final AlertDialog.Builder builder = 
				new AlertDialog.Builder(MainActivity.this);
		builder.setMessage (msg)
		.setTitle(title)
		.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				try {
					posFunc.call();
				} catch (Exception e) {
					return;
				}
			}	
		})

		.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if (negFunc != null) {
					try {
						negFunc.call();
					} catch (Exception e) {
						return;
					}
				}
			}
		});

		runOnUiThread(new Runnable() {
			public void run() {
				AlertDialog dialog = builder.create();
				dialog.show();
			}
		});
	}

	private void resetActivity() {
		if (mRegistry != null) {
			if (mSubscription != null) {
				mRegistry.removeRemoteSubscription((RemoteGENASubscription) mSubscription);
			}
			mRegistry.removeAllLocalDevices();
			mRegistry.removeAllRemoteDevices();
			mRegistry.removeListener(registryListener);
		}
		finish();
		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(new Intent(this, MainActivity.class));
	}
}
