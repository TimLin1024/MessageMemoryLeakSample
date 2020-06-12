package com.timlin.messagememoryleaksample

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment

/**
 * Created by linjintian on 2020/6/10.
 */
private const val TAG = "AvoidLeakedDialogFragment"

open class AvoidLeakedDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AvoidLeakDialog(
            requireContext(),
            theme
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return TextView(activity)
            .apply {
                text = "Avoid Leak Dialog Fragment"
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

    /**
     * 接收 handlerThread 的 looper
     * */
    fun flushStackLocalLeaks(looper: Looper) {
        val handler = Handler(looper)
        handler.post {
            //当 队列 闲置的时候，就向它发送空的 message，以确保不会发生 message 内存泄漏
            Looper.myQueue().addIdleHandler {
                handler.sendMessageDelayed(handler.obtainMessage(), 1000)
                //返回 true，不会自动移除
                return@addIdleHandler true
            }
        }
    }
}