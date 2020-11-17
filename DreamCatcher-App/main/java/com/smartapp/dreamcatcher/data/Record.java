package com.smartapp.dreamcatcher.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Record {
    public double bpm, hrv, rr, spo2;
    public Date time;

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH);

    public static Record fromJSON(JSONObject obj) throws JSONException, ParseException {
        Record record = new Record();
        record.bpm = obj.getDouble("bpm");
        record.hrv = obj.getDouble("hrv");
        record.rr = obj.getDouble("rr");
        record.spo2 = obj.getDouble("spo2");
        record.time = Record.dateFormat.parse(obj.getString("time"));
        return record;
    }
}