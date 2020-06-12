package com.timlin.messagememoryleaksample

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment

/**
 * Created by linjintian on 2020/6/10.
 */
private const val TAG = "NormalDialogFragment"
open class NormalDialogFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return TextView(activity)
            .apply {
                text = "Normal Dialog Fragment"
            }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        Log.d(TAG, "onDismiss() called with: dialog = [$dialog]")
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        Log.d(TAG, "onCancel() called with: dialog = [$dialog]")
    }
}