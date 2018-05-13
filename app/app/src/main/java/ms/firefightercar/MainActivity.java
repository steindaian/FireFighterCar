package ms.firefightercar;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity {

    Button button;
    private final String rpiMac = "50:2b:73:e0:26:85";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the layout from video_main.xml
        setContentView(R.layout.activity_main);

        // Locate the button in activity_main.xml
        button = (Button) findViewById(R.id.MyButton);

        //get raspberry pi ip adress
        String rpi_ip = "192.168.137.184";//getRpiIp(rpiMac);


        //new MyTask().execute();
        // Capture button clicks
        button.setOnClickListener((arg0) -> {
                if(rpi_ip == null) {
                    Toast.makeText(this,"Raspberry Pi not found on local network",Toast.LENGTH_LONG).show();
                    this.recreate();
                }
                else {
                    // Start NewActivity.class
                    Intent myIntent = new Intent(MainActivity.this,
                            VideoViewActivity.class);
                    myIntent.putExtra("IP",rpi_ip);
                    startActivity(myIntent);
                }

        });
    }
    private class MyTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                Socket s = new Socket("192.168.137.184",8089);
                JSONObject j = new JSONObject();
                for(int i=0;i<=2;i++) {
                    j.put("operation", "set_speed");
                    j.put("speed", 10);
                    j.put("direction", 1);

                    Log.e(this.getClass().getSimpleName(), j.toString());
                    PrintWriter o = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8),true);
                    o.println(j.toString());
                    o.flush();
                }
                //s.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                return null;
            }

        }
    }
    @Nullable
    private String getRpiIp(String rpi_mac) {
        BufferedReader bufRead = null;

        try {
            bufRead = new BufferedReader(new FileReader("/proc/net/arp"));
            String fileLine;
            while ((fileLine = bufRead.readLine()) != null) {
                String[] splitted = fileLine.split(" +");

                if ((splitted != null) && (splitted.length >= 4)) {
                    //Log.e(this.getClass().getSimpleName(),splitted[0]+" : "+splitted[3]);
                    String mac = splitted[3];
                    if (mac.equals(rpi_mac)) {
                        boolean isReachable = pingCmd(splitted[0]);
                        if (isReachable) {
                            return splitted[0];
                        }
                        else return null;
                    }
                }
            }
        } catch (Exception e) {

        } finally {
            try {
                bufRead.close();
            } catch (IOException e) {

            }
        }

        return null;
    }

    private boolean pingCmd(String ip){
        Runtime runtime = Runtime.getRuntime();
        try
        {
            Process  mIpAddrProcess = runtime.exec("/system/bin/ping -c 1 "+ip);
            int mExitValue = mIpAddrProcess.waitFor();
            if(mExitValue==0){
                return true;
            }else{
                return false;
            }
        }
        catch (InterruptedException ignore)
        {
            ignore.printStackTrace();
            System.out.println(" Exception:"+ignore);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.out.println(" Exception:"+e);
        }
        return false;
    }

}