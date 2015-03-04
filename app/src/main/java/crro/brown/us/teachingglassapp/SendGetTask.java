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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
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
    private String _sessionCode;
    private HttpClient _httpClient;
    private ImageCache _imageCache;

    //This constructor is for all the other types of requests
    public SendGetTask(PresentationModeActivity presentation, LocalBroadcastManager lbm, String sessionCode, HttpClient hC) {
        _presentation = presentation;
        _lbm = lbm;
        _notes = presentation.getNotes();
        _slideIndex = presentation.getSlideIndex();
        _currentIndex = presentation.getCurrentIndex();
        _sessionCode = sessionCode;
        _httpClient = hC;
        _imageCache = presentation.getEquationCache();
    }

    @Override
    protected String doInBackground(String... params) {

        try {
            int status;
            String actionRequested = params[0];
            ContentResponse response;
            if (actionRequested.equals("IMAGE")) {
                response = _httpClient.GET("http://1-dot-firm-aria-738.appspot.com/googleglassserver?ACTION=IMAGE&EQUATION=" + params[1] +
                        "&SESSION=" + _sessionCode);
            } else if (actionRequested.equals("INDEX")) {
                response =  _httpClient.GET("http://1-dot-firm-aria-738.appspot.com/googleglassserver?ACTION=INDEX" +
                        "&SESSION=" + _sessionCode);
            } else if (actionRequested.equals("NOTES")){
                response =  _httpClient.GET("http://1-dot-firm-aria-738.appspot.com/googleglassserver?ACTION=NOTES" +
                        "&SESSION=" + _sessionCode);
            } else {
                response = null;
            }
            String responseTxt = "No Action";
            if (actionRequested.equals("INDEX")) {
                //We are requesting an update on the index
                status = response.getStatus();
                if (status != 200) {
                    responseTxt = "Unabled to fetch notes...";
                } else {
                    String text = response.getContentAsString();
                    //System.out.println(text);
                    int newSlideIndex = Integer.parseInt(text.trim());
                    //This check is done to see if we need to update, the > 0 check
                    //is done becase -1 is returned if there is no new information
                    if (newSlideIndex != _presentation.getSlideIndex() && newSlideIndex > -1) {
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
            } else if (actionRequested.equals("NOTES")) {
                //Here we process the notes and request the images.
                status = response.getStatus();
                if (status != 200) {
                    responseTxt = "Unable to fetch notes...";
                } else {
                    String text = response.getContentAsString();
                    //System.out.println(text);
                    String[] wordChunk = text.split("\n");
                    ArrayList<String> wordChunks = new ArrayList<String>(Arrays.asList(wordChunk));
                    int len = wordChunks.size();
                    int slideNum = -1;
                    ArrayList<ArrayList<String>> notes = new ArrayList<ArrayList<String>>();
                    ArrayList<String> slideNotes = new ArrayList<String>();
                    String note = "";
                    for (int i = 0; i < len; i++) {
                        String lineNote = wordChunks.get(i);
                        if (lineNote.length() > 229) {
                            StringBuilder sBOne = new StringBuilder();
                            StringBuilder sBTwo = new StringBuilder();
                            String[] words = lineNote.split(" ");
                            int count = 0;
                            for (int j = 0; j < words.length; j++) {
                                if (count == 30) {
                                    break;
                                }
                                sBOne.append(words[j]).append(" ");
                                count++;
                            }
                            for (;count < words.length; count++) {
                                sBTwo.append(words[count]).append(" ");
                            }
                            lineNote = sBOne.toString();
                            wordChunks.add(i+1, "");
                            wordChunks.add(i+2, sBTwo.toString());
                        }
                        boolean split = false;
                        processString(lineNote);
                        if (lineNote.contains("PROCSLIDE")) {
                            if (slideNum != -1) {
                                if (!note.equals("")) {
                                    slideNotes.add(note);
                                }
                                notes.add(slideNotes);
                                slideNotes = new ArrayList<String>();
                                //I think here the note has to reset
                                note = "";
                            }
                            slideNum++;
                            note = note.concat(lineNote.substring(10) + "\n");
                        } else if (!lineNote.equals("")) {
                            note = note.concat(lineNote+"\n");
                        } else {
                            //It's "", so we add the entire note
                            if (!note.equals("")) {
                                slideNotes.add(note);
                                note = "";
                            }

                        }
                    }
                    //We need to add the last slide
                    if (!note.equals("")) {
                        slideNotes.add(note);
                    }
                    notes.add(slideNotes);
                    _presentation.setCurrentIndex(0);
                    _presentation.setSlideIndex(0);
                    String firstNote = notes.get(0).get(0);
                    if (firstNote.contains("<<") && firstNote.contains(">>")) {
                        String[] firstNotes = note.split("<<");
                        String equation = firstNotes[1].replaceAll("(\\r|\\n)", "");
                        equation = URLEncoder.encode(equation.substring(0, equation.length() - 2),"UTF-8");
                        Bitmap imageB = _imageCache.get(equation);
                        _presentation.setNoteTVVisibility(View.GONE);
                        _presentation.setEquationsVisibility(View.VISIBLE);
                        _presentation.setEquationImage(imageB);
                        responseTxt = "Fetching the image";
                    } else {
                        responseTxt = firstNote;
                    }
                    _presentation.setNotes(notes);
                    new SendGetTask(_presentation, _lbm, _sessionCode, _httpClient).execute("INDEX");
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
        if (result.equals("")) {
            return;
        }
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
                _slideIndex = _presentation.getSlideIndex();
                if (_notes.get(_slideIndex).get(0).charAt(0) == '<') {
                    //Then we use the empty equation approach
                    String equation = null;
                    try {
                        equation = _notes.get(_slideIndex).get(0).replaceAll("(\\r|\\n)", "");
                        equation = URLEncoder.encode(equation.substring(2, equation.length() - 2), "UTF-8");
                        //new SendGetTask(_presentation, _lbm, _sessionCode, _httpClient).execute("IMAGE", equation);
                        Bitmap imageB = _imageCache.get(equation);
                        _presentation.setNoteTVVisibility(View.GONE);
                        _presentation.setEquationsVisibility(View.VISIBLE);
                        _presentation.setEquationImage(imageB);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else if (_notes.get(_slideIndex).get(0).contains("<<") && _notes.get(_slideIndex).get(0).contains(">>")) {
                    String[] notes = _notes.get(_slideIndex).get(0).split("<<");
                    String equation;
                    try {
                        equation = notes[1].replaceAll("(\\r|\\n)", "");
                        equation = URLEncoder.encode(equation.substring(0, equation.length() - 2), "UTF-8");
                        //new SendGetTask(_presentation, _lbm, _sessionCode, _httpClient).execute("IMAGE", equation);
                        Bitmap imageB = _imageCache.get(equation);
                        _presentation.setNoteTVVisibility(View.GONE);
                        _presentation.setEquationsVisibility(View.VISIBLE);
                        _presentation.setEquationImage(imageB);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

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
            } else {
                _presentation.setNoteTVText(result);
                _presentation.setNoteTVVisibility(View.VISIBLE);
                _presentation.setEquationsVisibility(View.GONE);
                return;
            }
        }
        //We set it to gone
        _presentation.setEquationsVisibility(View.GONE);
    }

    /**
     * This method looks at the string and requests an image if necessary and then stores it in the cache
     */
    private int processString(String line) {
        boolean eqRequest = false;
        String equation = null;
        if (line.length() == 0) {
            return -1;
        }
        if (line.charAt(0) == '<') {
            //Then we use the empty equation approach
            try {
                equation = line.replaceAll("(\\r|\\n)", "");
                equation = URLEncoder.encode(equation.substring(2, equation.length() - 2), "UTF-8");
                eqRequest = true;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        } else if (line.contains("<<") && line.contains(">>")) {
            String[] notes = line.split("<<");
            try {
                equation = notes[1].replaceAll("(\\r|\\n)", "");
                equation = URLEncoder.encode(equation.substring(0, equation.length() - 2), "UTF-8");
                eqRequest = true;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }
        //If there is no equation we dont do anything and go on with our lives.
        if (eqRequest) {
            try {
                ContentResponse response = _httpClient.GET("http://1-dot-firm-aria-738.appspot.com/googleglassserver?ACTION=IMAGE&EQUATION=" + equation +
                        "&SESSION=" + _sessionCode);
                String responseTxt = "";
                int status = response.getStatus();
                if (status != 200) {
                    return status;
                } else {
                    byte[] content = response.getContent();
                    _imageIS = new ByteArrayInputStream(content);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    Bitmap bitmapImage = BitmapFactory.decodeStream(_imageIS, null, options);
                    //Now we store the image in the cache
                    _imageCache.put(equation, bitmapImage);
                    return status;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

}
