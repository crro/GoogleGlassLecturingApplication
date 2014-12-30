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
 * This class is in charge of performing GET requests in the doInBackground method
 * and then modifying the UI in the onPostExecute method. The list of performed requests
 * is available in the README
 * Created by David on 09/30/14.
 */
public class SendGetTask extends AsyncTask<String, Integer, String> {
    private PresentationModeActivity _presentation;
    private InputStream _imageIS;
    private LocalBroadcastManager _lbm;
    private ArrayList<ArrayList<String>> _notes;
    private int _slideIndex;
    private int _currentIndex;

    public SendGetTask(PresentationModeActivity presentation, LocalBroadcastManager lbm) {
        _presentation = presentation;
        _lbm = lbm;
        _notes = presentation.getNotes();
        _slideIndex = presentation.getSlideIndex();
        _currentIndex = presentation.getCurrentIndex();
    }

    @Override
    protected String doInBackground(String... params) {

        try {
            HttpClient httpClient = new HttpClient();
            httpClient.start();
            int status;
            String actionRequested = params[0];
            ContentResponse response;
            if (actionRequested.equals("IMAGE")) {
                response = httpClient.GET("http://googleglassserver.herokuapp.com?Action=IMAGE&Equation=" + params[1]);
            } else if (actionRequested.equals("INDEX")) {
                response = httpClient.GET("http://googleglassserver.herokuapp.com?Action=INDEX");
            } else {
                return null;
            }

            String responseTxt = "No Action";
            if (actionRequested.equals("INDEX")) {
                //We are requesting an update on the index
                status = response.getStatus();
                if (status != 200) {
                    responseTxt = "Unabled to fetch notes...";
                } else {
                    String text = response.getContentAsString();
                    System.out.println(text);
                    int newSlideIndex = Integer.parseInt(text.trim());
                    //This check is done to see if we need to update, the > 0 check
                    //is done becase -1 is returned if there is no new information
                    if (newSlideIndex != _presentation.getSlideIndex() && newSlideIndex > 0) {
                        //We need to update view in the slide
                        _presentation.setSlideIndex(newSlideIndex);
                        _presentation.setCurrentIndex(0);
                        responseTxt = "INDEX UPDATE";
                    } else {
                        responseTxt = "INDEX NO UPDATE";
                    }
                }
            } else if (actionRequested.equals("IMAGE")) {
                //We request an image
                status = response.getStatus();
                if (status != 200) {
                    responseTxt = "Unabled to fetch notes...";
                } else {
                    byte[] content = response.getContent();
                    _imageIS = new ByteArrayInputStream(content);
                    responseTxt = "IMAGE";
                    return responseTxt;
                }
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
        // Do something when finished.
        if (!result.equals("No Action")) {
            //We update in case of an image
            if (result.equals("IMAGE")) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                Bitmap bitmapImage = BitmapFactory.decodeStream(_imageIS, null, options);
                //Set the image visible. I might need to call inSampleSize() to save memory
                _presentation.setNoteTVVisibility(View.GONE);
                _presentation.setEquationsVisibility(View.VISIBLE);
                _presentation.setEquationImage(bitmapImage);
                return;
            } else if (result.equals("INDEX UPDATE")){
                //We update in case of an index update.
                //We send the broadcast to send a new async task and change the slide
                if (_notes.get(_slideIndex).get(0).charAt(0) == '<') {
                    //Then we use the empty equation approach
                    String equation = _notes.get(_slideIndex).get(0);
                    new SendPostTask(_presentation).execute("GET IMAGE", equation.substring(2, equation.length() - 2));
                } else if (_notes.get(_slideIndex).get(0).contains("<<") && _notes.get(_slideIndex).get(0).contains(">>")) {
                    String[] notes = _notes.get(_slideIndex).get(0).split("<<");
                    new SendPostTask(_presentation).execute("GET IMAGE", notes[1].substring(0, notes[1].length() - 2));
                } else {
                    //no equation
                    _presentation.setNoteTVVisibility(View.VISIBLE);
                    _presentation.setEquationsVisibility(View.GONE);
                    _presentation.setNoteTVText(_notes.get(_slideIndex).get(0));
                }
                Intent updateIntent = new Intent();
                updateIntent.setAction(GlassConstants.UPDATE_FINISHED);
                _lbm.sendBroadcast(updateIntent);

                return;
            } else if (result.equals("INDEX NO UPDATE")) {
                //In case nothing happened, we simply do another request.
                Intent updateIntent = new Intent();
                updateIntent.setAction(GlassConstants.UPDATE_FINISHED);
                _lbm.sendBroadcast(updateIntent);
                return;
            }
        }
        //We set it to gone
        _presentation.setEquationsVisibility(View.GONE);
    }
}
