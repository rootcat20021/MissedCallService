package com.brandonjenniges.javamailapidemo;

import android.Manifest;
import android.app.Notification;
//import android.app.NotificationManager;
import android.app.PendingIntent;
import android.R;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.lang.String;

import java.sql.Timestamp;
import java.util.Date;
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

import static android.app.Notification.EXTRA_NOTIFICATION_ID;


public class HelloService extends IntentService {
    private EditText user;
    private EditText pass;
    private EditText subject;
    private EditText body;
    private EditText recipient;
    private String CallerNumber;
    private static final String TAG = "HelloService";
    private static final String TAGMAIL = "HelloServiceMail";
    private static final String TAGSMS = "HelloServiceSMS";
    private static final String TAGCSV = "HelloServiceCSV";
    private boolean isRunning  = false;
    private TelephonyManager telephonyManager;
    private Thread backgroundThread;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final boolean USE_EXTERNAL_SMS_SERVICE = true;
    private PowerManager.WakeLock wl1;
    private PowerManager.WakeLock wl2;
    private BufferedWriter WriterService;
    private BufferedWriter WriterLife;
    private BufferedWriter writer_sms_record;
    private java.util.Date date;

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
        date = new java.util.Date();
        try {
            WriterService = new BufferedWriter(new FileWriter("/sdcard/Android/data/MissedCall/Service.txt",true));
            WriterService.write("\n---------------------------------\n");
            WriterService.write(new Timestamp(date.getTime()) + "\n");
            WriterService.write("Created new Service\nNow Acquiring lock - OnCreateTag\n");
        } catch (Exception e) {}
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wl1 = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OnCreateTag");
        wl1.setReferenceCounted(false);
        if((wl1 != null) && (wl1.isHeld()==false)) {
            wl1.acquire();
            try {
                date = new java.util.Date();
                WriterService.write(new Timestamp(date.getTime()) + "\n");
                WriterService.write("Lock acquired\n");
            } catch(Exception e) {}
        }

        // If a Context object is needed, call getApplicationContext() here.
        this.isRunning = true;
        this.backgroundThread = new Thread(myTask);


        telephonyManager = (TelephonyManager) getSystemService(getApplicationContext().TELEPHONY_SERVICE);
        //Phone listner doesn't allow to set which sim id to get triggered from. So using reflection to modify that field
        try {
            Class classToInvestigate = Class.forName("android.telephony.PhoneStateListener");
            WriterService.write("Investigating this class: " + classToInvestigate.getName() + "\n");
            // Dynamically do stuff with this class
            // List constructors, fields, methods, etc.
            Field newmSubId = classToInvestigate.getDeclaredField("mSubId");
            newmSubId.setAccessible(true);
            SubscriptionManager subscriptionManager = (SubscriptionManager) getApplicationContext().getSystemService(getApplicationContext().TELEPHONY_SUBSCRIPTION_SERVICE);
            int subscriptionIdOfSimCard1 = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(0).getSubscriptionId();
            newmSubId.set(myListner,subscriptionIdOfSimCard1);
            date = new java.util.Date();
            WriterService.write(new Timestamp(date.getTime()) + "\n");
            WriterService.write("MyListner updated for subscription ID: " + subscriptionIdOfSimCard1 + "\n");

        } catch (ClassNotFoundException e) {
            // Class not found!
            try {
                date = new java.util.Date();
                WriterService.write(new Timestamp(date.getTime()) + "\n");
                WriterService.write(e.getLocalizedMessage());
            } catch(Exception ee) {}

        } catch (Exception e) {
            // Unknown exception
            try {
                date = new java.util.Date();
                WriterService.write(new Timestamp(date.getTime()) + "\n");
                WriterService.write(e.getLocalizedMessage());
            } catch(Exception ee) {}
        }

        telephonyManager.listen(myListner,PhoneStateListener.LISTEN_CALL_STATE);
        try {
            date = new java.util.Date();
            WriterService.write(new Timestamp(date.getTime()) + "\n");
            WriterService.write("Created call listner\n");
        } catch(Exception ee) {}

