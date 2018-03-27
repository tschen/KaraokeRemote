/*
Copyright (c) 2018 Tim Chen

This file is part of KaraokeRemote.

This file may be used under the terms of the MIT license.

This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Public License along with KaraokeRemote.
*/
package com.telegraphmedia.karaokeremote;

import org.teleal.cling.model.types.UDN;

public class UpnpDevice {
	private String mFriendlyName = null;
	private UDN mUDN = null;

	public UpnpDevice (String friendlyName, UDN udn) {
		mFriendlyName = friendlyName;
		mUDN = udn;
	}

	public void setFriendlyName (String friendlyName) {
		mFriendlyName = friendlyName;
	}

	public void setUDN (UDN udn) {
		mUDN = udn;
	}

	public String getFriendlyName () {
		return mFriendlyName;
	}

	public UDN getUDN () {
		return mUDN;
	}

	@Override
	public String toString() {
		return mFriendlyName;
	}
}
