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
import android.util.Log;

public class AppLauncherService extends Service {
    private static final String TAG = "AppLauncherService";
    private static final String TARGET_PACKAGE_NAME = "com.example.targetapp"; // Replace with your target app package
    private static final int CHECK_INTERVAL = 30000; // 30 seconds in milliseconds
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "AppLauncherServiceChannel";
    
    private Handler handler;
    private Runnable checkRunnable;
    private static boolean isDialogShowing = false;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            createNotificationChannel();
            
            handler = new Handler(Looper.getMainLooper());
            checkRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!isDialogShowing) {
                            showAlertDialog();
                        }
                        handler.postDelayed(this, CHECK_INTERVAL);
                    } catch (Exception e) {
                        Log.e(TAG, "Error in checkRunnable", e);
                    }
                }
            };
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Notification notification = createNotification();
            if (notification != null) {
                startForeground(NOTIFICATION_ID, notification);
                handler.post(checkRunnable);
            } else {
                stopSelf();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartCommand", e);
            stopSelf();
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
                NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "App Launcher Service Channel",
                    NotificationManager.IMPORTANCE_LOW
                );
                serviceChannel.setSound(null, null);
                serviceChannel.enableVibration(false);
                notificationManager.createNotificationChannel(serviceChannel);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification channel", e);
        }
    }

    private Notification createNotification() {
        try {
            Intent notificationIntent = new Intent(this, DialogActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("App Launcher Service")
                .setContentText("Monitoring for app launch/install")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSound(null)
                .setPriority(NotificationCompat.PRIORITY_LOW);

            return builder.build();
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification", e);
            return null;
        }
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
        try {
            if (handler != null) {
                handler.removeCallbacks(checkRunnable);
            }
            stopForeground(true);
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
import android.util.Log;

public class DialogActivity extends AppCompatActivity {
    private static final String TAG = "DialogActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            
            AppLauncherService.setDialogShowing(true);
            
            AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
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
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            finish();
        }
    }

    private void checkAndLaunchApp() {
        try {
            String TARGET_PACKAGE_NAME = "com.example.targetapp"; // Replace with your target app package
            
            if (isAppInstalled(TARGET_PACKAGE_NAME)) {
                PackageManager pm = getPackageManager();
                Intent launchIntent = pm.getLaunchIntentForPackage(TARGET_PACKAGE_NAME);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launchIntent);
                }
            } else {
                try {
                    Intent storeIntent = new Intent(Intent.ACTION_VIEW, 
                        Uri.parse("market://details?id=" + TARGET_PACKAGE_NAME));
                    storeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(storeIntent);
                } catch (android.content.ActivityNotFoundException e) {
                    Intent webIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + TARGET_PACKAGE_NAME));
                    webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(webIntent);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in checkAndLaunchApp", e);
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
