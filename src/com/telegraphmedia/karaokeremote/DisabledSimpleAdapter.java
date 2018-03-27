/*
Copyright (c) 2018 Tim Chen

This file is part of KaraokeRemote.

This file may be used under the terms of the MIT license.

This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Public License along with KaraokeRemote.
*/
package com.telegraphmedia.karaokeremote;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.widget.SimpleAdapter;

public class DisabledSimpleAdapter extends SimpleAdapter {

	public DisabledSimpleAdapter(Context context,
			List<? extends Map<String, ?>> data, int resource,
					String[] from, int[] to) {
		super(context, data, resource, from, to);
	}

	@Override
	public boolean isEnabled (int position) {
		return false;
	}
}
