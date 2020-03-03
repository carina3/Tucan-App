package moca.clockdraw;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Shows the Info/Copyright/FAQ of the app.
 */
public class InfoWebView extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_web_view);

        WebView infoView = (WebView) findViewById(R.id.infoWebView);

        //load html asset file:
        AssetManager man = getAssets();
        InputStream str = null;
        String htmlString = "";
        try {
            str = man.open("info.html");
            htmlString = StreamToString(str);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("tucan", "info.html was not found!");
        }

        if(!htmlString.isEmpty()) infoView.loadData(htmlString, "text/html", "UTF-8");
    }

    /**
     * Converts an input stream to string.
     * resource: https://inducesmile.com/android-tutorials-for-nigerian-developer/loading-html-file-from-assets-folder-in-android-webview/
     * @param in
     * @return
     * @throws IOException
     */
    public static String StreamToString(InputStream in) throws IOException {
        if(in == null) {
            return "";
        }
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        try {
            Reader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } finally {
        }
        return writer.toString();
    }

}
