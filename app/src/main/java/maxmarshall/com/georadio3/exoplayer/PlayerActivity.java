package maxmarshall.com.georadio3.exoplayer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.CaptioningManager;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Location;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.VideoSurfaceView;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.metadata.GeobMetadata;
import com.google.android.exoplayer.metadata.PrivMetadata;
import com.google.android.exoplayer.metadata.TxxxMetadata;
import com.google.android.exoplayer.text.CaptionStyleCompat;
import com.google.android.exoplayer.text.SubtitleView;
import com.google.android.exoplayer.util.Util;
import com.google.android.exoplayer.util.VerboseLogUtil;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import maxmarshall.com.georadio3.Constants;
import maxmarshall.com.georadio3.GeofenceErrorMessages;
import maxmarshall.com.georadio3.GeofenceTransitionsIntentService;
import maxmarshall.com.georadio3.R;

/**
 * Created by Max on 08/07/2015.
 */

public class PlayerActivity extends Activity implements OnMapReadyCallback,
        View.OnClickListener,
        ThePlayer.Listener, ThePlayer.TextListener, ThePlayer.Id3MetadataListener,
        AudioCapabilitiesReceiver.Listener, SeekBar.OnSeekBarChangeListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    public static final String CONTENT_TYPE_EXTRA = "content_type";
    public static final String CONTENT_ID_EXTRA = "content_id";

    private static final String TAG = "PlayerActivity";

    private static final int MENU_GROUP_TRACKS = 1;
    private static final int ID_OFFSET = 2;
    private MediaController mediaController;
    private View debugRootView;
    private TextView debugTextView;
    private TextView playerStateTextView;
    private Button videoButton;
    private Button audioButton;
    private Button textButton;
    private Button retryButton;
    private Button back;
    private SeekBar seekBar1;
    private TextView info;

    private ThePlayer player;
    private MediaCodecAudioTrackRenderer audioRenderer1;
    private boolean playerNeedsPrepare;

    private long playerPosition;
    private boolean enableBackgroundAudio;

    private Uri contentUri;
    private int contentType;
    private String contentId;


    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;
    private AudioCapabilities audioCapabilities;
    private GoogleMap map;
    GoogleMapOptions options = new GoogleMapOptions();
    private LatLngBounds Clerkenwell = new LatLngBounds(
            new LatLng( 51.516270, -0.119611) , new LatLng(51.523306, -0.098679));
    private LatLng ClerkCentre = new LatLng( 51.516270, -0.119611);
    private Intent SeekIntent;

    protected GoogleApiClient mGoogleApiClient;

    protected ArrayList<Geofence> mGeofenceList;

    private boolean mGeofencesAdded;

    private PendingIntent mGeofencePendingIntent;

    private SharedPreferences mSharedPreferences;

    private Button BeginTour;
    private Button EndTour;





    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        Intent intent = getIntent();
        contentUri = intent.getData();
        contentType = intent.getIntExtra(CONTENT_TYPE_EXTRA, -1);
        contentId = intent.getStringExtra(CONTENT_ID_EXTRA);

        setContentView(R.layout.playeractivity);
        View root = findViewById(R.id.root);


        map=((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(Clerkenwell, 100, 100, 20));
        map.setMyLocationEnabled(true);

        options.mapType(GoogleMap.MAP_TYPE_HYBRID)
                .compassEnabled(true)
                .rotateGesturesEnabled(false)
                .tiltGesturesEnabled(false)
                .scrollGesturesEnabled(false)
                .zoomControlsEnabled(false);
        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                TextView info = (TextView) findViewById(R.id.info);
                info.setText(marker.getSnippet());
                findViewById(R.id.info).setVisibility(View.VISIBLE);
                findViewById(R.id.back).setVisibility(View.VISIBLE);

            }
        });

        map.addMarker(new MarkerOptions()
                .position(new LatLng(51.521037, -0.099674))
                .snippet("Originally founded as a Carthusian Monastery, the Charterhouse passed through successive owners following its dissolution by Henry VIII, before coming into the possession of the hugely wealthy Sir Thomas Sutton in 1611. Here, he established both the Charterhouse Hospital for aged men, and Charterhouse School for the education of the sons of the poor. By the early 19th century, Charterhouse had become a leading public school. However, the writer William Makepeace Thackeray (1811–63), an ex-pupil, was not impressed by the establishment, later recalling how he ‘was lulled into indolence & when I grew older & could think for myself was abused into sulkiness and bullied into despair’. Although the school moved out of the area in the late 19th Century, the Charterhouse is still a hospital-cum-retirement home and can be visited at certain times of the year.")
                .title("The Charterhouse"));
        map.addMarker(new MarkerOptions()
                .position(new LatLng(51.519783, -0.101576))
                .title("The Meat Market"));
        map.addMarker(new MarkerOptions()
                .position(new LatLng(51.522035, -0.102846))
                .title("St Johns museum"));
        map.addMarker(new MarkerOptions()
                .position(new LatLng(51.521703, -0.104112))
                .title("The Jerusalem Tavern"));
        map.addMarker(new MarkerOptions()
                .position(new LatLng(51.522204, -0.106234))
                .title("The River Fleet"));
        map.addMarker(new MarkerOptions()
                .position(new LatLng(51.522638, -0.105694))
                .title("The old sessions house"));
        map.addMarker(new MarkerOptions()
                .position(new LatLng(51.523376, -0.105639))
                .title("The Church of St James"));
        map.addMarker(new MarkerOptions()
                .position(new LatLng(51.523341, -0.107239))
                .title("The Peabody estate"));
        map.addMarker(new MarkerOptions()
                .position(new LatLng(51.521383, -0.108756))
                .title("The Police Office"));
        map.addMarker(new MarkerOptions()
                .position(new LatLng(51.520277, -0.106726))
                .title("Saffron Hill"));
        map.addMarker(new MarkerOptions()
                .position(new LatLng(51.518371, -0.107406))
                .title("Ye Olde Mitre Tavern"));
        map.addMarker(new MarkerOptions()
                .position(new LatLng(51.518110, -0.107079))
                .title("Ely Place"));


        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    toggleControlsVisibility();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    view.performClick();
                }
                return true;
            }
        });
        root.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                    return mediaController.dispatchKeyEvent(event);
                }
                return false;
            }
        });
        audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(getApplicationContext(), this);

        debugRootView = findViewById(R.id.controls_root);
        debugTextView = (TextView) findViewById(R.id.debug_text_view);
        playerStateTextView = (TextView) findViewById(R.id.player_state_view);

        mediaController = new MediaController(this);
        mediaController.setAnchorView(root);
        retryButton = (Button) findViewById(R.id.retry_button);
        retryButton.setOnClickListener(this);
        videoButton = (Button) findViewById(R.id.video_controls);
        audioButton = (Button) findViewById(R.id.audio_controls);
        textButton = (Button) findViewById(R.id.text_controls);
        GeoUtil.setDefaultCookieManager();
        back = (Button) findViewById(R.id.back);
        back.setOnClickListener(this);

        seekBar1 = (SeekBar) findViewById(R.id.seekBar1);
        seekBar1.setOnSeekBarChangeListener(this);
        seekBar1.setProgress(100);






        BeginTour = (Button) findViewById(R.id.begintour);
        EndTour = (Button) findViewById(R.id.endtour);

        mGeofenceList = new ArrayList<Geofence>();
        mGeofencePendingIntent = null;

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME,
                MODE_PRIVATE);

        mGeofencesAdded = mSharedPreferences.getBoolean(Constants.GEOFENCES_ADDED_KEY, false);
        setButtonsEnabledState();

        populateGeofenceList();

        buildGoogleApiClient();





    }

    @Override
    public void onResume() {
        super.onResume();
        audioCapabilitiesReceiver.register();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }


    @Override
    public void onClick(View view) {
        if (view == retryButton) {
            preparePlayer();
        }
        if (view == back){
            findViewById(R.id.info).setVisibility(View.GONE);
            findViewById(R.id.back).setVisibility(View.GONE);


        }

    }


    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        boolean audioCapabilitiesChanged = !audioCapabilities.equals(this.audioCapabilities);
        if (player == null || audioCapabilitiesChanged) {
            this.audioCapabilities = audioCapabilities;
            releasePlayer();
            preparePlayer();
        } else if (player != null) {
        }
    }


    private ThePlayer.RendererBuilder getRendererBuilder() {
        String userAgent = Util.getUserAgent(this);
        switch (contentType) {
            case GeoUtil.TYPE_DASH:
                return new DashRendererBuilder(this, userAgent, contentUri.toString(),
                        null, debugTextView, audioCapabilities);
            default:
                throw new IllegalStateException("Unsupported type: " + contentType);
        }
    }

    private void preparePlayer() {
        if (player == null) {
            player = new ThePlayer(getRendererBuilder());
            player.addListener((ThePlayer.Listener) this);
            player.setTextListener(this);
            player.setMetadataListener(this);
            player.seekTo(playerPosition);
            playerNeedsPrepare = true;
            mediaController.setMediaPlayer(player.getPlayerControl());
            mediaController.setEnabled(true);
        }
        if (playerNeedsPrepare) {
            player.prepare();
            playerNeedsPrepare = false;
            updateButtonVisibilities();
        }
        player.setPlayWhenReady(true);
    }

    private void releasePlayer() {
        if (player != null) {
            playerPosition = player.getCurrentPosition();
            player.release();
            player = null;
        }
    }


    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_ENDED) {
            showControls();
        }
        String text = "playWhenReady=" + playWhenReady + ", playbackState=";
        switch(playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                text += "buffering";
                break;
            case ExoPlayer.STATE_ENDED:
                text += "ended";
                break;
            case ExoPlayer.STATE_IDLE:
                text += "idle";
                break;
            case ExoPlayer.STATE_PREPARING:
                text += "preparing";
                break;
            case ExoPlayer.STATE_READY:
                text += "ready";
                break;
            default:
                text += "unknown";
                break;
        }
        playerStateTextView.setText(text);
        updateButtonVisibilities();
    }

    @Override
    public void onError(Exception e) {

    }


    private void updateButtonVisibilities() {
        retryButton.setVisibility(playerNeedsPrepare ? View.VISIBLE : View.GONE);
        videoButton.setVisibility(haveTracks(ThePlayer.TYPE_VIDEO) ? View.VISIBLE : View.GONE);
        audioButton.setVisibility(haveTracks(ThePlayer.TYPE_AUDIO) ? View.VISIBLE : View.GONE);
        textButton.setVisibility(haveTracks(ThePlayer.TYPE_TEXT) ? View.VISIBLE : View.GONE);
    }

    private boolean haveTracks(int type) {
        return player != null && player.getTracks(type) != null;
    }

    public void showVideoPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        configurePopupWithTracks(popup, null, ThePlayer.TYPE_VIDEO);
        popup.show();
    }

    public void showAudioPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        Menu menu = popup.getMenu();
        menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.enable_background_audio);
        final MenuItem backgroundAudioItem = menu.findItem(0);
        backgroundAudioItem.setCheckable(true);
        backgroundAudioItem.setChecked(enableBackgroundAudio);
        PopupMenu.OnMenuItemClickListener clickListener = new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item == backgroundAudioItem) {
                    enableBackgroundAudio = !item.isChecked();
                    return true;
                }
                return false;
            }
        };
        configurePopupWithTracks(popup, clickListener, ThePlayer.TYPE_AUDIO);
        popup.show();
    }

    public void showTextPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        configurePopupWithTracks(popup, null, ThePlayer.TYPE_TEXT);
        popup.show();
    }

    public void showVerboseLogPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        Menu menu = popup.getMenu();
        menu.add(Menu.NONE, 0, Menu.NONE, R.string.logging_normal);
        menu.add(Menu.NONE, 1, Menu.NONE, R.string.logging_verbose);
        menu.setGroupCheckable(Menu.NONE, true, true);
        menu.findItem((VerboseLogUtil.areAllTagsEnabled()) ? 1 : 0).setChecked(true);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == 0) {
                    VerboseLogUtil.setEnableAllTags(false);
                } else {
                    VerboseLogUtil.setEnableAllTags(true);
                }
                return true;
            }
        });
        popup.show();
    }

    private void configurePopupWithTracks(PopupMenu popup,
                                          final PopupMenu.OnMenuItemClickListener customActionClickListener,
                                          final int trackType) {
        if (player == null) {
            return;
        }
        String[] tracks = player.getTracks(trackType);
        if (tracks == null) {
            return;
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return (customActionClickListener != null
                        && customActionClickListener.onMenuItemClick(item))
                        || onTrackItemClick(item, trackType);
            }
        });
        Menu menu = popup.getMenu();
        menu.add(MENU_GROUP_TRACKS, ThePlayer.DISABLED_TRACK + ID_OFFSET, Menu.NONE, R.string.off);
        if (tracks.length == 1 && TextUtils.isEmpty(tracks[0])) {
            menu.add(MENU_GROUP_TRACKS, ThePlayer.PRIMARY_TRACK + ID_OFFSET, Menu.NONE, R.string.on);
        } else {
            for (int i = 0; i < tracks.length; i++) {
                menu.add(MENU_GROUP_TRACKS, i + ID_OFFSET, Menu.NONE, tracks[i]);
            }
        }
        menu.setGroupCheckable(MENU_GROUP_TRACKS, true, true);
        menu.findItem(player.getSelectedTrackIndex(trackType) + ID_OFFSET).setChecked(true);
    }

    private boolean onTrackItemClick(MenuItem item, int type) {
        if (player == null || item.getGroupId() != MENU_GROUP_TRACKS) {
            return false;
        }
        player.selectTrack(type, item.getItemId() - ID_OFFSET);
        return true;
    }

    private void toggleControlsVisibility()  {
        if (mediaController.isShowing()) {
            mediaController.hide();
            debugRootView.setVisibility(View.GONE);
        } else {
            showControls();
        }
    }

    private void showControls() {
        mediaController.show(0);
        debugRootView.setVisibility(View.VISIBLE);
    }


    @Override
    public void onText(String text) {

    }


    @Override
    public void onId3Metadata(Map<String, Object> metadata) {
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (TxxxMetadata.TYPE.equals(entry.getKey())) {
                TxxxMetadata txxxMetadata = (TxxxMetadata) entry.getValue();
                Log.i(TAG, String.format("ID3 TimedMetadata %s: description=%s, value=%s",
                        TxxxMetadata.TYPE, txxxMetadata.description, txxxMetadata.value));
            } else if (PrivMetadata.TYPE.equals(entry.getKey())) {
                PrivMetadata privMetadata = (PrivMetadata) entry.getValue();
                Log.i(TAG, String.format("ID3 TimedMetadata %s: owner=%s",
                        PrivMetadata.TYPE, privMetadata.owner));
            } else if (GeobMetadata.TYPE.equals(entry.getKey())) {
                GeobMetadata geobMetadata = (GeobMetadata) entry.getValue();
                Log.i(TAG, String.format("ID3 TimedMetadata %s: mimeType=%s, filename=%s, description=%s",
                        GeobMetadata.TYPE, geobMetadata.mimeType, geobMetadata.filename,
                        geobMetadata.description));
            } else {
                Log.i(TAG, String.format("ID3 TimedMetadata %s", entry.getKey()));
            }
        }
    }


    @Override
    public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {


        switch (arg0.getId())
        {
            case R.id.seekBar1:
                int volume = (int) (1 - (Math.log(100 - progress) / Math.log(100)));


                break;

        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }




    @Override
    public void onMapReady(GoogleMap map) {
    }



    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }





    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();

    }

    /**
     * Adds geofences, which sets alerts to be notified when the device enters or exits one of the
     * specified geofences. Handles the success or failure results returned by addGeofences().
     */
    public void addGeofencesButtonHandler(View view) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    /**
     * Removes geofences, which stops further notifications when the device enters or exits
     * previously registered geofences.
     */
    public void removeGeofencesButtonHandler(View view) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            // Remove geofences.
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    // This is the same pending intent that was used in addGeofences().
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }


    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }

    /**
     * Runs when the result of calling addGeofences() and removeGeofences() becomes available.
     * Either method can complete successfully or with an error.
     *
     * Since this activity implements the {@link ResultCallback} interface, we are required to
     * define this method.
     *
     * @param status The Status returned through a PendingIntent when addGeofences() or
     *               removeGeofences() get called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
            // Update state and save in shared preferences.
            mGeofencesAdded = !mGeofencesAdded;
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(Constants.GEOFENCES_ADDED_KEY, mGeofencesAdded);
            editor.commit();

            // Update the UI. Adding geofences enables the Remove Geofences button, and removing
            // geofences enables the Add Geofences button.
            setButtonsEnabledState();

            Toast.makeText(
                    this,
                    getString(mGeofencesAdded ? R.string.geofences_added :
                            R.string.geofences_removed),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(TAG, errorMessage);
        }
    }


    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);


    }



    public void populateGeofenceList() {
        for (Map.Entry<String, LatLng> entry : Constants.Geolocations.entrySet())

            mGeofenceList.add(new Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(entry.getKey())

                            // Set the circular region of this geofence.
                    .setCircularRegion(
                            entry.getValue().latitude,
                            entry.getValue().longitude,
                            Constants.GEOFENCE_RADIUS_IN_METERS
                    )

                            // Set the expiration duration of the geofence. This geofence gets automatically
                            // removed after this period of time.
                    .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                            // Set the transition types of interest. Alerts are only generated for these
                            // transition. We track entry and exit transitions in this sample.
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT)

                            // Create the geofence.
                    .build());
    }

    private void setButtonsEnabledState() {
        if (mGeofencesAdded) {
            BeginTour.setVisibility(View.GONE);
            EndTour.setVisibility(View.VISIBLE);
        } else {
            BeginTour.setVisibility(View.VISIBLE);
            EndTour.setVisibility(View.GONE);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason.
        Log.i(TAG, "Connection suspended");

        // onConnected() will be called again automatically when the service reconnects
    }





}



