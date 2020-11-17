package com.smartapp.dreamcatcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.smartapp.dreamcatcher.data.BTListAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ManageActivity extends AppCompatActivity {
    public static final String DEVICE_EXTRA = "com.asdf.multiactivitiesbt.SOCKET";
    public static final String DEVICE_UUID = "com.asdf.multiactivitiesbt.uuid";
    public static final String BUFFER_SIZE = "com.asdf.multiactivitiesbt.buffersize";
    private static final int BT_ENABLE_REQUEST = 10;

    private boolean isServiceActive = true;

    TextView tvServiceActive;
    Button btnServiceActive, btnReload;
    RecyclerView rvBTList;
    View connView;

    BTListAdapter btListAdapter;
    ArrayList<BluetoothDevice> btList = new ArrayList<>();
    BluetoothDevice selectedDevice;
    BluetoothAdapter btAdapter;

    Handler serviceSyncHandler;

    private void syncActiveUI() {
        tvServiceActive.setText(isServiceActive ? R.string.status_on : R.string.status_off);
        btnServiceActive.setText(isServiceActive ? "Stop" : "Start");
        connView.setVisibility(isServiceActive ? View.GONE : View.VISIBLE);
        if (isServiceActive) {
            btnServiceActive.setEnabled(true);
        }
    }

    public boolean isSleepServiceRunning() {
        ActivityManager manager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        String className = SleepService.class.getName();
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (className.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void beginBTDiscovery() {
        btListAdapter.clearSelection();
        btnServiceActive.setEnabled(false);
        btnServiceActive.setEnabled(false);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth not found", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!btAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, BT_ENABLE_REQUEST);
        } else {
            new BTSearchTask().execute();
        }
    }

    private void updateServiceStatus() {
        boolean currentState = isSleepServiceRunning();
        if (isServiceActive && !currentState) {
            beginBTDiscovery();
        }
        isServiceActive = currentState;
        syncActiveUI();
    }

    private void startService() {
        Intent intent = new Intent(this, SleepService.class);
        Log.d("dreamcatcher_helloextra", selectedDevice.toString());
        intent.putExtra(DEVICE_EXTRA, selectedDevice);
        intent.putExtra(DEVICE_UUID, UUID.fromString("00001101-0000-1000-8000-00805F9B34FB").toString());
        stopService(intent);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        } else {
            startService(intent);  // UNREACHABLE!
        }
    }

    private void stopService() {
        Intent intent = new Intent(this, SleepService.class);
        Log.d("dreamcatcher_hellostops", selectedDevice.toString());
        intent.putExtra(DEVICE_EXTRA, selectedDevice);
        intent.putExtra(DEVICE_UUID, UUID.fromString("00001101-0000-1000-8000-00805F9B34FB").toString());
        stopService(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage);

        tvServiceActive = findViewById(R.id.manage_active_text);
        btnServiceActive = findViewById(R.id.manage_toggle_btn);
        btnReload = findViewById(R.id.manage_btn_reload);
        rvBTList = findViewById(R.id.manage_bt_list);
        connView = findViewById(R.id.manage_connection_root);
        btListAdapter = new BTListAdapter(btList);
        rvBTList.setAdapter(btListAdapter);
        rvBTList.setLayoutManager(new LinearLayoutManager(this));
        btListAdapter.setOnClickListener(new BTListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int pos, BluetoothDevice item) {
                if (pos != RecyclerView.NO_POSITION) {
                    selectedDevice = item;
                    Log.d("dreamcatcher_helloitems", item.toString());
                    btnServiceActive.setEnabled(true);
                }
            }
        });
        btnServiceActive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("dreamcatcher_helloserv", selectedDevice.toString());
                if (isServiceActive) {
                    stopService();
                } else {
                    startService();
                }
            }
        });
        btnReload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beginBTDiscovery();
            }
        });

        serviceSyncHandler = new Handler();
        final Runnable syncTask = new Runnable() {
            @Override
            public void run() {
                updateServiceStatus();
                serviceSyncHandler.postDelayed(this, 1000);
            }
        };
        serviceSyncHandler.postDelayed(syncTask, 1000);
        updateServiceStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceSyncHandler != null) {
            serviceSyncHandler.removeCallbacksAndMessages(null);
            serviceSyncHandler = null;
        }
    }

    private class BTSearchTask extends AsyncTask<Void, Void, List<BluetoothDevice>> {
        @Override
        protected List<BluetoothDevice> doInBackground(Void... params) {
            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
            return new ArrayList<>(pairedDevices);
        }

        @Override
        protected void onPostExecute(List<BluetoothDevice> listDevices) {
            super.onPostExecute(listDevices);
            if (listDevices.size() > 0) {
                btList.clear();
                btList.addAll(listDevices);
                btListAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(getApplicationContext(), "No paired devices found, please pair your serial BT device and try again", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BT_ENABLE_REQUEST) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Bluetooth enabled successfully", Toast.LENGTH_SHORT).show();
                new BTSearchTask().execute();
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth couldn't be enabled", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
