package ms.firefightercar;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Activity;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.github.controlwear.virtual.joystick.android.JoystickView;


public class VideoViewActivity extends Activity {
    private WebView webView;
    private Button button;
    private Button stopButton;
    private JoystickView speedJoystick,steeringJoystick;
    private static final int REFRESH_INTERVAL = 200;
    private TextView liveInfo1,liveInfo2;
    private String IP;
    private final int motor_port = 8089;
    private final int camera_port = 8081;
    private final static String TAG = VideoViewActivity.class.getSimpleName();

    private boolean en = true;
    private ConcurrentLinkedQueue<JSONObject> q;
    private Socket s = null;

    private SpeechRecognizer mGoogleSr = null;
    private String result;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videoview_main);
        IP = getIntent().getStringExtra("IP");
        q = new ConcurrentLinkedQueue<JSONObject>();
        webView = (WebView) findViewById(R.id.webview);
        webView.loadUrl("http://"+IP+":"+camera_port);
        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                Toast.makeText(view.getContext(),"Video Stream error, go back and retry",Toast.LENGTH_LONG).show();
                speedJoystick.setVisibility(View.INVISIBLE);
                steeringJoystick.setVisibility(View.INVISIBLE);
                //liveInfo.setVisibility(View.INVISIBLE);
                speedJoystick.setEnabled(false);
                steeringJoystick.setEnabled(false);
                super.onReceivedError(view,request,error);
                Intent myIntent = new Intent(VideoViewActivity.this,
                        MainActivity.class);
                en = false;
                //startActivity(myIntent);
            }
            @Override
            public void onPageFinished(WebView view,String url) {
                Log.d(TAG,"Page finished loading");
            }
        });

        speedJoystick = (JoystickView)findViewById(R.id.speedJoystick);
        speedJoystick.setOnMoveListener(this::setSpeed,REFRESH_INTERVAL);
        speedJoystick.setButtonDirection(1);

        steeringJoystick = (JoystickView)findViewById(R.id.steeringJoystick);
        steeringJoystick.setOnMoveListener(this::setSteering,REFRESH_INTERVAL);






        button = (Button)findViewById(R.id.button);
        stopButton = (Button)findViewById(R.id.stopButton);
        stopButton.setVisibility(View.INVISIBLE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this,"NONONOONONONONONONONO",Toast.LENGTH_LONG).show();
            button.setVisibility(View.INVISIBLE);
        }
        button.setOnClickListener((arg0) -> {

            startGoogleSr();
            button.setVisibility(View.INVISIBLE);
            stopButton.setVisibility(View.VISIBLE);
            speedJoystick.setEnabled(false);
            steeringJoystick.setEnabled(false);


        });

        stopButton.setOnClickListener((arg0) -> {
            cancelRecognizing();
            stopButton.setVisibility(View.INVISIBLE);
            button.setVisibility(View.VISIBLE);
            steeringJoystick.setEnabled(true);
            speedJoystick.setEnabled(true);
            Toast.makeText(this,result,Toast.LENGTH_LONG).show();
        });
        initGoogleSr(this);



        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("sensor");

        liveInfo1 = (TextView) findViewById(R.id.liveInfo1);
        liveInfo2 = (TextView) findViewById(R.id.liveInfo2);
        liveInfo1.setTextColor(Color.WHITE);
        liveInfo2.setTextColor(Color.WHITE);
        // Read from the database
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                    //Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    HashMap<String,Object> map = (HashMap<String, Object>) dataSnapshot.getValue();
                JSONObject j = null;
                j = new JSONObject(map);
                String lpg,co_level,co_leak,smoke,distance,temperature,humidity;
                    liveInfo1.setText("Sensor data:\nLPG: "+map.get("lpg")+"ppm\nCO_LEVEL: "+map.get("co_level")+"ppm\nSmoke level: "+map.get("smoke")+"ppm\nCO LEAKAGE: "+map.get("co_leak"));
                    liveInfo2.setText("Sensor data:\nTemperature: "+map.get("temperature")+" C\nHumidity: "+map.get("humidity")+" %\nDistance from nearest front object: "+map.get("distance")+" cm");


            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }
    void initGoogleSr(Context context) {
        mGoogleSr = SpeechRecognizer.createSpeechRecognizer(context);
        mGoogleSr.setRecognitionListener(new GoogleSrListener());
    }

    void startGoogleSr() {
        if (mGoogleSr != null) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            mGoogleSr.startListening(intent);
        }
    }
    void cancelRecognizing() {
        if (mGoogleSr != null) {
            mGoogleSr.cancel();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        //Thread t = new Thread(this::startSocket);
        //t.start();
        /*try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }*/
        en = true;
        new Thread(this::run).start();
    }

    @Override
    public void onBackPressed() {
        Log.v(TAG,"I pushed back button");
        cancelRecognizing();
        en = false;
        this.onDestroy();
        super.onBackPressed();
        mGoogleSr.destroy();
        try {
            finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        //new Thread(this::stopSocket).start();
    }
    @Override
    protected void onStop() {
        super.onStop();
        //new Thread(this::stopSocket).start();
        en = false;
        cancelRecognizing();
        try {
            finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
    private void doVocalCommand(String result) {
        try {
            if (result != null) {
                Toast.makeText(this,result,Toast.LENGTH_LONG).show();
                JSONObject j = new JSONObject();
                String[] tokens = result.split(" ");
                if (tokens[0].equals("go") && tokens.length == 2) {
                    if (tokens[1].equals("left")) {
                        j.put("operation", "set_steering");
                        j.put("direction", 1);
                        j.put("steering", 100);
                    }
                    else if(tokens[1].equals("right")) {
                        j.put("operation", "set_steering");
                        j.put("direction", 1);
                        j.put("steering", 100);
                    }
                    else if(tokens[1].equals("forward")) {
                        j.put("operation", "set_speed");
                        j.put("direction", 1);
                        j.put("speed", 50);
                    }
                    else if(tokens[1].equals("back")) {
                        j.put("operation", "set_speed");
                        j.put("direction", 0);
                        j.put("speed", 50);
                    }


                }
                else  {
                    j.put("operation","stop");
                }
                if(j.length()>0) {
                    Log.e(TAG,j.toString());
                    q.add(j);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void setSpeed(int angle,int strength) {
        try {
            JSONObject j = new JSONObject();
            int speed = strength;
            int direction = 1;
            if ( angle == 270) direction = 0;
            j.put("operation", "set_speed");
            j.put("direction",direction);
            j.put("speed",speed);

            if(en)
            {
                //mBoundService.sendMessage(j.toString());
                //new SendJSON().execute(j);
                q.add(j);
                Log.d(TAG,"Speed: "+speed+", direction:"+(direction == 1? "forward":"backward"));
            }
            //new MyTask(IP,motor_port).execute(j);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
    private void setSteering(int angle,int strength) {
        try {
            JSONObject j = new JSONObject();

            int steering = (int) Math.abs(strength*Math.tan(Math.toRadians(angle)));
            int direction = 0;
            if ( angle > 90 && angle < 270) direction = 1;
            j.put("operation", "set_steering");
            j.put("direction",direction);
            j.put("steering",steering);


            //new MyTask(IP,motor_port).execute(j);
            if(en)
            {
                q.add(j);

                //Log.e(TAG, String.valueOf(q.size()));
                //new SendJSON().execute(j);
                //mBoundService.sendMessage(j.toString());
                Log.d(TAG,"Steering: "+steering+", direction:"+(direction == 1 ? "left":"right"));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void startSocket() {
        try {
            if (s == null)
                s = new Socket(IP,motor_port);
        } catch (IOException e) {
            e.printStackTrace();
            s = null;
        }
    }
    private void stopSocket() {
        try {
            if (s!=null)
                s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            s = null;
        }
    }
    public void run() {
        try {
            Socket s = new Socket(IP,motor_port);

            PrintWriter o = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8),true);
            while(en) {

                //Log.e(TAG, String.valueOf(q.size()));
                if(q.peek()!=null) {
                    Log.e(TAG,"HERE1");
                    o.println(q.poll().toString());
                    o.flush();

                    Log.e(TAG,"HERE2");
                }
            }
            s.close();

        } catch (IOException e) {
            Toast.makeText(this,"Cannot connect to remote car",Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            //Toast.makeText(VideoViewActivity.this,"Couldn't send command",Toast.LENGTH_LONG).show();
            return ;
        }
    }
    public class GoogleSrListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {
        }
        @Override
        public void onBeginningOfSpeech() {
        }
        @Override
        public void onRmsChanged(float rmsdB) {
        }
        @Override
        public void onBufferReceived(byte[] buffer) {
        }
        @Override
        public void onEndOfSpeech() {

        }
        @Override
        public void onError(int error) {
            result = "Error";
            Log.v(TAG, ">>> onError : " + error);
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    Log.e(TAG, "ERROR_AUDIO");
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    Log.e(TAG, "ERROR_CLIENT");
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    Log.e(TAG, "ERROR_INSUFFICIENT_PERMISSIONS");
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    Log.e(TAG, "ERROR_NETWORK");
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    Log.e(TAG, "ERROR_NETWORK_TIMEOUT");
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    Log.e(TAG, "ERROR_RECOGNIZER_BUSY");
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    Log.e(TAG, "ERROR_SERVER");
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    Log.v(TAG, "ERROR_NO_MATCH");
                    Toast.makeText(VideoViewActivity.this,"Not a valid command",Toast.LENGTH_LONG).show();
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    Log.v(TAG, "ERROR_SPEECH_TIMEOUT");
                    break;
                default:
                    Log.v(TAG, "ERROR_UNKOWN");
            }
        }
        @Override
        public void onPartialResults(Bundle partialResults) {
        }
        @Override
        public void onResults(Bundle results) {
            List<String> resultList = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (resultList != null) {
                String text = resultList.get(0);
                result = text;
                speedJoystick.setEnabled(true);
                steeringJoystick.setEnabled(true);
                button.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.INVISIBLE);
                doVocalCommand(result);
            }
            else {
                result = "";
            }
            //Toast.makeText(VideoViewActivity.this,result,Toast.LENGTH_LONG).show();
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    }
}