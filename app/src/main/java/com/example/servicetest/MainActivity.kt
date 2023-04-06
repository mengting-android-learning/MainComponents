package com.example.servicetest

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.servicetest.ui.theme.ServiceTestTheme

class MainActivity : ComponentActivity() {

    lateinit var downloadBinder: MyService.DownloadBinder
    lateinit var timeChangeReceiver: TimeChangeReceiver

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            downloadBinder = p1 as MyService.DownloadBinder
            downloadBinder.startDownload()
            downloadBinder.getProgress()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, getString(R.string.channel_id))
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        setContent {
            ServiceTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background
                ) {
                    val myIntent = Intent(this, MyService::class.java)
                    val helloIntent = Intent(this, HelloService::class.java)
                    val notificationManager: NotificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    createNotificationChannel(notificationManager)
                    Greeting(
                        {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                applicationContext.startForegroundService(helloIntent)
                            }
                        },
                        { bindService(myIntent, connection, Context.BIND_AUTO_CREATE) },
                        { unbindService(connection) },
                        {
                            notificationManager.notify(1, builder.build())
                        },
                        {
                            val broadcastIntent = Intent("com.example.servicetest.MY_BROADCAST")
                            broadcastIntent.setPackage(packageName)
                            sendOrderedBroadcast(broadcastIntent,null)
                        })
                }
            }
        }
        val notificationManager = NotificationManagerCompat.from(this)
        if (!notificationManager.areNotificationsEnabled())
            showNotificationDialog(this)
        val intentFilter = IntentFilter()
        intentFilter.addAction("android.intent.action.TIME_TICK")
        timeChangeReceiver = TimeChangeReceiver()
        registerReceiver(timeChangeReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(timeChangeReceiver)
    }

    inner class TimeChangeReceiver : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            Toast.makeText(p0, "Time has changes", Toast.LENGTH_SHORT).show()
        }

    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(getString(R.string.channel_id), name, importance).apply {
                    description = descriptionText
                }
            notificationManager.createNotificationChannel(channel)
        }
    }
}

@Composable
fun Greeting(
    start: () -> Unit,
    bind: () -> Unit,
    unbind: () -> Unit,
    notify: () -> Unit,
    sendBroadcast: () -> Unit
) {
    Column {
        Button(onClick = { start() }) {
            Text(text = "Start Service")
        }
        Button(onClick = { bind() }) {
            Text(text = "Bind Service")
        }
        Button(onClick = { unbind() }) {
            Text(text = "unBind Service")
        }
        Button(onClick = { notify() }) {
            Text(text = "notification")
        }
        Button(onClick = { sendBroadcast() }) {
            Text(text = "broadcast")
        }
    }
}

private fun showNotificationDialog(context: Context) {
    AlertDialog.Builder(context)
        .setMessage("Allow Notifications？")
        .setPositiveButton("yes") { _, _ ->
            val intent = Intent().apply {
                action = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    else -> "android.settings.APP_NOTIFICATION_SETTINGS"
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            }
            context.startActivity(intent)
        }
        .setNegativeButton("no") { _, _ -> }
        .show()
}


