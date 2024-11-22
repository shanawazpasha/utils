// AppLauncherService.java
public class AppLauncherService extends Service {
    private static final String TARGET_PACKAGE_NAME = "com.example.targetapp"; // Replace with your target app package
    private static final int CHECK_INTERVAL = 30000; // 30 seconds in milliseconds
    private Handler handler;
    private Runnable checkRunnable;
    private boolean isDialogShowing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
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
        handler.post(checkRunnable);
        return START_STICKY;
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

    private void checkAndLaunchApp() {
        PackageManager pm = getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(TARGET_PACKAGE_NAME);

        if (isAppInstalled(TARGET_PACKAGE_NAME)) {
            // App is installed, launch it
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
            }
        } else {
            // App is not installed, redirect to Play Store
            try {
                Intent storeIntent = new Intent(Intent.ACTION_VIEW, 
                    Uri.parse("market://details?id=" + TARGET_PACKAGE_NAME));
                storeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(storeIntent);
            } catch (android.content.ActivityNotFoundException e) {
                // Fall back to browser if Play Store app is not installed
                Intent webIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + TARGET_PACKAGE_NAME));
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(webIntent);
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

    // Helper method to be called from DialogActivity
    public static void setDialogShowing(boolean showing) {
        isDialogShowing = showing;
    }
}

// DialogActivity.java
public class DialogActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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