        try {
            WriterService.write("Exiting onCreate\n");
            WriterService.close();
        } catch(Exception e) {}
    }

    private PhoneStateListener myListner = new PhoneStateListener(){
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            Log.i(TAG,"State: " + state);
            Log.i(TAG,"incomingNumber : "+incomingNumber);
            try {
                WriterService = new BufferedWriter(new FileWriter("/sdcard/Android/data/MissedCall/Service.txt",true));
                WriterService.write("--------------------------------\n");
                date = new java.util.Date();
                WriterService.write(new Timestamp(date.getTime()) + "\n");
                WriterService.write("Call state listner got hit\n");
                WriterService.write("Stare = " + state + "\n");
                WriterService.write("Incoming Number = " + incomingNumber + "\n");
            } catch(Exception e) {}


            try {
                Class clazz = Class.forName(telephonyManager.getClass().getName());
                Method method = clazz.getDeclaredMethod("getITelephony");
                method.setAccessible(true);
                ITelephony telephonyService = (ITelephony) method.invoke(telephonyManager);
                if(state == TelephonyManager.CALL_STATE_RINGING) {
                    CallerNumber = incomingNumber;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // Restore interrupt status.
                        Thread.currentThread().interrupt();
                    }

                    Log.i(TAG, "Disconnecting number");
                    try {
                        date = new java.util.Date();
                        WriterService.write(new Timestamp(date.getTime()) + "\n");
                        WriterService.write("Disconnecting number" + telephonyManager.getSimSerialNumber() + "\n");
                    } catch(Exception e) {
                    }
                    telephonyService.endCall();
                    try {
                        date = new java.util.Date();
                        WriterService.write(new Timestamp(date.getTime()) + "\n");
                        WriterService.write("Disconnected number.Now starting backgroundthread\n");
                    } catch(Exception e) {
                    }
                    backgroundThread.start();
                    try {
                        date = new java.util.Date();
                        WriterService.write(new Timestamp(date.getTime()) + "\n");
                        WriterService.write("Started Thread\n");
                    } catch(Exception e) {
                    }
                    //this.backgroundThread.start();
                    //sendMessage();
                }
            }
            catch(Exception e) {
                Log.i(TAG,"Unable to disconnect call");
                try {
                    date = new java.util.Date();
                    WriterService.write(new Timestamp(date.getTime()) + "\n");
                    WriterService.write(e.getLocalizedMessage());
                    WriterService.write(e.getMessage());
                } catch(Exception ee) {
                }
            }

            try {
                date = new java.util.Date();
                WriterService.write(new Timestamp(date.getTime()) + "\n");
                WriterService.write("Exiting phonelistner!\n");
                WriterService.close();
            } catch(Exception e) {}
            //super.onCallStateChanged(state, incomingNumber);
        }
    };



