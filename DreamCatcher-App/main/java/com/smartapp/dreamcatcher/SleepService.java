package com.smartapp.dreamcatcher;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;

public class SleepService extends Service {
    PowerManager.WakeLock wakeLock;

    private BluetoothSocket mBTSocket;
    private BluetoothDevice mDevice;
    private boolean mIsBluetoothConnected = false;
    private UUID mDeviceUUID;
    private ReadInput mReadThread;

    private RequestQueue volleyQueue;
// is it works now? I can see bunch of 200, the 200's are coming from the additional app routes (for auth login, etc. not too importnt)
    //The bluetooth should've sent val to request route.. JSONException @ MainFragment? Where is the MainFragment? I can't fi Maybe MainFragment is just a compiler-generated one.
    // hmm let me check the android log...
    private void sendMeasurement(String data) { // combine with bt_read below, OR use the stringbuild global variable to be defined below
        HashMap<String, Object> payload = new HashMap<>();
        //payload.put("name", );
        payload.put("value", data);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                String.format("%s/records", MainActivity.SERVER_HOST), //TODO: (just using highlight feature) change back to /records
                new JSONObject(payload),
                new Response.Listener<JSONObject>() {  // how we can build, And can I get an app screenshot? yeah

                    @Override
                    public void onResponse(JSONObject response) {  // below is unreachable Hmm unreachable too. how should we do? hm
                        Log.d("dreamcatcher_helloworld", "we can be beaten by the inf server!");  // So, if this function handles received object, it'll leave a log
                        try {
                            if(response.getInt("sleeping") < 0){
                                stopService(new Intent(getApplicationContext(), MusicService.class));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //So I will run mock server now yes
                        // What to do about the app route? Should add other routes that call from this app? Yes.
                        // There was error last time, since you can't log in to the app at the beginning
                        //
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("dreamcatcher_helloworld", error.toString()); //FIXME: <<---- This occures!
                    }
                }
        );
        volleyQueue.add(request);  // Unreachable? not executed ever. That's strange.. this code should be executed to run inf server
    }

    private JsonObjectRequest buildStartRequest() {
        return new JsonObjectRequest(
                Request.Method.PUT,
                String.format("%s/session/start", MainActivity.SERVER_HOST),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("SleepService::startReq", "Sent start request");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("SleepService::startReq", error.toString());
                    }
                }
        );
    }

    private JsonObjectRequest buildEndRequest() {
        return new JsonObjectRequest(
                Request.Method.PUT,
                String.format("%s/session/end", MainActivity.SERVER_HOST),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("SleepService::endReq", "Sent end request");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("SleepService::endReq", error.toString());
                    }
                }
        );
    }
    @Override
    public void onCreate() {
        super.onCreate();
        // TODO: Set the update rate to a reasonable rate (ex. 1 minute?)
        volleyQueue = Volley.newRequestQueue(getApplicationContext());
    } // my keyboard got

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        volleyQueue.add(buildStartRequest());  // :)
        Log.d("dreamcatcher_hellosleep", intent.toString());
        Bundle b = intent.getExtras(); // < here, I don;'t know why error again here, not inf server <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< other functionalities works, but this code makes all of the error
        //assert b != null;
        mDevice = b.getParcelable(ManageActivity.DEVICE_EXTRA); //I'm not sure, intent class stored info to transport between activities, but I don't understand why that section is a problem....
        // I think we can use Kodulo, hmmm I  thiWnke  don't have enough time to resolve all of the issues. but when using Kodulo, we can build apps in less than 3 hrs.
        mDeviceUUID = UUID.fromString(b.getString(ManageActivity.DEVICE_UUID));
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "my_service_channel";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "My Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("DreamCatcher")
                .setContentText("Reading your measurements")  // is this notification showed? Yes  but send bt not implemented in this area
                .setContentIntent(pendingIntent);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DC::MyWakelockTag");
        wakeLock.acquire();
        startForeground(1, builder.build());

        new ConnectionTask().execute();
        //return super.onStartCommand(intent, flags, startId);
        return START_REDELIVER_INTENT; //method tofix error b.getParcelable above (bluetooth code)
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wakeLock.release();
        volleyQueue.add(buildEndRequest());
        new DisconnectionTask().execute();
    }


    private class ReadInput implements Runnable {
        private boolean bStop = false;
        private Thread taskThread;
        private InputStream inputStream;
        private OutputStream outputStream;
        private byte[] buffer = new byte[513]; //1024?
        private int is_size = 0;
        private String stringbuilder = "";

        public ReadInput() {
            taskThread = new Thread(this, "Input Thread");
            taskThread.start();
        }

        public boolean isRunning() {
            return taskThread.isAlive();
        }

        private String readUntil(InputStream stream) {
            // https://stackoverflow.com/questions/33466910/android-inputstream-read-buffer-until-specific-character
            StringBuilder sb = new StringBuilder();

            try {
                BufferedReader buffer=new BufferedReader(new InputStreamReader(stream));

                int r;
                while ((r = buffer.read()) != -1) {
                    char c = (char) r;

                    if (c == 0x0A)  // 0x0A means newline
                        break;

                    sb.append(c);
                }
            } catch(IOException e) {
                // Error handling
            }
            return sb.toString();
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        private void btRead() throws IOException { //Error-check this line
            while (true){  //-- please resolve this
                try {
                    Thread.sleep(3000); //-- unhandled
                    int cnt = 0;
                    StringBuilder sb = new StringBuilder(""); // TODO reset to empty string -- need to be inside while loop (doesn't embrace lower sb val)
                    int avail = inputStream.available();
                    Log.e("dreamcatcher_helloread", Integer.toString(avail));
                    byte[] waste = new byte[avail];
                    inputStream.read(waste);
                    waste = null;  // HACK: Call garbage collector


                    while(true){
                        int available = inputStream.available();
                        if(available > 0){
                            byte[] snippet = new byte[available];
                            inputStream.read(snippet);
                            String strSnippet = new String(snippet, StandardCharsets.UTF_8);
                            sb.append(strSnippet);
                            for(int i = 0; i < available; i++){
                                if(snippet[i] == '\n') {
                                    cnt++;  // Number of lines read. WARNING: Single quotes!
                                }
                            }
                            if(cnt >= 1200) break;  // Read up to 1025 lines (1 "untrustworthy" line + 1024 lines)
                        }
                    }
                    Log.e("dreamcatcher_hellocnt", Integer.toString(cnt));
                    String data = sb.toString();
                    StringBuilder sbReturn = new StringBuilder("");  // JSON "value" payload
                    String[] splitted = data.split("\n");
                    Log.e("dreamcatcher_hellosplt", data);
                    Log.e("dreamcatcher_hellosplt", Integer.toString(splitted.length));
                    for(int i = 1; i < 1025; i++){
                        sbReturn.append(splitted[i]).append("\n"); // "Assemble" 2~1025 lines (total 1024)
                    }
                    String valuePayload = sbReturn.toString(); // DONE! Put it to the JSON payload!
                    sendMeasurement(valuePayload);  // :)
                }
                catch (InterruptedException e) { //I think this is correct
                    Log.e("btread_error", "Didn't send");
                }  // how to catch exception in java?
            }

            }


        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void run() {
            try {
                inputStream = mBTSocket.getInputStream();
                outputStream = mBTSocket.getOutputStream();
                outputStream.write("S".getBytes());
            } catch (IOException e) {
                Log.e("BT_ERROR", "Failed to get BT input stream");
                Log.e("BT_ERROR", e.toString());
                return;
            }
            while (true) {
                try {
                    //Thread.sleep(60*1000);
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    try {
                        outputStream.write("P".getBytes());
                    } catch (IOException e2) {
                        Log.e("BT_ERROR", e2.toString());
                    }
                    break;
                }
                try {
                    btRead();
                } catch (IOException e) {
                    Log.e("BT_ERROR", e.toString());
                    break;
                }
            }
        }

        public void stop() {
            taskThread.interrupt();
        }
    }


    private class DisconnectionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            if (mReadThread != null) {
                mReadThread.stop();
                while (mReadThread.isRunning()); // Wait until it stops
                mReadThread = null;
            }
            try {
                mBTSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            stopSelf();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mIsBluetoothConnected = false;
        }
    }

    // ConnectionTask class is an AsyncTask that establishes a connection to our device.
    private class ConnectionTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog loadingDialog;
        private boolean connectSuccess = false;

        @Override
        protected void onPreExecute() {
            // http://stackoverflow.com/a/11130220/1287554
            makeToast("Starting sleep monitoring");
        }

        @Override
        protected Void doInBackground(Void... devices) {
            try {
                if (mBTSocket == null || !mIsBluetoothConnected) {
                    mBTSocket = mDevice.createInsecureRfcommSocketToServiceRecord(mDeviceUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    mBTSocket.connect();
                    connectSuccess = true;
                }
            } catch (IOException e) {
                // Unable to connect to device
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (!connectSuccess) {
                makeToast("Could not connect to device. Please turn on your Hardware");
                stopSelf();
            } else {
                makeToast("Connected to device");
                mIsBluetoothConnected = true;
                mReadThread = new ReadInput(); // Kick off input reader
            }
        }
    }

    private void makeToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
