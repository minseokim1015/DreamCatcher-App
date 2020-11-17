package com.smartapp.dreamcatcher.ui.history;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.smartapp.dreamcatcher.MainActivity;
import com.smartapp.dreamcatcher.R;
import com.smartapp.dreamcatcher.data.Record;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {
    Spinner countSelection;
    LineChart chart;
    RequestQueue volleyQueue;
    View loadingView, rootView;

    private void setLoading(boolean loading) {
        loadingView.setVisibility(loading ? View.VISIBLE : View.GONE);
        rootView.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private ArrayList<Record> generateDemoData(int count) {
        ArrayList<Record> ret = new ArrayList<>();
        for (int i=0; i<count; i++) {
            Record record = new Record();
            record.bpm = i%100;
            record.hrv = (i%100)*2;
            record.rr = 75+Math.sin((i+10)*Math.PI/17)*14;
            record.spo2 = 50+Math.sin(i*Math.PI/5)*25;
            ret.add(record);
        }
        return ret;
    }

    private void setChartData(ArrayList<Record> data) {
        List<Entry> entryBPM = new ArrayList<>();
        List<Entry> entryHRV = new ArrayList<>();
        List<Entry> entryRR = new ArrayList<>();
        List<Entry> entrySPO2 = new ArrayList<>();

        for (int i=0; i<data.size(); i++) {
            Record now = data.get(i);
            entryBPM.add(new Entry(i+1, (float)now.bpm));
            entryHRV.add(new Entry(i+1, (float)now.hrv));
            entryRR.add(new Entry(i+1, (float)now.rr));
            entrySPO2.add(new Entry(i+1, (float)now.spo2));
        }
        LineDataSet ldBPM = new LineDataSet(entryBPM, "BPM");
        ldBPM.setColor(ColorTemplate.MATERIAL_COLORS[0]);
        ldBPM.setCircleColor(ColorTemplate.MATERIAL_COLORS[0]);
        LineDataSet ldHRV = new LineDataSet(entryHRV, "HRV");
        ldHRV.setColor(ColorTemplate.MATERIAL_COLORS[1]);
        ldHRV.setCircleColor(ColorTemplate.MATERIAL_COLORS[1]);
        LineDataSet ldRR = new LineDataSet(entryRR, "RR");
        ldRR.setColor(ColorTemplate.MATERIAL_COLORS[2]);
        ldRR.setCircleColor(ColorTemplate.MATERIAL_COLORS[2]);
        LineDataSet ldSPO2 = new LineDataSet(entrySPO2, "SPO2");
        ldSPO2.setColor(ColorTemplate.MATERIAL_COLORS[3]);
        ldSPO2.setCircleColor(ColorTemplate.MATERIAL_COLORS[3]);
        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(ldBPM);
        dataSets.add(ldHRV);
        dataSets.add(ldRR);
        dataSets.add(ldSPO2);
        LineData lineData = new LineData(dataSets);
        chart.setData(lineData);
        chart.invalidate();
    }

    private void setData(ArrayList<Record> data) {
        setChartData(data);
    }

    public void getData(int count) {
//        setData(generateDemoData(count));
        setLoading(true);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                String.format("%s/records/list/%d", MainActivity.SERVER_HOST, count),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        setLoading(false);
                        JSONArray items;
                        try {
                            items = response.getJSONArray("result");
                        } catch (JSONException e) {
                            Log.e("HistoryFragment", e.toString());
                            Toast.makeText(getContext(), "An error occurred.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ArrayList<Record> data = new ArrayList<>();
                        for (int i=0; i<items.length(); i++) {
                            try {
                                data.add(Record.fromJSON(items.getJSONObject(i)));
                            } catch (JSONException | ParseException e) {
                                Log.e("HistoryFragment", e.toString());
                            }
                        }
                        setData(data);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setLoading(false);
                        Log.e("HistoryFragment", error.toString());
                        Toast.makeText(getContext(), "An error occurred.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        volleyQueue.add(request);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_history, container, false);
        volleyQueue = Volley.newRequestQueue(root.getContext());

        loadingView = root.findViewById(R.id.history_loading);
        rootView = root.findViewById(R.id.history_data_root);
        chart = root.findViewById(R.id.history_chart);
        countSelection = root.findViewById(R.id.history_spinner_count);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(root.getContext(),
                R.array.history_counts_array, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countSelection.setAdapter(spinnerAdapter);
        countSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                getData(Integer.parseInt(adapterView.getItemAtPosition(i).toString()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });
        return root;
    }
}
