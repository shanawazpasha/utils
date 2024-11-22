// DialogActivity.java
package com.example.yourapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

public class DialogActivity extends Activity {
    private static final String TAG = "DialogActivity";
    private static final String TARGET_PACKAGE_NAME = "com.example.targetapp"; // Replace with your target app package

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Ensure the activity is in a clean state
        if (isFinishing()) {
            return;
        }

        try {
            // Mark dialog as showing
            AppLauncherService.setDialogShowing(true);
            
            // Create dialog
            AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("App Required")
                .setMessage("Would you like to launch/install the required app?")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checkAndLaunchApp();
                        finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .create();

            // Set dismiss listener
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    AppLauncherService.setDialogShowing(false);
                    finish();
                }
            });

            // Show dialog
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error creating dialog", e);
            Toast.makeText(this, "Unable to show dialog", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void checkAndLaunchApp() {
        try {
            PackageManager pm = getPackageManager();
            
            // Check if app is installed
            Intent launchIntent = pm.getLaunchIntentForPackage(TARGET_PACKAGE_NAME);
            if (launchIntent != null) {
                // App is installed, launch it
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
                return;
            }

            // App not installed, try to open Play Store
            try {
                Intent storeIntent = new Intent(Intent.ACTION_VIEW, 
                    Uri.parse("market://details?id=" + TARGET_PACKAGE_NAME));
                storeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(storeIntent);
            } catch (android.content.ActivityNotFoundException e) {
                // If Play Store app is not installed, open web version
                Intent webIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + TARGET_PACKAGE_NAME));
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(webIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching or finding app", e);
            Toast.makeText(this, "Unable to launch app", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        // Ensure dialog showing state is reset
        AppLauncherService.setDialogShowing(false);
        super.onDestroy();
    }
}
