// AppLauncherService.java
package com.example.yourapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class AppLauncherService extends Service {
    private static final String TAG = "AppLauncherService";
    private static final int CHECK_INTERVAL = 30000; // 30 seconds in milliseconds
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "AppLauncherServiceChannel";
    
    private Handler handler;
    private Runnable checkRunnable;
    private static volatile boolean isDialogShowing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        
        handler = new Handler(Looper.getMainLooper());
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                // Only show dialog if not already showing
                if (!isDialogShowing) {
                    showAlertDialog();
                }
                
                // Repost the runnable
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            // Create notification channel
            createNotificationChannel();
            
            // Create and show foreground notification
            Notification notification = createNotification();
            startForeground(NOTIFICATION_ID, notification);
            
            // Start periodic checking
            handler.post(checkRunnable);
        } catch (Exception e) {
            Log.e(TAG, "Error starting service", e);
        }
        
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "App Launcher Service",
                NotificationManager.IMPORTANCE_LOW
            );
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, DialogActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            notificationIntent, 
            PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Launcher Service")
            .setContentText("Monitoring for app launch/install")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private void showAlertDialog() {
        try {
            Intent dialogIntent = new Intent(this, DialogActivity.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                                   Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                                   Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(dialogIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error showing dialog", e);
        }
    }

    @Override
    public void onDestroy() {
        // Remove any pending callbacks
        if (handler != null) {
            handler.removeCallbacks(checkRunnable);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Synchronized method to ensure thread-safe access
    public static synchronized void setDialogShowing(boolean showing) {
        isDialogShowing = showing;
    }
}
