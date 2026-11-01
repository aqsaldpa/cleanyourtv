package com.gathin.tvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Closes apps left in the background each time Home comes back (config.json "close_background_apps").
 * Uses ActivityManager.killBackgroundProcesses, which only ends cached processes: an app playing music
 * or showing a notification runs a foreground service and is left alone, and so is anything in "keep".
 * Runs off the main thread.
 */
final class BackgroundCleaner {
    private static final String TAG = "TvHome.Cleaner";

    private final Context context;
    private final Set<String> keep;

    BackgroundCleaner(Context context, Set<String> keep) {
        this.context = context.getApplicationContext();
        this.keep = keep;
    }

    int run() {
        ActivityManager am = context.getSystemService(ActivityManager.class);
        if (am == null) return 0;
        Set<String> packages = launchablePackages();
        packages.remove(context.getPackageName());
        packages.removeAll(keep);
        int count = 0;
        for (String pkg : packages) {
            try {
                am.killBackgroundProcesses(pkg);
                count++;
            } catch (SecurityException e) {
                Log.w(TAG, "cannot close " + pkg + ": " + e.getMessage());
            }
        }
        return count;
    }

    private Set<String> launchablePackages() {
        PackageManager pm = context.getPackageManager();
        Set<String> out = new HashSet<>();
        for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(category);
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            for (ResolveInfo info : list) out.add(info.activityInfo.packageName);
        }
        return out;
    }
}
