// AppLauncherService.java
package com.example.yourapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;

public class AppLauncherService extends Service {
    private static final String TARGET_PACKAGE_NAME = "com.example.targetapp"; // Replace with your target app package
    private static final int CHECK_INTERVAL = 30000; // 30 seconds in milliseconds
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "AppLauncherServiceChannel";
    
    private Handler handler;
    private Runnable checkRunnable;
    private static boolean isDialogShowing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        handler = new Handler(Looper.getMainLooper());
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isDialogShowing) {
                    showAlertDialog();
                }
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        handler.post(checkRunnable);
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "App Launcher Service Channel",
                NotificationManager.IMPORTANCE_LOW
            );
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
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
            .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(checkRunnable);
    }

    private void showAlertDialog() {
        Intent dialogIntent = new Intent(this, DialogActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);
    }

    private boolean isAppInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void setDialogShowing(boolean showing) {
        isDialogShowing = showing;
    }
}

// DialogActivity.java
package com.example.yourapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import androidx.appcompat.app.AppCompatActivity;

public class DialogActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        AppLauncherService.setDialogShowing(true);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("App Required")
               .setMessage("Would you like to launch/install the required app?")
               .setCancelable(false)
               .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       checkAndLaunchApp();
                       finish();
                   }
               });

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                AppLauncherService.setDialogShowing(false);
                finish();
            }
        });
        
        dialog.show();
    }

    private void checkAndLaunchApp() {
        String TARGET_PACKAGE_NAME = "com.example.targetapp"; // Replace with your target app package
        
        if (isAppInstalled(TARGET_PACKAGE_NAME)) {
            // App is installed, launch it
            PackageManager pm = getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(TARGET_PACKAGE_NAME);
            if (launchIntent != null) {
                startActivity(launchIntent);
            }
        } else {
            // App is not installed, redirect to Play Store
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, 
                    Uri.parse("market://details?id=" + TARGET_PACKAGE_NAME)));
            } catch (android.content.ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + TARGET_PACKAGE_NAME)));
            }
        }
    }

    private boolean isAppInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
