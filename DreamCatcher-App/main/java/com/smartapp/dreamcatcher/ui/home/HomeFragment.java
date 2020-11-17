package com.smartapp.dreamcatcher.ui.home;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.smartapp.dreamcatcher.ManageActivity;
import com.smartapp.dreamcatcher.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.smartapp.dreamcatcher.MainActivity.SERVER_HOST;

public class HomeFragment extends Fragment {
    RequestQueue volleyQueue;
    View loadingView, dataView;
    TextView tvName, tvBPM, tvHRV, tvSPO2, tvRR, tvSleepTime, tvSleepUnit, tvUpdateTime;
    Button btnSession;

    private void setDataView(String name, double bpm, double hrv, double spo2, double rr, double sleepTime, String lastUpdated) {
        tvName.setText(String.format("Hello, %s.", name));
        if (bpm >= 0) {
            tvBPM.setText(String.format("%.1f", bpm));
 //           tvBPM.setText("68.2");
        } else {
            tvBPM.setText(String.format("--"));
  //          tvBPM.setText("68.2");
        }
        if (hrv >= 0) {
            tvHRV.setText(String.format("%.1f", hrv));
        } else {
            tvHRV.setText("--");
        }
        if (spo2 >= 0) {
            tvSPO2.setText(String.format("%.1f", spo2));
        } else {
            tvSPO2.setText("--");
        }
        if (rr >= 0) {
 //           tvRR.setText("879.7");
            tvRR.setText(String.format("%.1f", rr));
        } else {
            tvRR.setText("--");
        }
        if (sleepTime < 0 || sleepTime >=0) {
            tvSleepTime.setText("00:00");
            tvSleepUnit.setVisibility(View.GONE);

        }
//        else if (sleepTime >= 3600) {
//            tvSleepTime.setText(String.format("%.1f", sleepTime / 3600f)); // changing to hrs
//            tvSleepUnit.setVisibility(View.VISIBLE);
 //       } else {
 //           tvSleepTime.setText(String.format("%02d:%02d", (int)Math.floor(sleepTime / 60), (int)(sleepTime) % 60)); // minutes and seconds
//            tvSleepUnit.setVisibility(View.GONE);
 //       }
        if (lastUpdated.equals("")) {
            tvUpdateTime.setText("Last Updated: --");
        } else {
            try {
                Date updated = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH).parse(lastUpdated);
                tvUpdateTime.setText(String.format("Last Updated: %s", new SimpleDateFormat("MMM. d, yyyy HH:mm:ss", Locale.ENGLISH).format(updated)));
            } catch (ParseException e) {
                tvUpdateTime.setText("Last Updated: --");
            }
        }
    }

    private void getMainData() {
        String url = String.format("%s/records/main", SERVER_HOST);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            setDataView(
                                    response.getString("name"),
                                    response.getDouble("bpm"),  // hr not bpm in the server. <----------
                                    response.getDouble("hrv"),
                                    response.getDouble("spo2"),
                                    response.getDouble("rr"),
                                    response.getDouble("sleep_time"), // and... sleepingtime (not sleep_time) !!!!!!!!!!!!!!! <--
                                    response.getString("record_time")
                            );
                            setLoading(false);
                        } catch (JSONException e) {
                            Log.d("dreamcatcher_helloworld", response.toString());  // FIXME: I CHANGED THIS LINE!
                            Log.e("MainFragment", e.toString());
                            Toast.makeText(getContext(), "An error occurred. Please restart the app.", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("MainFragment", error.toString());
                        Toast.makeText(getContext(), "An error occurred. Please restart the app.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        volleyQueue.add(request);
    }

    private void setLoading(boolean isLoading) {
        loadingView.setVisibility(isLoading ? View.VISIBLE : View.GONE); //compressed if-else
        dataView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        volleyQueue = Volley.newRequestQueue(getContext());

        loadingView = root.findViewById(R.id.main_loading);
        dataView = root.findViewById(R.id.main_data_root);

        tvName = root.findViewById(R.id.main_text_greeting);
        tvSleepTime = root.findViewById(R.id.main_text_sleep_time);
        tvSleepUnit = root.findViewById(R.id.main_text_sleep_unit);
        tvSPO2 = root.findViewById(R.id.main_text_spo2);
        tvHRV = root.findViewById(R.id.main_text_hrv);
        tvBPM = root.findViewById(R.id.main_text_bpm);
        tvRR = root.findViewById(R.id.main_text_rr);
        tvUpdateTime = root.findViewById(R.id.main_text_last_record);

        btnSession = root.findViewById(R.id.main_session_btn);
        btnSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSessionBtnClick();
            }
        });

        return root;
    }

    public void onSessionBtnClick() {
        Intent intent = new Intent(getContext(), ManageActivity.class);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        setLoading(true);

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                getMainData();
                handler.postDelayed(this, 1000);
                Log.i("onResume", "Homescreen updated!");
            }
        };
        handler.post(runnable); // Handler.post used when operating on the UI thread

        /*
        super.onResume();
        setLoading(true);
        getMainData(); // <-- here
        final Handler hdlr = new Handler();
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                hdlr.postDelayed(this, 2000);
                Log.e("dreamcatcher_helloworld", "AAAAAARG");
                getMainData();
            }
        };
        hdlr.postDelayed(r, 2000);

         */
    } //End of onResume
}