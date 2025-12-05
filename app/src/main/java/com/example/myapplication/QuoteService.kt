package com.example.myapplication

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.app.KeyguardManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.random.Random

class QuoteService : Service() {
    
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var quoteRunnable: Runnable? = null
    
    companion object {
        private const val CHANNEL_ID = "quote_service_channel"
        private const val QUOTE_CHANNEL_ID = "quote_notification_channel"
        private const val NOTIFICATION_ID = 1001
        private const val QUOTE_INTERVAL = 5 * 60 * 1000L // 5 minutes
        
        fun getRandomQuote(): String = AmbitionQuotes.getRandomQuote()
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(NOTIFICATION_ID, createServiceNotification())
        startQuoteScheduler()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        quoteRunnable?.let { handler.removeCallbacks(it) }
        removeOverlay()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Service notification channel
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Quote Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the quote service running"
            }
            
            // Quote notification channel
            val quoteChannel = NotificationChannel(
                QUOTE_CHANNEL_ID,
                "Motivational Quotes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows motivational quotes"
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(quoteChannel)
        }
    }
    
    private fun createServiceNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Habit Tracker")
            .setContentText("Motivational quotes active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun startQuoteScheduler() {
        quoteRunnable = object : Runnable {
            override fun run() {
                showQuote()
                handler.postDelayed(this, QUOTE_INTERVAL)
            }
        }
        // Show first quote after 5 seconds, then every 5 minutes
        handler.postDelayed(quoteRunnable!!, 5000)
    }
    
    private fun showQuote() {
        val quote = getRandomQuote()
        
        if (isDeviceLocked()) {
            showQuoteNotification(quote)
        } else {
            showQuoteOverlay(quote)
        }
    }
    
    private fun isDeviceLocked(): Boolean {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguardManager.isKeyguardLocked
    }
    
    private fun showQuoteNotification(quote: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // Split quote and analogy if present
        val parts = quote.split(" | ")
        val mainQuote = parts[0]
        val fullText = if (parts.size > 1) {
            "$mainQuote\n\n💭 ${parts[1]}"
        } else {
            mainQuote
        }
        
        val notification = NotificationCompat.Builder(this, QUOTE_CHANNEL_ID)
            .setContentTitle("💡 Daily Motivation")
            .setContentText(mainQuote)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullText))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(Random.nextInt(10000), notification)
    }
    
    private fun showQuoteOverlay(quote: String) {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            // Fall back to notification if no overlay permission
            showQuoteNotification(quote)
            return
        }
        
        handler.post {
            removeOverlay()
            
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            
            // Split quote and analogy if present
            val parts = quote.split(" | ")
            val displayText = if (parts.size > 1) {
                "💡 ${parts[0]}\n\n💭 ${parts[1]}"
            } else {
                "💡 $quote"
            }
            
            // Create overlay view
            overlayView = TextView(this).apply {
                text = displayText
                setBackgroundColor(0xE6333333.toInt()) // Dark semi-transparent
                setTextColor(0xFFFFFFFF.toInt()) // White text
                textSize = 16f
                setPadding(40, 30, 40, 30)
                gravity = Gravity.CENTER
                maxLines = 6
            }
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 100 // Offset from top
            }
            
            try {
                windowManager?.addView(overlayView, params)
                
                // Auto-dismiss after 8 seconds
                handler.postDelayed({
                    removeOverlay()
                }, 8000)
            } catch (e: Exception) {
                e.printStackTrace()
                showQuoteNotification(quote)
            }
        }
    }
    
    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // View might already be removed
            }
            overlayView = null
        }
    }
}
