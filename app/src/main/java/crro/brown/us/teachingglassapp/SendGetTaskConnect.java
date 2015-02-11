package crro.brown.us.teachingglassapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Created by David on 2/2/15.
 */
public class SendGetTaskConnect extends AsyncTask<String, Integer, String> {
    private MainActivity _activity;

    public SendGetTaskConnect(MainActivity activity) {
        _activity = activity;
    }

    @Override
    protected String doInBackground(String... params) {
        //We establish a session and then pop up the code in a dialog activity that the user can easily dismiss.
        try {
            HttpClient httpClient = new HttpClient();
            httpClient.start();
            ContentResponse resp = httpClient.GET("http://1-dot-firm-aria-738.appspot.com/googleglassserver?SESSION=NEW");
            int status = resp.getStatus();
            String result = null;
            if (status == 200) {
                result = resp.getContentAsString();
            }
            return result;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void onPostExecute(String result) {
        // Do something when finished.
        //Lets see if we can directly call the method
        if (result != null) {
            _activity.displayCode(result);
        }
    }
}