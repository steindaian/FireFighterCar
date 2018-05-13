package ms.firefightercar;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SocketService extends Service {
    public static final String SERVERIP = "192.168.137.184"; //your computer IP address should be written here
    public static final int SERVERPORT = 8089;
    OutputStreamWriter out = null;
    Socket socket;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        System.out.println("I am in Ibinder onBind method");
        return myBinder;
    }

    private final IBinder myBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        public SocketService getService() {
            Log.e(this.getClass().getSimpleName(),"Get service");
            return SocketService.this;

        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(this.getClass().getSimpleName(),"Create service");

    }

    public void IsBoundable(){
        Toast.makeText(this,"I bind like butter", Toast.LENGTH_LONG).show();
    }

    public void sendMessage(String message){
        if (out != null ) {
            new Thread(this::send).start();
        }
    }

    void send() {

            try {
                Socket socket = new Socket(SERVERIP,SERVERPORT);
                String m = "ASDA";
                System.out.println("in sendMessage"+m);
                out = new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8);
                out.write(m);
                out.flush();
                out.close();
                out = null;
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                return ;
            }
    }

    @Override
    public int onStartCommand(Intent intent,int flags, int startId){
        super.onStartCommand(intent, flags, startId);
        Log.e(this.getClass().getSimpleName(),"Start service");

        //  Toast.makeText(this,"Service created ...", Toast.LENGTH_LONG).show();
        Runnable connect = new connectSocket();
        new Thread(connect).start();
        return START_STICKY;
    }


    class connectSocket implements Runnable {

        @Override
        public void run() {


            try {
                //here you must put your computer's IP address.
                Log.e("TCP Client", "C: Connecting...");
                //create a socket to make the connection with the server

                socket = new Socket(SERVERIP, SERVERPORT);

                try {


                    //send the message to the server
                    //out = new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8);


                    Log.e("TCP Client", "C: Sent.");

                    Log.e("TCP Client", "C: Done.");


                }
                catch (Exception e) {

                    Log.e("TCP", "S: Error", e);

                }
            } catch (Exception e) {

                Log.e("TCP", "C: Error", e);

            }

        }

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            Log.e(this.getClass().getSimpleName(),"Closing socket");
            if(socket!=null)
                socket.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        socket = null;
    }


}