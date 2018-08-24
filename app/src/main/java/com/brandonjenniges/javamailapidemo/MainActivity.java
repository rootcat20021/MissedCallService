package com.brandonjenniges.javamailapidemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;

import java.io.File;

import static android.support.v4.content.WakefulBroadcastReceiver.startWakefulService;


public class MainActivity extends Activity {
    // Call `launchTestService()` in the activity
    // to startup the service
    private PowerManager.WakeLock wl;
    private static final String DNAME = "MissedCall";
    public void launchTestService() {
        // Construct our Intent specifying the Service
        System.out.println("Created Intent!");
        Intent i = new Intent(this, HelloService.class);
        // Add extras to the bundle
        i.putExtra("foo", "bar");
        System.out.println("Created Service!");
        // Start the service
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "BeforeServiceTag");
        wl.setReferenceCounted(false);
        if((wl != null) && (wl.isHeld()==false)) {
            wl.acquire();
        }

        startService(i);
        //startWakefulService(this, i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("Created a MainActivity!");
        System.out.println("Launching service!");
        File rootPath = new File("/sdcard/Android/data/", DNAME);
        if(!rootPath.exists()) {
            rootPath.mkdirs();
        }
        launchTestService();
        System.out.println("Launched service!");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wl.isHeld()) wl.release();
    }
}
