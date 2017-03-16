package com.apimca.masdemoapp;

import android.app.Activity;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.content.Context;
import android.widget.Toast;
import android.view.View;

import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASAuthenticationListener;
import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASDevice;
import com.ca.mas.foundation.MASOtpAuthenticationHandler;
import com.ca.mas.foundation.MASRequest;
import com.ca.mas.foundation.MASResponse;
import com.ca.mas.foundation.MASUser;
import com.ca.mas.foundation.auth.MASAuthenticationProviders;
import com.ca.mas.ui.MASLoginFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();
    private TextView txtResponse;
    private Button btnStartMAS;
    private Button btnUserAuth;
    private Button btnCallAPI;
    private Button btnLogout;
    private Button btnDeregister;
    private Context mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtResponse = (TextView) findViewById(R.id.txtResponse);
        btnStartMAS = (Button) findViewById(R.id.btnStartMAS);
        btnUserAuth = (Button) findViewById(R.id.btnUserAuth);
        btnCallAPI = (Button) findViewById(R.id.btnCallAPI);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnDeregister = (Button) findViewById(R.id.btnDeregister);
        mContext = this;

    }

    //
    // Function to start MAS
    //
    public void startMAS(View v) {

        MAS.debug();
        //This will start MAS with the default configuration
        MAS.start(mContext,true);


    }

    //
    // Function to trigger the User Authentication
    //
    public void userAuth(View v) {

        MAS.setAuthenticationListener(new MASAuthenticationListener() {
            @Override
            public void onAuthenticateRequest(Context context, long requestId, MASAuthenticationProviders providers) {
                android.app.DialogFragment loginFragment = MASLoginFragment.newInstance(requestId, providers);
                loginFragment.show(((Activity) context).getFragmentManager(), "logonDialog");
            }

            @Override
            public void onOtpAuthenticateRequest(Context context, MASOtpAuthenticationHandler handler) {
                //Ignore for now
            }
        });

        MAS.setAuthenticationListener(
                new MASAuthenticationListener() {
                    @Override
                    public void onAuthenticateRequest(Context context, long requestId,
                                                      MASAuthenticationProviders masAuthenticationProviders) {
                        android.app.DialogFragment loginFragment =
                                MASLoginFragment.newInstance(requestId, masAuthenticationProviders);
                        loginFragment.show(((android.app.Activity) context).getFragmentManager(), "loginDialog");
                    }

                    @Override
                    public void onOtpAuthenticateRequest(Context context, MASOtpAuthenticationHandler masOtpAuthenticationHandler) {
                        //skip for now
                    }
                }
        );

        MASUser.login(new MASCallback<MASUser>() {
            @Override
            public void onSuccess(MASUser result) {
                Log.w("TUTORIAL", "Logged in as " + MASDevice.getCurrentDevice().getIdentifier());

                //getFlightList();

            }

            @Override
            public void onError(Throwable e) {

                Log.w("TUTORIAL", "Login failure: " + e);
                String error = e.getLocalizedMessage();
                txtResponse.setText(error);
            }
        });

    }

    //
    // User Logout
    //
    public void userLogout(View v) {

        MASUser.getCurrentUser().logout(new MASCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                showMessage("User successfully logged out",1);
            }

            @Override
            public void onError(Throwable e) {
                showMessage("Error during user logout: " + e.getLocalizedMessage(),1);
            }
        });

    }

    //
    // De-register the device
    //
    public void deregDevice(View v) {



        MASDevice.getCurrentDevice().resetLocally();
        MASDevice.getCurrentDevice().deregister(new MASCallback<Void>() {
            @Override
            public void onSuccess(Void object) {
                //The device is successfully de-registered.
                showMessage("The device has been de-registered!",1);
            }

            @Override
            public void onError(Throwable e) {
                showMessage("Error during device de-registration: "+ e.getLocalizedMessage(),1);
            }
        });

    }

    //
    // Uses MAS SDK to call a protected endpoint on the CA API Gateway
    //
    public void callAPI(View v) {

        //Endpoint (API) to be called
        String path = "/protected/resource/products";

        Uri.Builder uriBuilder = new Uri.Builder().encodedPath(path);
        uriBuilder.appendQueryParameter("operation", "listProducts");
        uriBuilder.appendQueryParameter("pName2", "pValue2");

        MASRequest.MASRequestBuilder requestBuilder = new MASRequest.MASRequestBuilder(uriBuilder.build());
        requestBuilder.header("hName1", "hValue1");
        requestBuilder.header("hName2", "hValue2");
        MASRequest request = requestBuilder.get().build();

        MAS.invoke(request, new MASCallback<MASResponse<JSONObject>>() {
            @Override
            public Handler getHandler() {
                return new Handler(Looper.getMainLooper());
            }

            @Override
            public void onSuccess(MASResponse<JSONObject> result) {
                try {
                    List<String> objects = parseProductListJson(result.getBody().getContent());
                    String objectString = "";
                    int size = objects.size();
                    for (int i = 0; i < size; i++) {
                        objectString += objects.get(i);
                        if (i != size - 1) {
                            objectString += "\n";
                        }
                    }

                    txtResponse.setText(objectString);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage());
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, e.getMessage());
            }
        });

    }

    public void showMessage(final String message, final int toastLength) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, toastLength).show();
            }
        });
    }

    private static List<String> parseProductListJson(JSONObject json) throws JSONException {
        try {
            List<String> objects = new ArrayList<>();
            JSONArray items = json.getJSONArray("products");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = (JSONObject) items.get(i);
                Integer id = (Integer) item.get("id");
                String name = (String) item.get("name");
                String price = (String) item.get("price");
                objects.add(id + ": " + name + ", $" + price);
            }
            return objects;
        } catch (ClassCastException e) {
            throw (JSONException) new JSONException("Response JSON was not in the expected format").initCause(e);
        }
    }

}
