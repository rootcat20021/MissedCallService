package com.brandonjenniges.javamailapidemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;


public class MainActivity extends Activity {
    // Call `launchTestService()` in the activity
    // to startup the service
    public void launchTestService() {
        // Construct our Intent specifying the Service
        System.out.println("Created Intent!");
        Intent i = new Intent(this, HelloService.class);
        // Add extras to the bundle
        i.putExtra("foo", "bar");
        System.out.println("Created Service!");
        // Start the service
        startService(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("Created a MainActivity!");
        super.onCreate(savedInstanceState);
        System.out.println("Launching service!");
        launchTestService();
        System.out.println("Launched service!");
    }
}
