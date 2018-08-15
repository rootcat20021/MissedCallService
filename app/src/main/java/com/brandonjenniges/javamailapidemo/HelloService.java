package com.brandonjenniges.javamailapidemo;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.EditText;
import javax.mail.AuthenticationFailedException;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Iterator;
import android.content.Context;
import android.telecom.TelecomManager;
import java.io.FileReader;
import com.opencsv.CSVReader;
import com.android.internal.telephony.*;
import com.sun.mail.imap.IMAPFolder;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;


public class HelloService extends IntentService {
    private EditText user;
    private EditText pass;
    private EditText subject;
    private EditText body;
    private EditText recipient;
    private String CallerNumber;
    private static final String TAG = "HelloService";
    private static final String TAGMAIL = "MailFetch";
    private static final String TAGSMS = "SendSMS";
    private static final String TAGCSV = "CsvRead";
    private boolean isRunning  = false;
    private TelephonyManager telephonyManager;
    private Thread backgroundThread;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private Runnable myTask = new Runnable() {
        public void run() {
            sendMessage();
            isRunning = false;
        }
    };


    public HelloService() {
        super("HelloService");
    }

    @Override
    public void onCreate() {
        super.onCreate(); // if you override onCreate(), make sure to call super().
        // If a Context object is needed, call getApplicationContext() here.
        this.isRunning = true;
        this.backgroundThread = new Thread(myTask);
        telephonyManager = (TelephonyManager) getSystemService(getApplicationContext().TELEPHONY_SERVICE);
        telephonyManager.listen(myListner,PhoneStateListener.LISTEN_CALL_STATE);
    }

    private PhoneStateListener myListner = new PhoneStateListener(){
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            Log.i(TAG,"State: " + state);
            Log.i(TAG,"incomingNumber : "+incomingNumber);
            CallerNumber = incomingNumber;
            try {
                Class clazz = Class.forName(telephonyManager.getClass().getName());
                Method method = clazz.getDeclaredMethod("getITelephony");
                method.setAccessible(true);
                ITelephony telephonyService = (ITelephony) method.invoke(telephonyManager);
                if(state == TelephonyManager.CALL_STATE_RINGING) {
                    Log.i(TAG, "Disconnecting number");
                    telephonyService.endCall();
                    backgroundThread.start();
                    //this.backgroundThread.start();
                    //sendMessage();
                }
            }
            catch(Exception e) {
                Log.i(TAG,"Unable to disconnect call");
                //Log.i(TAG,e.getLocalizedMessage());
            }
            //super.onCallStateChanged(state, incomingNumber);
        }

    };


