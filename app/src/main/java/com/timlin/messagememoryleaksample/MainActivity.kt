package com.timlin.messagememoryleaksample

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference
import java.util.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private val backgroundHandlerThread = HandlerThread("BackgroundThread")
        .apply {
            start()
        }
    private val backgroundHandler = Handler(backgroundHandlerThread.looper)

    private fun doSth() {
        Log.d(TAG, "doSth() called")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val clearOnDetachListener =
            ClearOnDetachListener(DialogInterface.OnClickListener { _, _ -> {} })
        val dialog = AlertDialog.Builder(this)
            .setPositiveButton("sure", clearOnDetachListener)
            .show()
        clearOnDetachListener.clearOnDetach(dialog)

        mBtnDumpThreadInfo.setOnClickListener {
            printAppHandlerThreadInfo()
        }
        testDialogLeak()
        testDialogFragmentLeak()
        testDialogReuse()
    }

    private fun testDialogLeak() {
        mBtnNormalDialog.setOnClickListener {
            trySimulateMessageLeak {
                AlertDialog.Builder(this)
                    .setPositiveButton("Sure") { _, _ -> }
                    .show()
            }
        }
        mBtnShowAvoidLeakDialog.setOnClickListener {
            trySimulateMessageLeak(Runnable {
                AvoidLeakDialog(this)
                    .apply {
                        setTitle("Avoid Leaked Dialog")
                        setOnCancelListener {
                            Log.d(TAG, "onCancel() called")
                            doSth()
                        }
                        setOnShowListener {
                            Log.d(TAG, "onShow() called")
                            doSth()
                        }
                        setOnDismissListener {
                            Log.d(TAG, "onDismiss() called")
                            doSth()
                        }
                        setContentView(TextView(this@MainActivity).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            text = " Avoid Leak "
                        })
                    }
                    .show()
            })
        }
    }

    private fun testDialogFragmentLeak() {
        mBtnShowNormalDialogFragment.setOnClickListener {
            trySimulateMessageLeak {
                val fragment = NormalDialogFragment()
                fragment.apply {
                    show(supportFragmentManager, "NormalFragment")
                }
            }
        }
        mBtnShowAvoidLeakDialogFragment.setOnClickListener {
            trySimulateMessageLeak {
                val fragment = AvoidLeakedDialogFragment()
                fragment.show(supportFragmentManager, "AvoidLeakDialog")
            }
        }
    }

    /**
     * 测试 Dialog 复用问题 dimiss 之后，重新 show
     * */
    private fun testDialogReuse() {
        val alertDialog = AlertDialog.Builder(this)
            .setPositiveButton("sure") { dialog, which ->
                Toast.makeText(this, "you click sure", Toast.LENGTH_SHORT).show()
            }
            .create()
        mBtnShow.setOnClickListener {
            alertDialog.show()
        }
        mBtnDismiss.setOnClickListener {
            alertDialog.dismiss()
        }
    }

    private fun trySimulateMessageLeak(runnable: Runnable) {
        backgroundHandler.post {
            runOnUiThread(runnable)
        }
    }

    private fun trySimulateMessageLeak(method: () -> Unit) {
        backgroundHandler.post {
            runOnUiThread(method)
        }
    }

    private fun printAppHandlerThreadInfo() {
        val threadSet: Set<Thread> =
            Thread.getAllStackTraces().keys
        val allThreadsInfo = StringBuilder()
        allThreadsInfo.append("Total thread count=")
            .append(threadSet.size)
            .append("\n")
            .append("Total alive thread count=")
            .append(Thread.activeCount())
            .append("\n")
        val allThreadInfoList: MutableList<String> = ArrayList(threadSet.size)

        val handlerThreadsInfo = StringBuilder("all handler Threads are list as below:\n")
        var handlerThreadCount = 0

        for (thread in threadSet) {
            val stringBuilder = getThreadInfo(thread)
            allThreadInfoList.add(stringBuilder.toString())

            if (thread is HandlerThread) {
                handlerThreadCount++
                handlerThreadsInfo.append(thread.getName())
                    .append("\n")
            }
        }

        for (s in allThreadInfoList) {
            allThreadsInfo.append(s)
        }

        //遍历完才知道 handlerThreadCount
        handlerThreadsInfo.append("handler thread total num = ")
            .append(handlerThreadCount)
        Log.d(TAG, allThreadsInfo.toString() + "\n\n" + handlerThreadsInfo.toString())
    }

    private fun flushStackLocalLeaks(thread: HandlerThread) {
        if (!thread.isAlive) {
            return
        }
        val handler = Handler()
        val looper = thread.looper
        looper.queue.addIdleHandler {
            handler.sendMessageDelayed(handler.obtainMessage(), 1000)
            true
        }
    }

    private fun getThreadInfo(thread: Thread): StringBuilder {
        val stringBuilder = StringBuilder()
        stringBuilder.append("name=").append(thread.name).append("  ")
            .append("id=").append(thread.id).append("  ")
            .append("groupName = ").append(thread.threadGroup.name).append("  ")
            .append(if (thread.isDaemon) "daemon" else "")
            .append("\n")
        return stringBuilder
    }
}

/**
 * 弱引用可以解决泄漏问题，但是需要有一个地方强引用着 delegate，避免提前回收
 * */
class WeakDialogListener(delegate: DialogInterface.OnClickListener?) :
    DialogInterface.OnClickListener {
    private var weakRef = WeakReference(delegate)

    override fun onClick(dialog: DialogInterface?, which: Int) {
        weakRef.get()?.onClick(dialog, which)
    }
}

/**
 * https://medium.com/square-corner-blog/a-small-leak-will-sink-a-great-ship-efbae00f9a0f
 * square 的解决方案。View detach 的时候就将引用置为 null 了，
 * 会导致 Dialog 重新显示的时候，原来设置的 Listener 收不到回调
 *
 * 在 show 之后，调用 clearOnDetach
 * */
class ClearOnDetachListener(private var delegate: DialogInterface.OnClickListener?) :
    DialogInterface.OnClickListener {

    override fun onClick(dialog: DialogInterface?, which: Int) {
        delegate?.onClick(dialog, which)
    }

    fun clearOnDetach(dialog: Dialog) {
        dialog.window?.decorView?.viewTreeObserver?.addOnWindowAttachListener(object :
            ViewTreeObserver.OnWindowAttachListener {
            override fun onWindowDetached() {
                Log.d(TAG, "onWindowDetached: ")
                delegate = null
            }

            override fun onWindowAttached() {
            }
        })
    }
}