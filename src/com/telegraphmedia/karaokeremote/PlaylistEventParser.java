/*
Copyright (c) 2018 Tim Chen

This file is part of KaraokeRemote.

This file may be used under the terms of the MIT license.

This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Public License along with KaraokeRemote.
*/
package com.telegraphmedia.karaokeremote;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

public class PlaylistEventParser {

	class SongModInfo {
		private int mIndex;
		private String mTitle;
		private String mArtist;
		private String mFilepath;

		public SongModInfo (int index, String title, String artist, String filepath) {
			mIndex = index;
			mTitle = title;
			mArtist = artist;
			mFilepath = filepath;
		}

		public int getIndex() {
			return mIndex;
		}

		public String getTitle() {
			return mTitle;
		}

		public String getArtist() {
			return mArtist;
		}

		public String getFilepath() {
			return mFilepath;
		}
	}

	private XmlPullParser parser;
	private static final String ns = null;
	private ArrayList<SongModInfo> songMods = new ArrayList<SongModInfo>();

	public PlaylistEventParser() {
		parser = Xml.newPullParser();
	}

	private String readText () 
			throws XmlPullParserException, IOException{
		String result = ""; 
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}

	private String readTitle() 
			throws XmlPullParserException, IOException{
		parser.require(XmlPullParser.START_TAG, ns, "title");
		String title = readText();
		parser.require(XmlPullParser.END_TAG, ns, "title");
		return title;
	}

	private String readArtist() 
			throws XmlPullParserException, IOException{
		parser.require(XmlPullParser.START_TAG, ns, "artist");
		String artist = readText();
		parser.require(XmlPullParser.END_TAG, ns, "artist");
		return artist;
	}

	private String readFilePath() 
			throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "filePath");
		String filePath = readText();
		parser.require(XmlPullParser.END_TAG, ns, "filePath");
		return filePath;
	}

	private String readIndex() 
			throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "index");
		String index = readText();
		parser.require(XmlPullParser.END_TAG, ns, "index");
		return index;
	}

	public ArrayList <HashMap<String, String>> modifyPlaylist (ArrayList <HashMap<String, String>> playlist,
			String playlistModAsXML) throws XmlPullParserException, IOException {
		//TODO needs error checking
		parser.setInput (new StringReader (playlistModAsXML));
		parser.nextTag();

		String modType = parser.getAttributeValue(null, "modType");	

		String title = "";
		String artist = "";
		String filePath = "";
		int index = 0;

		int eventType;
		String name;
		while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				name = parser.getName();
				if (name.equals ("songMod")) {
					while ((eventType = parser.next()) != XmlPullParser.END_TAG && 
							!parser.getName().equals("songMod")) { 
						name = parser.getName();
						if (name.equals ("title")) {
							title = readTitle ();
						} else if (name.equals ("artist")) {
							artist = readArtist ();
						} else if (name.equals ("filePath")) {
							filePath = readFilePath ();
						} else if (name.equals ("index")) {
							index = Integer.parseInt(readIndex());
						}
					}
					SongModInfo songMod = new SongModInfo (index, title, artist, filePath);
					songMods.add(songMod);
				}
			}
		}

		if (modType.equals ("del")) {
			if (index < playlist.size()) {
				playlist.remove(index);
			}
		} else if (modType.equals ("add")) {
			for (SongModInfo curSongMod : songMods) {
				index = curSongMod.getIndex();
				title = curSongMod.getTitle();
				artist = curSongMod.getArtist();
				filePath = curSongMod.getFilepath();

				HashMap<String, String> addSong = new HashMap<String, String>();
				addSong.put("artist", artist);
				addSong.put("title", title);
				addSong.put("filepath", filePath);
				if (playlist.size() == 0) {
					playlist.add (addSong);
				} else {
					if (index <= playlist.size()) {
						playlist.add(index, addSong);
					} else {
						// Index exceeds playlist size
						// Something went wrong so ignore
					}
				}
			}
		} else if (modType.equals("clear")) {
			playlist.clear();
		}
		
		// Number each playlist entry
		for (int i = 0; i < playlist.size(); i++) {
			HashMap<String, String> curSong = playlist.get(i);
			String curSongTitle = curSong.get("title");
			curSong.put("numberedTitle", Integer.toString(i + 1) // i starts from 0 
					+ ". " + curSongTitle);			
		}
		
		
		return playlist;
	}
}