//    public int onStartCommand(Intent intent, int flags, int startId) {
//    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //super.onHandleIntent();
        // Normally we would do some work here, like download a file.
        // For our sample, we just sleep for 5 seconds.
        Intent previousIntent = new Intent(this, HelloService.class);
        PendingIntent ppreviousIntent = PendingIntent.getService(this, 0,
                previousIntent, 0);
        //CharSequence name = getString("MyChannel");
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "MissedCallChannel")
                .setSmallIcon(R.drawable.stat_notify_missed_call)
                .setContentTitle("Missed Call Service")
                .setContentText("I will send message")
                .setContentIntent(ppreviousIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(17299, mBuilder.build());

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wl2 = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocationManagerService");
        wl2.setReferenceCounted(false);
        if((wl2 != null) && (wl2.isHeld()==false)) {
            wl2.acquire();
        }

        //startForeground(1337,notification);
        startForeground(1337,mBuilder.build());
        while(true) {
            try {
                WriterLife = new BufferedWriter(new FileWriter("/sdcard/Android/data/MissedCall/Tick.txt",true));
                date = new java.util.Date();
                WriterLife.write(new Timestamp(date.getTime()) + "\n");
                WriterLife.write("I was last seen awake\n");
                WriterLife.close();
            } catch(Exception e) {
                Log.i(TAG,e.getLocalizedMessage());
            }
            //Log.i(TAG,"I was last seen awake\n");
            try {
                Thread.sleep(2000);
            } catch(Exception e) {}
        }

        //wl.release();
        //Log.i(TAG,"Exiting handler");
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
            try {
                WriterService = new BufferedWriter(new FileWriter("/sdcard/Android/data/MissedCall/Service.txt",true));
                writer_sms_record = new BufferedWriter(new FileWriter("/sdcard/Android/data/MissedCall/SMSRecord.csv",true));
                writer_sms_record.write("Time,Phone No.,ID,Name,Message,Mechanism\n");
                date = new java.util.Date();
                WriterService.write(new Timestamp(date.getTime()) + "\n");
                WriterService.write("Inside SendMesage\n");
                Log.i(TAG,"opened SendMessage debug.txt");
            } catch(Exception e) {
                System.out.println(e.getMessage());
                System.out.println("SendMessageDebug.txt file couldnt be open");
            }
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
            Date date = new Date() ;
            try {
                WriterService.write(new Timestamp(date.getTime()) + "\n");
                WriterService.write("Connected mail\n");
            } catch(Exception ee) {
            }


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
                try {
                    date = new java.util.Date();
                    WriterService.write(new Timestamp(date.getTime()) + "\n");
                    WriterService.write("Message: " + (i + 1) + ":");
                    WriterService.write("Subject: " + subject + "\n");
                } catch(Exception e) {
                }

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
            try {
                date = new java.util.Date();
                WriterService.write(new Timestamp(date.getTime()) + "\n");
                WriterService.write("Failed Mail Access\n");
                WriterService.write("ERROR: " + e.getLocalizedMessage() + "\n");
            } catch(Exception ee) {
            }

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
                listName.put(nextLine[3],nextLine[8]);
                listNewID.put(nextLine[3],nextLine[4]);
                listMsg.put(nextLine[3],nextLine[21]);
                // nextLine[] is an array of values from the line
                //Log.i(TAGCSV,nextLine[3] + " , " + nextLine[1] + " , " + nextLine[2] + "," + nextLine[21]);
            }
            String Name = listName.get(CallerNumber);
            String Msg = listMsg.get(CallerNumber);
            String NewID = listNewID.get(CallerNumber);
            if (listName.containsKey(CallerNumber)) {
            } else {
                Msg = "Please Contact GP Alagh 8800298700 for registering your number";
            }
            if(USE_EXTERNAL_SMS_SERVICE) {
                URL url;
                HttpURLConnection urlConnection = null;
                String inputLine;
                try {
                    String sms_url_api = "http://enterprise.easyserve.me/http-api.php?username=vajoff&password=vajoff@123&senderid=BAJOFF&route=1&number=" + CallerNumber + "&message=" + Msg;
                    Log.i(TAGSMS,"Opening URL: " + sms_url_api);
                    url = new URL(sms_url_api);
                    urlConnection = (HttpURLConnection) url
                            .openConnection();
                    //Set methods and timeouts
                    urlConnection.setRequestMethod("GET");
                    urlConnection.setReadTimeout(15000);
                    urlConnection.setConnectTimeout(15000);

                    try {
                        date = new java.util.Date();
                        WriterService.write(new Timestamp(date.getTime()) + "\n");
                        WriterService.write("Attempting to connect the external SMS service\n");
                    } catch(Exception e) {
                    }
                    //Connect to our url
                    urlConnection.connect();
                    //Create a new InputStreamReader
                    InputStreamReader streamReader = new
                            InputStreamReader(urlConnection.getInputStream());
                    //Create a new buffered reader and String Builder
                    BufferedReader breader = new BufferedReader(streamReader);
                    StringBuilder stringBuilder = new StringBuilder();
                    //Check if the line we are reading is not null
                    while((inputLine = breader.readLine()) != null){
                        stringBuilder.append(inputLine);
                    }
                    //Close our InputStream and Buffered reader
                    breader.close();
                    streamReader.close();
                    urlConnection.disconnect();
                    date = new java.util.Date();
                    writer_sms_record.write(new Timestamp(date.getTime()) + "," + CallerNumber + "," + NewID + "," + Name + "," + Msg + "," + "EXTERNAL SMS SERVICE\n");
                    Log.i(TAGSMS,"Successfully opened external sms url api!");
                    try {
                        date = new java.util.Date();
                        WriterService.write(new Timestamp(date.getTime()) + "\n");
                        WriterService.write("Successfully external SMS service\n");
                    } catch(Exception e) {
                    }
                }
                catch(Exception e) {
                    Log.i(TAGSMS,"Couldnt send by external SMS service! Trying by phone sms");
                    try {
                        date = new java.util.Date();
                        WriterService.write(new Timestamp(date.getTime()) + "\n");
                        WriterService.write("Couldnt send by external SMS service\n");
                        WriterService.write("ERROR: " + e.getLocalizedMessage() + "\n");
                    } catch(Exception ee) {
                    }
                    SmsManager smsManager = SmsManager.getDefault();
                    if (listName.containsKey(CallerNumber)) {
                        Log.i(TAGSMS, "Backup Sending SMS Sent to " + CallerNumber);
                        smsManager.sendTextMessage(CallerNumber, null, Msg, null, null);
                        System.out.println("Detailed SMS Sent to " + CallerNumber);
                    } else {
                        Log.i(TAGSMS, "Backup Sending GP Alagh SMS Sent to " + CallerNumber);
                        smsManager.sendTextMessage(CallerNumber, null, "Please contact GP Alagh in Badge Office", null, null);
                        Log.i(TAGSMS, "GP Alagh SMS Sent to " + CallerNumber);
                    }
                    date = new java.util.Date();
                    writer_sms_record.write(new Timestamp(date.getTime()) + "," + CallerNumber + "," + NewID + "," + Name + "," + Msg + "," + "LOCAL PHONE SMS SERVICE\n");
                    try {
                        date = new java.util.Date();
                        WriterService.write(new Timestamp(date.getTime()) + "\n");
                        WriterService.write("Sent by phone SMS failsafe\n");
                    } catch(Exception ee) {
                    }
                }
            } else {
                try {
                    date = new java.util.Date();
                    WriterService.write(new Timestamp(date.getTime()) + "\n");
                    WriterService.write("Sending by phone SMS\n");
                } catch(Exception ee) {
                }
                SmsManager smsManager = SmsManager.getDefault();
                if (listName.containsKey(CallerNumber)) {
                    Log.i(TAGSMS, "Sending SMS Sent to " + CallerNumber);
                    smsManager.sendTextMessage(CallerNumber, null, Msg, null, null);
                    System.out.println("Detailed SMS Sent to " + CallerNumber);
                } else {
                    Log.i(TAGSMS, "Sending GP Alagh SMS Sent to " + CallerNumber);
                    smsManager.sendTextMessage(CallerNumber, null, "Please contact GP Alagh in Badge Office", null, null);
                    Log.i(TAGSMS, "GP Alagh SMS Sent to " + CallerNumber);
                }
                date = new java.util.Date();
                writer_sms_record.write(new Timestamp(date.getTime()) + "," + CallerNumber + "," + NewID + "," + Name + "," + Msg + "," + "LOCAL PHONE SMS SERVICE\n");
                try {
                    WriterService.write(new Timestamp(date.getTime()) + "\n");
                    WriterService.write("Sent by phone SMS\n");
                } catch(Exception ee) {
                }
            }
        }
        catch(Exception e) {
            Log.i(TAGCSV,"#############################");
            Log.i(TAGCSV,"Even csv open failed!");
            e.printStackTrace();
            try {
                date = new java.util.Date();
                WriterService.write(new Timestamp(date.getTime()) + "\n");
                WriterService.write("Even open CSV failed\n");
                WriterService.write("ERROR: " + e.getLocalizedMessage() + "\n");
            } catch(Exception ee) {
            }
        }
        try {
            WriterService.close();
            writer_sms_record.close();
        } catch(Exception e) {
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            date = new java.util.Date();
            WriterService.write(new Timestamp(date.getTime()) + "\n");
            WriterService.write("Service about to be destroyed\n");
            WriterService.write("+++++++++++++++++++++++++++++++++\n");
        } catch(Exception e) {}
        if (wl1.isHeld()) {
            wl1.release();
        } else {
            try {
                WriterService.write("OnCreateTag Wakelock tag not held?\n");
            } catch(Exception e) {}
        }
        if (wl2.isHeld()) {
            wl2.release();
        } else {
            try {
                WriterService.write("LocationManagerService Wakelog tag not held?\n");
            } catch(Exception e) {}
        }

        try {
            WriterService.write("Both wakelocks released\n");
            WriterService.close();
        } catch(Exception e) {}
    }

}
