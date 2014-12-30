package crro.brown.us.teachingglassapp;

import com.google.android.glass.app.Card;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * This MainActivity is the activity that runs when the application starts.
 * It allows the user to start the presentation through the options menu.
 *
 * Created by David Correa on 09/30/2014
 */
public class MainActivity extends Activity {

    /**
     * Handler used to post requests to start new activities so that the menu closing animation
     * works properly.
     */
    private final Handler mHandler = new Handler();

    /** Listener that displays the options menu when the touchpad is tapped. */
    private final GestureDetector.BaseListener mBaseListener = new GestureDetector.BaseListener() {
        @Override
        public boolean onGesture(Gesture gesture) {
            if (gesture == Gesture.TAP) {
                mAudioManager.playSoundEffect(Sounds.TAP);
                openOptionsMenu();
                return true;
            } else {
                return false;
            }
        }
    };

    /** Audio manager used to play system sound effects. */
    private AudioManager mAudioManager;

    /**
     * Value that can be updated to enable/disable gesture handling in the game. For example,
     * gestures are disabled briefly when a phrase is scored so that the user cannot score or
     * pass again until the animation has completed.
     */
    private boolean mGesturesEnabled;

    /** Detects gestures during the interaction. */
    private GestureDetector mGestureDetector;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_start_presentation);
        setGesturesEnabled(true);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mGestureDetector = new GestureDetector(this).setBaseListener(mBaseListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.start_presentation, menu);
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
            case R.id.start_presentation:
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        startPresentation();
                    }
                });
                return true;
            default:
                return false;
        }
    }

    /** Returns true if gestures should be processed or false if they should be ignored. */
    private boolean areGesturesEnabled() {
        return mGesturesEnabled;
    }

    /**
     * Enables gesture handling if {@code enabled} is true, otherwise disables gesture handling.
     * Gestures are temporarily disabled when a phrase is scored so that extraneous taps and
     * swipes are ignored during the animation.
     */
    private void setGesturesEnabled(boolean enabled) {
        mGesturesEnabled = enabled;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return mGestureDetector.onMotionEvent(event);
    }

    /**
     * Starts the PresentationModeActivity.
     */
    private void startPresentation() {
        startActivity(new Intent(this, PresentationModeActivity.class));
    }



}