//    public int onStartCommand(Intent intent, int flags, int startId) {
//    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Normally we would do some work here, like download a file.
        // For our sample, we just sleep for 5 seconds.
        Intent previousIntent = new Intent(this, HelloService.class);
        PendingIntent ppreviousIntent = PendingIntent.getService(this, 0,
                previousIntent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("MissedCallService")
                .setTicker("Missed Call Service")
                .setContentText("Missed Call Service")
                .setOngoing(true)
                .build();

        startForeground(1337,notification);
        while(true) {
            try {
                Thread.sleep(200000000);
            } catch (InterruptedException e) {
                // Restore interrupt status.
                Thread.currentThread().interrupt();
            }
        }
        Log.i(TAG,"Exiting handler");
//        return START_STICKY;
    }


    private void sendMessage() {
        Log.i(TAG,"Sending a message!");
        Log.i(TAG,"Inside Send message : " + CallerNumber);

        IMAPFolder folder = null;
        Store store = null;
        String subject = null;
        Flags.Flag flag = null;
        Log.i(TAGMAIL,"About to access mail!");
        try {
            FileOutputStream ffile = new FileOutputStream(new File("/sdcard/Android/data/MissedCall/demo.txt"));
            ffile.write('d');
            ffile.close();
        }
        catch(Exception e) {
            Log.i(TAGMAIL,"Failed to create file!");
            Log.i(TAGMAIL,e.getLocalizedMessage());
            Log.i(TAGMAIL,e.getMessage());
        }


        try
        {
            Log.i(TAGMAIL,"Getting system property!!");
            Properties props = System.getProperties();
            props.setProperty("mail.store.protocol", "imaps");
            Log.i(TAGMAIL,"Setting session!!");
            Session session = Session.getDefaultInstance(props, null);
            Log.i(TAGMAIL,"getting store!!");
            store = session.getStore("imaps");
            Log.i(TAGMAIL,"Attemptin to connect!!");
            store.connect("imap.googlemail.com","acknowledgesynchronization@gmail.com", "synchronizationacknowledge");
            Log.i(TAGMAIL,"Connected!!");
            folder = (IMAPFolder) store.getFolder("[Gmail]/All Mail"); // This doesn't work for other email account
            //folder = (IMAPFolder) store.getFolder("[Gmail]/Sent Mail"); // This doesn't work for other email account
            //folder = (IMAPFolder) store.getFolder("[Gmail]/Drafts Mail"); // This doesn't work for other email account
            //folder = (IMAPFolder) store.getFolder("inbox"); This works for both email account


            if(!folder.isOpen())
                folder.open(Folder.READ_WRITE);
            Message[] messages = folder.getMessages();
            Log.i(TAGMAIL,"No of Messages : " + folder.getMessageCount());
            Log.i(TAGMAIL,"No of Unread Messages : " + folder.getUnreadMessageCount());

            //for (int i=0; i < messages.length;i++)
            for (int i=messages.length -1 ; i >0 ;i--) {
                Log.i(TAGMAIL,"*****************************************************************************");
                Log.i(TAGMAIL,"MESSAGE " + (i + 1) + ":");
                Message msg = messages[i];
                subject = msg.getSubject();
                Log.i(TAGMAIL,"SUBJECT: " + subject);
                if (subject.matches("SMSReport.*")) {
                    String contentType = msg.getContentType();
                    String messageContent = "";

                    // store attachment file name, separated by comma
                    String attachFiles = "";

                    if (contentType.contains("multipart")) {
                        // content may contain attachments
                        Multipart multiPart = (Multipart) msg.getContent();
                        int numberOfParts = multiPart.getCount();
                        for (int partCount = 0; partCount < numberOfParts; partCount++) {
                            MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(partCount);
                            if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                                // this part is attachment
                                String fileName = part.getFileName();
                                attachFiles += fileName + ", ";
                                part.saveFile("/sdcard/Android/data/MissedCall/" + File.separator + fileName);
                            } else {
                                // this part may be the message content
                                messageContent = part.getContent().toString();
                            }
                        }

                        if (attachFiles.length() > 1) {
                            attachFiles = attachFiles.substring(0, attachFiles.length() - 2);
                        }
                    } else if (contentType.contains("text/plain")
                            || contentType.contains("text/html")) {
                        Object content = msg.getContent();
                        if (content != null) {
                            messageContent = content.toString();
                        }
                    }

                    System.out.println("Subject: " + subject);
                    System.out.println("From: " + msg.getFrom()[0]);
                    System.out.println("To: " + msg.getAllRecipients()[0]);
                    System.out.println("Date: " + msg.getReceivedDate());
                    System.out.println("Size: " + msg.getSize());
                    System.out.println(msg.getFlags());
                    System.out.println("Body: \n" + msg.getContent());
                    System.out.println(msg.getContentType());
                    System.out.println(msg.ATTACHMENT);
                    break;
                }
            }
        }
        catch(Exception e) {
            Log.i(TAGMAIL,"Failed mail access!");
            Log.i(TAGMAIL,"^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
            Log.i(TAGMAIL,e.getLocalizedMessage());
            Log.i(TAGMAIL,"00000000000000000000000000000000000000000000000");

            System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
            e.printStackTrace();
            System.out.println(e.getMessage());
            System.out.println("**************************************************");

        }
        finally
        {
            System.out.println("Final Final");
            try {
                if (folder != null && folder.isOpen()) { folder.close(true); }
                if (store != null) { store.close(); }
            }
            catch(Exception e) {
            }
        }

        Log.i(TAG,"Reading CSV!");

        try {
            CSVReader reader = new CSVReader(new FileReader("/sdcard/Android/data/MissedCall/SendSMS.csv"));
            String[] nextLine;
            HashMap<String, String> listName = new HashMap<>();
            HashMap<String, String> listMsg = new HashMap<>();
            HashMap<String, String> listNewID = new HashMap<>();
            while ((nextLine = reader.readNext()) != null) {
                listName.put(nextLine[0],nextLine[1]);
                listNewID.put(nextLine[0],nextLine[2]);
                listMsg.put(nextLine[0],nextLine[3]);
                // nextLine[] is an array of values from the line
                Log.i(TAGCSV,nextLine[0] + " , " + nextLine[1] + " , " + nextLine[2]);
            }
            String Name = listName.get(CallerNumber);
            String Msg = listMsg.get(CallerNumber);
            SmsManager smsManager = SmsManager.getDefault();
            if (listName.containsKey(CallerNumber)) {
                Log.i(TAGSMS,"Sending SMS Sent to " + CallerNumber);
                smsManager.sendTextMessage(CallerNumber, null, Msg, null, null);
                System.out.println("Detailed SMS Sent to " + CallerNumber);
            } else {
                Log.i(TAGSMS,"Sending GP Alagh SMS Sent to " + CallerNumber);
                smsManager.sendTextMessage(CallerNumber, null, "Please contact GP Alagh in Badge Office", null, null);
                Log.i(TAGSMS,"GP Alagh SMS Sent to " + CallerNumber);
            }
        }
        catch(Exception e) {
            Log.i(TAGCSV,"#############################");
            Log.i(TAGCSV,"Even csv open failed!");
            e.printStackTrace();
        }
    }
}