package com.example.servicetest

import android.Manifest
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
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
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

    private val contacts = mutableListOf<String>()

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
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    call()
                } else {
                    Toast.makeText(this, "you have denied", Toast.LENGTH_SHORT).show()
                }
            }
        val requestReadContactsPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    readContacts()
                } else {
                    Toast.makeText(this, "can't read contacts", Toast.LENGTH_SHORT).show()
                }
            }

        setContent {
            ServiceTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
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
                            sendOrderedBroadcast(broadcastIntent, null)
                        },
                        {
                            if (
                                ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.CALL_PHONE
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                            } else {
                                call()
                            }
                        },
                        {
                            if (ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.READ_CONTACTS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                requestReadContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            } else {
                                readContacts()
                            }
                        }
                    )
                }
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction("android.intent.action.TIME_TICK")
        timeChangeReceiver = TimeChangeReceiver()
        registerReceiver(timeChangeReceiver, intentFilter)
    }

    private fun readContacts() {
        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )?.apply {
            while (moveToNext()) {
                val nameIndex = getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (nameIndex > -1) {
                    val contactName = getString(nameIndex)
                    val contactNumber = getString(numberIndex)
                    contacts.add("$contactName $contactNumber")
                }
            }
            Log.d("readContacts", contacts.toString())
        }
    }

    private fun call() {
        try {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:10086")
            startActivity(intent)
        } catch (e: SecurityException) {
            Log.w("MakeCall", e.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        val notificationManager = NotificationManagerCompat.from(this)
        if (!notificationManager.areNotificationsEnabled()) {
            showNotificationDialog(this)
        }
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
    sendBroadcast: () -> Unit,
    call: () -> Unit,
    readContacts: () -> Unit
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
        Button(onClick = { call() }) {
            Text(text = "make call")
        }
        Button(onClick = { readContacts() }) {
            Text(text = "show contacts")
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
