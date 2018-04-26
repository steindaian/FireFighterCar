package ms.firefightercar;

import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;


public class VideoViewActivity extends Activity {
    WebView webView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videoview_main);
        webView = (WebView) findViewById(R.id.webview);
        webView.loadUrl("http://192.168.137.184:8081");
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
    }




}