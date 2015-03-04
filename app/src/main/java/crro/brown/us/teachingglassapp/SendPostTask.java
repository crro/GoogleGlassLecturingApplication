package crro.brown.us.teachingglassapp;

import android.os.AsyncTask;
import android.view.View;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * This class is in charge of performing POST requests in the doInBackground method
 * and then modifying the UI in the onPostExecute method. The list of performed requests
 * is available in the README
 * Created by David on 09/30/14.
 */
public class SendPostTask extends AsyncTask<String, Integer, String> {

    private PresentationModeActivity _presentation;
    private String _sessionCode;
    private HttpClient _httpClient;

    public SendPostTask(PresentationModeActivity presentation, String sessionCode, HttpClient hC) {
        _presentation = presentation;
        _sessionCode = sessionCode;
        _httpClient = hC;
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            int status;
            Request request = _httpClient.POST("http://1-dot-firm-aria-738.appspot.com/googleglassserver");
            String actionRequested = params[0];
            ContentResponse content;
            if (actionRequested.equals("INDEX")) {
                content = request.param("ACTION", actionRequested).param("SESSION", _sessionCode).param("INDEX", params[1]).content(new StringContentProvider(actionRequested)).send();
            } else {
                content = request.param("ACTION", actionRequested).param("SESSION", _sessionCode).content(new StringContentProvider(actionRequested)).send();
            }

            String responseTxt = "No Action";
            status = content.getStatus();
            if (status != 200) {
                responseTxt = "Unable to fetch notes...";
            } else {

            }
            return responseTxt;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    protected void onPostExecute(String result) {
        if (!result.equals("No Action")) {
            //Update the notes
            _presentation.setNoteTVText(result);
            _presentation.setNoteTVVisibility(View.VISIBLE);
            _presentation.setEquationsVisibility(View.GONE);
            return;
        }
        //We set it to gone
        _presentation.setEquationsVisibility(View.GONE);
    }
}