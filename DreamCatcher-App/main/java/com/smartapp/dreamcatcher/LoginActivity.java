package com.smartapp.dreamcatcher;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.smartapp.dreamcatcher.util.PersistentCookieStore;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.HashMap;
import java.util.Map;

import static com.smartapp.dreamcatcher.MainActivity.SERVER_HOST;

public class LoginActivity extends AppCompatActivity {
    RequestQueue volleyQueue;
    EditText inputUsername, inputPassword;
    Button btnLogin, btnJoin;
    View viewLoading, viewLogin;

    private void setLoading(boolean isLoading) {
        viewLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        viewLogin.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    Response.Listener<JSONObject> loginSuccessHandler = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            // Logged in, proceed to main page
            try {
                setLoading(false);
                String name = response.getString("name");
                Toast.makeText(getApplicationContext(), String.format("Welcome back, %s", name), Toast.LENGTH_SHORT).show();
                openMainActivity();
            } catch (JSONException e) {
                Log.e("LoginActivity", e.toString());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        CookieManager cookieManager = new CookieManager(new PersistentCookieStore(getApplicationContext()), CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
        volleyQueue = Volley.newRequestQueue(this);

        inputUsername = findViewById(R.id.login_input_username);
        inputPassword = findViewById(R.id.login_input_password);
        btnLogin = findViewById(R.id.login_btn_login);
        btnJoin = findViewById(R.id.login_btn_join);
        viewLoading = findViewById(R.id.login_loading);
        viewLogin = findViewById(R.id.login_root);
        setLoading(true);
        checkUserAuthenticated();
    }

    public void onLoginBtnClick(View v) {
        String username, password;
        username = inputUsername.getText().toString();
        password = inputPassword.getText().toString();
        if (username.equals("") || password.equals("")) {
            Toast.makeText(getApplicationContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, String> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("password", password);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                String.format("%s/auth/login", SERVER_HOST),
                new JSONObject(payload),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String name = response.getString("name");
                            Toast.makeText(getApplicationContext(), String.format("Welcome back, %s", name), Toast.LENGTH_SHORT).show();
                            openMainActivity(); //openMainActivity provided below
                        } catch (JSONException e) {
                            Log.e("LoginActivity", e.toString());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                            Toast.makeText(getApplicationContext(), "Wrong username or password", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(getApplicationContext(), "An error occurred", Toast.LENGTH_SHORT).show();
                        Log.e("LoginActivity", error.toString());
                    }
                }
        );
        volleyQueue.add(request);
    }

    public void onJoinBtnClick(View v) {
        Intent intent = new Intent(this, JoinActivity.class);
        startActivity(intent);
    }

    public void checkUserAuthenticated() {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                String.format("%s/auth/me", SERVER_HOST),
                null,
                loginSuccessHandler,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setLoading(false);
                    }
                }
        );
        volleyQueue.add(request);
    }

    public void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}