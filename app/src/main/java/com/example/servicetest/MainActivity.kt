package com.example.servicetest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
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
import com.example.servicetest.ui.theme.ServiceTestTheme

class MainActivity : ComponentActivity() {

    lateinit var downloadBinder: MyService.DownloadBinder

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

        setContent {
            ServiceTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background
                ) {
                    val intent = Intent(this, MyService::class.java)
                    val helloIntent = Intent(this, HelloService::class.java)
                    Greeting(
                        { startService(helloIntent) },
                        { bindService(intent, connection, Context.BIND_AUTO_CREATE) },
                        { unbindService(connection) })
                }
            }
        }
    }
}

@Composable
fun Greeting(start: () -> Unit, bind: () -> Unit, unbind: () -> Unit) {
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
    }
}


