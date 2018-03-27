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

import android.text.TextUtils;
import android.util.Xml;
// UNUSED in SQLite implementation
public class SongListParser {
	private XmlPullParser parser;
	private static final String ns = null;

	public SongListParser() {
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

	private HashMap <String, String> readSongInfo() 
			throws XmlPullParserException, IOException {
		String sources[] = {"'", ".", "(", ")", ","};
		CharSequence destinations[] = {"", "", "", "", ""};
		parser.require (XmlPullParser.START_TAG, ns, "songInfo");
		String title = null;
		String artist = null;
		String filePath = null;
		HashMap <String, String> songInfo = new HashMap <String, String>();
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals ("title")) {
				title = readTitle ();
			} else if (name.equals ("artist")) {
				artist = readArtist ();
			} else if (name.equals ("filePath")) {
				filePath = readFilePath ();
			}
			if (title == null) {
				title = "";
			}
			if (artist == null) {
				artist = "";
			}
			songInfo.put("title", title);
			songInfo.put("titleNoPunct", TextUtils.replace (title, sources, destinations).
					toString().toLowerCase());

			songInfo.put("artist", artist);
			songInfo.put("artistNoPunct", TextUtils.replace (artist, sources, destinations).
					toString().toLowerCase());

			songInfo.put("filePath", filePath);
		}
		return songInfo;
	}

	public ArrayList <HashMap<String, String>> parseSongList (String songListAsXML) 
			throws XmlPullParserException, IOException {
		parser.setInput (new StringReader (songListAsXML));
		parser.nextTag();

		ArrayList<HashMap<String, String>> songs = new ArrayList<HashMap<String, String>>();
		int eventType;
		while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				String name = parser.getName();
				if (name.equals("songInfo")) {
					songs.add(readSongInfo());
				}
			}
		}
		return songs;
	}
}
