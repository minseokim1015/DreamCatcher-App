package com.smartapp.dreamcatcher;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class JoinActivity extends AppCompatActivity {
    RequestQueue volleyQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);
        volleyQueue = Volley.newRequestQueue(this);
    }

    public void onSubmitBtnClick(View view) {
        String username = ((EditText)findViewById(R.id.join_input_username)).getText().toString();
        String password = ((EditText)findViewById(R.id.join_input_password)).getText().toString();
        String password2 = ((EditText)findViewById(R.id.join_input_password2)).getText().toString();
        String name = ((EditText)findViewById(R.id.join_input_name)).getText().toString();

        if (username.equals("") || password.equals("") || name.equals("")) {
            Toast.makeText(getApplicationContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(password2)) {
            Toast.makeText(getApplicationContext(), "Passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("password", password);
        payload.put("name", name);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                String.format("%s/auth/join", MainActivity.SERVER_HOST),
                new JSONObject(payload),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(getApplication(), "Joined! Please log in.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.networkResponse != null) {
                            if (error.networkResponse.statusCode == 400) {
                                Toast.makeText(getApplication(), "This username already exists.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        // Default handling: Unknown error.
                        Toast.makeText(getApplication(), "An unexpected error occurred.", Toast.LENGTH_SHORT).show();
                        Log.e("JoinActivity", error.toString());
                    }
                }
        );
        volleyQueue.add(request);
    }
}