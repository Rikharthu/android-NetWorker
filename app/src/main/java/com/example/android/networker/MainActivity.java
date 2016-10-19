package com.example.android.networker;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = MainActivity.class.getSimpleName();

    private static final String URL="https://api.darksky.net/forecast/f001bb783cc27374e9f473499f3b196f/37.8267,-122.4233";
    private static final int PERMISSION_INTERNET = 12;
    private Button mDownloadButton;
    private TextView mOutputTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDownloadButton= (Button) findViewById(R.id.downloadButton);
        mOutputTextView= (TextView) findViewById(R.id.output);

        mDownloadButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        // do network stuff
        downloadData();
    }

    private void downloadData() {
        // 1. Check for permissions
        if(permissionsAllowed()){
            // we have permissions
            if(!isNetworkAvailable()){
                // no network connection
                createNetErrorDialog();
            }else{
                // network is available
                new DownloadWebpageTask().execute(URL);
            }
        }
    }

    // FIXME move to dialogfragment
    protected void createNetErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You need a network connection to use this application. Please turn on mobile network or Wi-Fi in Settings.")
                .setTitle("Unable to connect")
                .setCancelable(false)
                .setPositiveButton("Settings",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent i = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                                startActivity(i);
                            }
                        }
                )
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                               Log.d(TAG,"Internet is required for this action");
                                Toast.makeText(MainActivity.this
                                        , "Internet connection is required for this action"
                                        , Toast.LENGTH_SHORT).show();
                            }
                        }
                );
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");

    }

    private boolean isNetworkAvailable(){
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            Log.d(TAG,"You are connected! Connection type: "+networkInfo.getTypeName());
//            new DownloadWebpageTask().execute(stringUrl);
            return true;
        } else {
            Log.d(TAG,"No network connection!");
            return false;
        }
    }

    private boolean permissionsAllowed() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"No internet permission");
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Log.d(TAG,"Should show an explanation");
                Snackbar.make(findViewById(R.id.activity_main),
                        "This app requires Internet acces permissions",
                        Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.READ_CONTACTS},
                                PERMISSION_INTERNET);
                    }
                }).show();
            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        PERMISSION_INTERNET);

            }
            return false;
        }
        Log.d(TAG,"Everything is OK!");
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case PERMISSION_INTERNET: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG,"Permission granted!");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    downloadData();
                } else {
                    Log.d(TAG,"Permission denied!");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    // Uses AsyncTask to create a task away from the main UI thread. This task takes a
    // URL string and uses it to create an HttpUrlConnection. Once the connection
    // has been established, the AsyncTask downloads the contents of the webpage as
    // an InputStream. Finally, the InputStream is converted into a string, which is
    // displayed in the UI by the AsyncTask's onPostExecute method.
    private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            Log.d(TAG,"Starting download");
            // params comes from the execute() call: params[0] is the url.
            try {
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG,"Finished!");
            mOutputTextView.setText(result);
        }
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
// the web page content as a InputStream, which it returns as
// a string.
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        // Only display the first 500 characters of the retrieved
        // web page content.
        int len = 500;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(TAG, "The response is: " + response);
            is = conn.getInputStream();

            // Convert the InputStream into a string
//            String contentAsString = readIt(is, len);
            String contentAsString = readAll(is);
            return contentAsString;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    // Reads an InputStream and converts it to a String.
    public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Log.d(TAG,"Parsing data!");
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
        /* if it was an image
        Bitmap bitmap = BitmapFactory.decodeStream(is);
        ImageView imageView = (ImageView) findViewById(R.id.image_view);
        imageView.setImageBitmap(bitmap);
        */
    }

    public String readAll(InputStream stream){
        StringBuffer strBuffer = new StringBuffer();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        String line;
        try {
            while((line= reader.readLine())!=null){
                strBuffer.append(line);
            }
            reader.close();
            return strBuffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
