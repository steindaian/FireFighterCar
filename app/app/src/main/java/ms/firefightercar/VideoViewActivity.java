package ms.firefightercar;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.IBinder;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.MediaController;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.github.controlwear.virtual.joystick.android.JoystickView;


public class VideoViewActivity extends Activity {
    private WebView webView;
    private JoystickView speedJoystick,steeringJoystick;
    private static final int REFRESH_INTERVAL = 200;
    private TextView liveInfo;
    private String IP;
    private final int motor_port = 8089;
    private final int camera_port = 8081;
    private final static String TAG = VideoViewActivity.class.getSimpleName();
    private boolean mIsBound;
    private SocketService mBoundService;

    private boolean en = true;
    private ConcurrentLinkedQueue<JSONObject> q;
    private Socket s = null;
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
                startActivity(myIntent);
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


        /*startService(new Intent(VideoViewActivity.this,SocketService.class));
        doBindService();*/


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
        en = false;
        this.onDestroy();
        super.onBackPressed();

        //new Thread(this::stopSocket).start();
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
    /*private class SendJSON extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                JSONObject j = (JSONObject)objects[0];
                PrintWriter o = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8),true);
                o.println(j.toString());
                o.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                Toast.makeText(VideoViewActivity.this,"Couldn't send command",Toast.LENGTH_LONG).show();
                return null;
            }

        }
    }*/
    @Override
    protected void onStop() {
        super.onStop();
        //new Thread(this::stopSocket).start();
        en = false;
        /*doUnbindService();
        stopService(new Intent(this,SocketService.class));*/
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
    private ServiceConnection mConnection = new ServiceConnection() {
        //EDITED PART
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
            mBoundService = ((SocketService.LocalBinder)service).getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            mBoundService = null;
        }

    };


    private void doBindService() {
        bindService(new Intent(VideoViewActivity.this, SocketService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        if(mBoundService!=null){
            mBoundService.IsBoundable();
        }
    }


    private void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }
}