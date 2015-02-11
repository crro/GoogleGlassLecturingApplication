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
            ContentResponse content = request.param("ACTION", actionRequested).param("SESSION", _sessionCode).content(new StringContentProvider(actionRequested)).send();
            String responseTxt = "No Action";

            //Only in the case of the notes do we expect to do smth with the response
            // This is now in SendGetTask I believe
//            if (actionRequested.equals("Action Notes")) {
//                status = response.getStatus();
//                if (status != 200) {
//                    responseTxt = "Unable to fetch notes...";
//                } else {
//                    String text = response.getContentAsString();
//                    System.out.println(text);
//                    String[] wordChunk = text.split("\n");
//                    int len = wordChunk.length;
//                    int slideNum = -1;
//                    ArrayList<ArrayList<String>> notes = new ArrayList<ArrayList<String>>();
//                    ArrayList<String> slideNotes = new ArrayList<String>();
//                    String note = "";
//                    for (int i = 0; i < len; i++) {
//                        if (wordChunk[i].contains("PROCSLIDE")) {
//                            if (slideNum != -1) {
//                                if (!note.equals("")) {
//                                    slideNotes.add(note);
//                                }
//                                notes.add(slideNotes);
//                                slideNotes  = new ArrayList<String>();
//                            }
//                            slideNum++;
//                            note.concat(wordChunk[i].substring(10) + "\n");
//                        } else if (!wordChunk[i].equals("")) {
//                            note.concat(wordChunk[i]+"\n");
//                        } else {
//                            //It's "", so we add the entire note
//                            if (!note.equals("")) {
//                                slideNotes.add(note);
//                                note = "";
//                            }
//
//                        }
//                    }
//                    //We need to add the last slide
//                    if (!note.equals("")) {
//                        slideNotes.add(note);
//                    }
//                    notes.add(slideNotes);
//                    _presentation.setCurrentIndex(0);
//                    _presentation.setSlideIndex(0);
//                    responseTxt = notes.get(0).get(0);
//                    _presentation.setNotes(notes);
//                }
//            }
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