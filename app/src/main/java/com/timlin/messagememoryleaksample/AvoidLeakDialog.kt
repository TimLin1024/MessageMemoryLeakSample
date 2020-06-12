package com.timlin.messagememoryleaksample

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import java.lang.ref.WeakReference

/**
 * Created by linjintian on 2020/6/10.
 */
class AvoidLeakDialog @JvmOverloads
constructor(context: Context, themeResId: Int = 0) :
    Dialog(context, themeResId) {

    //主要是防止弱引用指向的 Listener被清除
    private var mOnDismissListener: DialogInterface.OnDismissListener? = null
    private var mOnCancelListener: DialogInterface.OnCancelListener? = null
    private var mOnShowListener: DialogInterface.OnShowListener? = null

    override fun setOnDismissListener(listener: DialogInterface.OnDismissListener?) {
        mOnDismissListener = listener
        super.setOnDismissListener(
            WrappedDismissDialogListener(
                listener
            )
        )
    }

    override fun setOnShowListener(listener: DialogInterface.OnShowListener?) {
        //包装一层，使用弱引用
        mOnShowListener = listener
        super.setOnShowListener(
            WrappedShowListener(
                listener
            )
        )
    }

    override fun setOnCancelListener(listener: DialogInterface.OnCancelListener?) {
        mOnCancelListener = listener
        super.setOnCancelListener(
            WrappedCancelListener(
                listener
            )
        )
    }
}

class WrappedCancelListener(delegate: DialogInterface.OnCancelListener?) :
    DialogInterface.OnCancelListener {
    private var weakRef = WeakReference(delegate)

    override fun onCancel(dialog: DialogInterface?) {
        weakRef.get()?.onCancel(dialog)
    }
}

class WrappedDismissDialogListener(delegate: DialogInterface.OnDismissListener?) :
    DialogInterface.OnDismissListener {
    private var weakRef = WeakReference(delegate)

    override fun onDismiss(dialog: DialogInterface?) {
        weakRef.get()?.onDismiss(dialog)
    }

}

class WrappedShowListener(delegate: DialogInterface.OnShowListener?) :
    DialogInterface.OnShowListener {
    private var weakRef = WeakReference(delegate)

    override fun onShow(dialog: DialogInterface?) {
        weakRef.get()?.onShow(dialog)
    }

}
