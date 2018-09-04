package com.brandonjenniges.javamailapidemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Timestamp;

import static android.support.v4.content.WakefulBroadcastReceiver.startWakefulService;


public class MainActivity extends Activity {
    // Call `launchTestService()` in the activity
    // to startup the service
    private PowerManager.WakeLock wl;
    private static final String DNAME = "MissedCall";
    private BufferedWriter WriterActivity;
    private java.util.Date date = new java.util.Date();
    public void launchTestService() {
        // Construct our Intent specifying the Service
        System.out.println("Created Intent!");
        Intent i = new Intent(this, HelloService.class);
        // Add extras to the bundle
        i.putExtra("foo", "bar");
        System.out.println("Created Service!");
        // Start the service
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OnBegining");
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
        try {
            WriterActivity = new BufferedWriter(new FileWriter("/sdcard/Android/data/MissedCall/MainActiviy.txt", true));
        } catch(Exception e) {
        }

        File rootPath = new File("/sdcard/Android/data/", DNAME);
        if(!rootPath.exists()) {
            rootPath.mkdirs();
        }

        try {
            WriterActivity.write("\n---------------------------------\n");
            WriterActivity.write(new Timestamp(date.getTime()) + "\n");
            WriterActivity.write("Created new Activity\nNow launching TestService\n");
        } catch (Exception e) {}
        launchTestService();
        try {
            WriterActivity.write("Launched Service\n");
        } catch (Exception e) {}
        System.out.println("Launched service!");
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            WriterActivity.write(new Timestamp(date.getTime()) + "\n");
            WriterActivity.write("Activity about to be paused\n");
        } catch(Exception e) {}
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            WriterActivity.write(new Timestamp(date.getTime()) + "\n");
            WriterActivity.write("Activity resumed\n");
        } catch(Exception e) {}
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            WriterActivity.write(new Timestamp(date.getTime()) + "\n");
            WriterActivity.write("Activity stopped\n");
        } catch (Exception e) {}
    }

    @Override
    public void onRestart() {
        super.onRestart();
        try {
            WriterActivity.write(new Timestamp(date.getTime()) + "\n");
            WriterActivity.write("Activity restarted\n");
        } catch (Exception e) {}
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            WriterActivity.write(new Timestamp(date.getTime()) + "\n");
            WriterActivity.write("Activity about to be destroyed. Going to release wakelock\n");
        } catch(Exception e) {}
        if (wl.isHeld()) wl.release();
        try {
            WriterActivity.write(new Timestamp(date.getTime()) + "\n");
            WriterActivity.write("Wakelock released\n");
            WriterActivity.close();
        } catch(Exception e) {}

    }
}
