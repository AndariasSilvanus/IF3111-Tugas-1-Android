package com.example.andariassilvanus.tomandjerry;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

public class JerryMap extends FragmentActivity implements SensorEventListener {

    // MAP
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";

    // COMPASS
    // define the display assembly compass picture
    private ImageView image;
    // record the compass picture angle turned
    private float currentDegree = 0f;
    // device sensor manager
    private SensorManager mSensorManager;
    TextView tvHeading;

    // HTTP_REQUEST
    private String contents;
    private long valid_until;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jerry_map);
        setUpMapIfNeeded();
        setUpCompass();
//        Toast trop = Toast.makeText(JerryMap.this, "time now: "+valid_until, Toast.LENGTH_LONG);
//        trop.show();
//        double valid = valid_until - (System.currentTimeMillis()/1000);
//        if (valid<1800) {
//            while (valid > 0 && valid<1800) {
//                if (valid <= 0) {
//                    getRequest();
//                }
//                else {
//                    valid = valid_until - (System.currentTimeMillis() / 1000);
//                    Toast trep = Toast.makeText(JerryMap.this, (int) valid, Toast.LENGTH_LONG);
//                    trep.show();
//                }
//            }
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        // COMPASS
        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // COMPASS
        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // get the angle around the z-axis rotated
        float degree = Math.round(event.values[0]);
//        tvHeading.setText("Heading: " + Float.toString(degree) + " degrees");
        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);

        // how long the animation will take place
        ra.setDuration(210);
        // set the animation after the end of the reservation status
        ra.setFillAfter(true);
        // Start the animation
        image.startAnimation(ra);
        currentDegree = -degree;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }


    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        getRequest();
        mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location arg0) {
                // TODO Auto-generated method stub
                mMap.addMarker(new MarkerOptions().position(new LatLng(arg0.getLatitude(), arg0.getLongitude())).title("Your Position is Here"));
            }
        });
//        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
//        mMap.getUiSettings().setZoomGesturesEnabled(true);
    }

    private void setUpCompass() {
        image = (ImageView) findViewById(R.id.imageViewCompass);
        // TextView that will tell the user what degree is he heading
        tvHeading = (TextView) findViewById(R.id.tvHeading);
        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    public void scanQR(View v){
        try{
            Intent intent = new Intent(ACTION_SCAN);
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(intent, 0);
        } catch(ActivityNotFoundException e){
            showDialog(JerryMap.this, "No Scanner Found", "Download a scanner code activity?", "Yes", "No").show();
        }
        postRequest();
    }

    // alert dialog for downloadDialog
    private static AlertDialog showDialog(final Activity act, CharSequence title, CharSequence message, CharSequence bYes, CharSequence bNo){
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(act);
        downloadDialog.setTitle(title);
        downloadDialog.setMessage(message);
        downloadDialog.setPositiveButton(bYes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Uri uri = Uri.parse("market://search?q=pname:" + "com.google.zxing.client.android");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                try{
                    act.startActivity(intent);
                } catch(ActivityNotFoundException e){e.printStackTrace();}
            }
        });
        downloadDialog.setNegativeButton(bNo, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
            }
        });
        return downloadDialog.show();
    }

    //on ActivityResult method
    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        if(requestCode == 0){
            if(resultCode == RESULT_OK){
                //get the extras that are returned from the intent
                contents = intent.getStringExtra("SCAN_RESULT");
                String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
//                Toast toast = Toast.makeText(this, "Content:" + contents + " Format:" + format, Toast.LENGTH_LONG);
//                toast.show();
            }
        }
    }

    // Jerry Tracking

    // GetRequest
    public void getRequest() {
        GetJerry jerry = new GetJerry();
        jerry.execute("http://167.205.32.46/pbd/api/track?nim=13512022");
    }

    private class GetJerry extends AsyncTask<String, String, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... uri){
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(URI.create(uri[0]));
            StringBuilder sb = null;
            HttpResponse response;
            try{
                response = client.execute(request);
                HttpEntity entity = response.getEntity();

                BufferedHttpEntity buffEntity = new BufferedHttpEntity(entity);
                BufferedReader buffRead = new BufferedReader(new InputStreamReader(buffEntity.getContent()));
                String line;
                sb = new StringBuilder();
                while((line = buffRead.readLine()) != null){
                    sb.append(line);
                }
            } catch(IOException e){ e.printStackTrace();}

            JSONObject json = null;
            try {
                json = new JSONObject(sb.toString());
            } catch(JSONException e) {e.printStackTrace();}

            if(json != null) {
                contents = json.toString();
            }

            return json;
        }

        @Override
        protected void onPostExecute(JSONObject result){
            super.onPostExecute(result);
            LatLng jerry_whereabouts = new LatLng(0,0);
            try{
                double lat = Double.parseDouble(result.getString("lat"));
                double lon = Double.parseDouble(result.getString("long"));
                valid_until = Long.parseLong(result.getString("valid_until"))*1000;
                jerry_whereabouts = new LatLng(lat, lon);

                new CountDownTimer((valid_until-System.currentTimeMillis()),1000){
                    public void onTick (long r){
                        long ETA = (valid_until - System.currentTimeMillis()) / 1000;
                        tvHeading.setText(String.valueOf(
                                (ETA/3600) + "jam " + (ETA%3600 / 60) + " menit " + (ETA%3600 %60) + " detik ketika Jerry berpindah lokasi"
                        ));
                    }

                    @Override
                    public void onFinish() {
                        setUpMapIfNeeded();
                    }
                }.start();
            } catch(JSONException e){e.printStackTrace();}
            //setUpMap();

            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(jerry_whereabouts).title("Jerry is Here!"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(jerry_whereabouts, 17));
        }
    }

    // PostRequest
    public void postRequest(){
        PostToken pt = new PostToken();
        pt.execute();
    }
    private class PostToken extends AsyncTask<Void, String, String> {
        @Override
        protected String doInBackground(Void... uri) {
            String content = "";
            try {
                HttpPost post = new HttpPost("http://167.205.32.46/pbd/api/catch");
                JSONObject json = new JSONObject();
                json.put("nim", "13512022");
                json.put("token", contents);
                StringEntity SE = new StringEntity(json.toString());
                SE.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                post.setEntity(SE);
                HttpClient client = new DefaultHttpClient();
                HttpResponse response = client.execute(post);
                if (response != null) {
                    HttpEntity entity = response.getEntity();
                    BufferedHttpEntity buffEnt = new BufferedHttpEntity(entity);
                    BufferedReader buffRead = new BufferedReader(new InputStreamReader(buffEnt.getContent()));
                    String line;
                    StringBuilder SB = new StringBuilder();
                    while ((line = buffRead.readLine()) != null) {
                        SB.append(line);
                    }
                    json = new JSONObject(SB.toString());
                    if (json.getInt("code")==200)
                        content = "Jerry berhasil ditangkap!";
                    else
                        content = "Jerry gagal ditangkap!";
//                    content = SB.toString();
                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
            return content;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Toast trep = Toast.makeText(JerryMap.this, "Result: "+result, Toast.LENGTH_LONG);
            trep.show();
        }
    }
}
