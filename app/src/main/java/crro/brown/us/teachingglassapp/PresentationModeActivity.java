package crro.brown.us.teachingglassapp;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * This application is the one responsible for making the application work.
 * It has the gesture listeners
 */
public class PresentationModeActivity extends Activity {
    private InputStream _imageIS;
    private ImageView _equationView;
    /** Listener for tap and swipe gestures during the presentation.
     * Tapping opens the menu
     * Swipping RIGHT or forward changes the note or the slide, depending on the current index
     * Swipping LEFT or backward changes the note or the slide, depending on the current index
     */
    private final GestureDetector.BaseListener mBaseListener = new GestureDetector.BaseListener() {
        @Override
        public boolean onGesture(Gesture gesture) {
            if (areGesturesEnabled()) {
                switch (gesture) {
                    case SWIPE_LEFT:
                        // Swipe left (backward) is to the previous slide. Update current index and slide index.
                        if (_currentIndex - 1 < 0 && _slideIndex == 0) {
                            //We are at the very beginning so we do not do anything
                            return true;
                        } else {
                            _currentIndex--;
                            //Deal with the changes in current index
                            if (_currentIndex < 0) {
                                //If we are out of bounds, we go to the previous slide
                                new SendPostTask(PresentationModeActivity.this).execute("Action Previous");
                                _slideIndex--;
                                _currentIndex = 0;
                            }
                            //regardless, we need to update glass
                            String note = _notes.get(_slideIndex).get(_currentIndex);
                            if (note.charAt(0) == '<') {
                                //Then we use the empty equation approach
                                String equation = note;
                                new SendPostTask(PresentationModeActivity.this).execute("GET IMAGE", equation.substring(2, equation.length() - 2));
                            } else if (note.contains("<<") && note.contains(">>")) {
                                String[] notes = note.split("<<");
                                new SendPostTask(PresentationModeActivity.this).execute("GET IMAGE", notes[1].substring(0, notes[1].length() - 2));
                            } else {
                                //no equation
                                _notesTv.setVisibility(View.VISIBLE);
                                _equationView.setVisibility((View.GONE));
                                _notesTv.setText(note);
                            }
                        }
                        return true;
                    case SWIPE_RIGHT:
                        // Delegate tap and swipe right (forward) to the NEXT slide. Update current index and slide index.
                        if (_currentIndex + 1 == _notes.get(_slideIndex).size() && _slideIndex == _notes.size() - 1) {
                            //We are at the very end so we don't do anything
                            return true;
                        } else {
                            _currentIndex++;
                            if (_currentIndex == _notes.get(_slideIndex).size()) {
                                //If we go out of bounds, we go to the next slide
                                new SendPostTask(PresentationModeActivity.this).execute("Action Next");
                                _slideIndex++;
                                _currentIndex = 0;
                            }
                            //Anyhow, we update glass display.
                            String note = _notes.get(_slideIndex).get(_currentIndex);
                            if (note.charAt(0) == '<') {
                                //Then we use the empty equation approach
                                String equation = note;
                                new SendPostTask(PresentationModeActivity.this).execute("GET IMAGE", equation.substring(2, equation.length() - 2));
                            } else if (note.contains("<<") && note.contains(">>")) {
                                String[] notes = note.split("<<");
                                new SendPostTask(PresentationModeActivity.this).execute("GET IMAGE", notes[1].substring(0, notes[1].length() - 2));
                            } else {
                                //no equation
                                _notesTv.setVisibility(View.VISIBLE);
                                _equationView.setVisibility((View.GONE));
                                _notesTv.setText(note);
                            }
                        }
                        return true;
                    case TAP:
                        mAudioManager.playSoundEffect(Sounds.TAP);
                        openOptionsMenu();
                        return true;
                    default:
                        return false;
                }
            }
            return false;
        }

    };

    /**
     * Local Broadcast Receiver set up to regulate the constant braodcast of events
     */
    private LocalBroadcastManager _lbm;

    private BroadcastReceiver _udpateIndex;

    /** Audio manager used to play system sound effects. */
    private AudioManager mAudioManager;

    /** Detects gestures during the interaction. */
    private GestureDetector mGestureDetector;

    private TextView _notesTv;

    private ArrayList<ArrayList<String>> _notes;
    //The slide index is used for the slides
    private int _slideIndex;
    //The current index for the current note within a slide being displayed.
    private int _currentIndex;

    /**
     * Value that can be updated to enable/disable gesture handling in the game. For example,
     * gestures are disabled briefly when a phrase is scored so that the user cannot score or
     * pass again until the animation has completed.
     */
    private boolean mGesturesEnabled;

    /**
     * This method does most of the initialization
     */
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.presentation_layout);
        setGesturesEnabled(true);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mGestureDetector = new GestureDetector(this).setBaseListener(mBaseListener);
        _notesTv = (TextView) findViewById(R.id.notes_panel);
        _equationView = (ImageView) findViewById(R.id.equations_viewer);
        _currentIndex = 0;
        _lbm = LocalBroadcastManager.getInstance(this);
        //Keeping the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Some other initialization that requires the Activity to exist.
     */
    @Override
    protected void onStart() {
        super.onStart();
        new SendPostTask(this).execute("Action Notes");
        IntentFilter filter = new IntentFilter(GlassConstants.UPDATE_FINISHED);
        _udpateIndex = new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {
                //Launch the next async task
                new SendGetTask(PresentationModeActivity.this, _lbm).execute("INDEX");
            }

        };
        _lbm.registerReceiver(_udpateIndex, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
    /** Returns true if gestures should be processed or false if they should be ignored. */
    private boolean areGesturesEnabled() {
        return mGesturesEnabled;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.end_presentation, menu);
        return true;
    }

    /**
     * The act of starting an activity here is wrapped inside a posted {@code Runnable} to avoid
     * animation problems between the closing menu and the new activity. The post ensures that the
     * menu gets the chance to slide down off the screen before the activity is started.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The startXXX() methods start a new activity, and if we call them directly here then
        // the new activity will start without giving the menu a chance to slide back down first.
        // By posting the calls to a handler instead, they will be processed on an upcoming pass
        // through the message queue, after the animation has completed, which results in a
        // smoother transition between activities.
        switch (item.getItemId()) {
            case R.id.end_presentation:
                this.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return mGestureDetector.onMotionEvent(event);
    }

    /**
     * Enables gesture handling if {@code enabled} is true, otherwise disables gesture handling.
     * Gestures are temporarily disabled when a phrase is scored so that extraneous taps and
     * swipes are ignored during the animation.
     */
    private void setGesturesEnabled(boolean enabled) {
        mGesturesEnabled = enabled;
    }

    /**
     * Setters and Getters
     */
    public void setCurrentIndex(int index) {
        _currentIndex = index;
    }

    public int getCurrentIndex() {
        return _currentIndex;
    }

    public void setSlideIndex(int index) {
        _currentIndex = index;
    }

    public int getSlideIndex() {
        return _currentIndex;
    }

    public void setNoteTVText(String text) {
        _notesTv.setText(text);
    }

    public ArrayList<ArrayList<String>> getNotes() {
        return _notes;
    }

    public void setNotes(ArrayList<ArrayList<String>> notes) {
        _notes = notes;
    }

    public void setNoteTVVisibility(int vis) {
        _notesTv.setVisibility(vis);
    }

    public void setEquationsVisibility(int vis) {
        _equationView.setVisibility(vis);
    }

    public void setEquationImage(Bitmap image) {
        _equationView.setImageBitmap(image);
    }


}
