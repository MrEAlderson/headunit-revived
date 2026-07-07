package com.andrerinas.headunitrevived.connection.carkey.fyt

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.IInterface
import android.os.Looper
import android.os.Parcel
import android.os.RemoteException
import android.widget.Toast
import com.andrerinas.headunitrevived.connection.carkey.CarKeyReceiver

// based of decompilation of "com.syu.steer_HD.apk"
// this mainly adds support for uis7870 (dudu 7, tested)
// although this should also affect all other uis models (uis7862 ...)
class CarFYTReceiver : CarKeyReceiver {

    private var connection: IPCConnection? = null

    override fun register(context: Context) {
        this.connection = IPCConnection(context)
        connection!!.connect()

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                "Connecting...",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun unregister() {
        TODO("Not yet implemented")
    }

    // reference: com.syu.ipcself.Conn, com.syu.steer.ipc.Ipc_NewNotifyPage
    private class IPCConnection(val context: Context) : ServiceConnection {

        private val PACKAGE_NAME = "com.syu.ms"
        private val CLASS_NAME = "app.ToolkitService"

        private var handler: Handler? = null
        private var toolkit: RemoteToolkit? = null

        fun connect() {
            if (this.handler != null)
                return

            // create separate thread
            val thread = HandlerThread("ConnectionThread")
            thread.start()

            handler = Handler(thread.looper)

            // initiate on new thread
            handler!!.post(this::attemptConnect)
        }

        private fun attemptConnect() {
            if (this.handler == null || this.toolkit != null)
                return;

            val intent = Intent()
            intent.setClassName(PACKAGE_NAME, CLASS_NAME)

            context.bindService(intent, this, Context.BIND_AUTO_CREATE)
            handler!!.postDelayed(this::attemptConnect, 2000)
        }

        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            this.toolkit = RemoteToolkit.Stub.asInterface(binder)

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    "Connected!",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // com.syu.steer.module.MySteer
            val module: RemoteModule = toolkit!!.getRemoteModule(10)!!
            val callback = AAPCallback(context)

            for (i in 0 until 20)
                module.register(callback, i, 1)
        }

        override fun onServiceDisconnected(p0: ComponentName) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    "Disconnected!",
                    Toast.LENGTH_SHORT
                ).show()
            }

            this.toolkit = null
            attemptConnect()
        }

        private class AAPCallback(val context: Context) : ModuleCallback.Stub() {

            override fun update(updateCode: Int, ints: IntArray, floats: FloatArray, strings: Array<String>) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "code=$updateCode\nints=${ints.joinToString()}\nfloats=${floats.joinToString()}\nstrings=${strings.joinToString()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
