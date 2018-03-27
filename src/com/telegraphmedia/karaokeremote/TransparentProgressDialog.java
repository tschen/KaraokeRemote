/*
Copyright (c) 2018 Tim Chen

This file is part of KaraokeRemote.

This file may be used under the terms of the MIT license.

This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Public License along with KaraokeRemote.
*/
package com.telegraphmedia.karaokeremote;

import android.app.Dialog;
import android.content.Context;
import android.view.ViewGroup.LayoutParams;
import android.widget.ProgressBar;

public class TransparentProgressDialog extends Dialog {

	public static TransparentProgressDialog show(Context context, CharSequence title,
			CharSequence message) {
		return show(context, title, message, false);
	}

	public static TransparentProgressDialog show(Context context, CharSequence title,
			CharSequence message, boolean indeterminate) {
		return show(context, title, message, indeterminate, false, null);
	}

	public static TransparentProgressDialog show(Context context, CharSequence title,
			CharSequence message, boolean indeterminate, boolean cancelable) {
		return show(context, title, message, indeterminate, cancelable, null);
	}

	public static TransparentProgressDialog show(Context context, CharSequence title,
			CharSequence message, boolean indeterminate,
			boolean cancelable, OnCancelListener cancelListener) {
		TransparentProgressDialog dialog = new TransparentProgressDialog(context);
		dialog.setTitle(title);
		dialog.setCancelable(cancelable);
		dialog.setOnCancelListener(cancelListener);
		/* The next line will add the ProgressBar to the dialog. */
		dialog.addContentView(new ProgressBar(context, null, android.R.attr.progressBarStyleLarge), new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		dialog.show();

		return dialog;
	}

	public TransparentProgressDialog(Context context) {
		super(context, R.style.transparentDialog);
	}

}
