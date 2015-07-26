package maxmarshall.com.georadio3.exoplayer;

import android.media.MediaCodec;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.widget.SeekBar;

import com.google.android.exoplayer.DummyTrackRenderer;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.chunk.ChunkSampleSource;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.chunk.MultiTrackChunkSource;
import com.google.android.exoplayer.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.metadata.MetadataTrackRenderer;
import com.google.android.exoplayer.text.TextRenderer;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.util.PlayerControl;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import maxmarshall.com.georadio3.R;

/**
 * Created by Max on 09/07/2015.
 */
public class ThePlayer implements ExoPlayer.Listener, ChunkSampleSource.EventListener,
        HlsSampleSource.EventListener, DefaultBandwidthMeter.EventListener,
        MediaCodecAudioTrackRenderer.EventListener,
        StreamingDrmSessionManager.EventListener, TextRenderer  {


    @Override
    public void onAudioTrackInitializationError(AudioTrack.InitializationException e) {

    }

    @Override
    public void onAudioTrackWriteError(AudioTrack.WriteException e) {

    }

    @Override
    public void onDrmSessionManagerError(Exception e) {

    }

    public interface RendererBuilder {

        void buildRenderers(ThePlayer player, RendererBuilderCallback callback);
    }

    public interface RendererBuilderCallback {

        void onRenderers(String[][] trackNames, MultiTrackChunkSource[] multiTrackSources,
                         TrackRenderer[] renderers);

        void onRenderersError(Exception e);
    }


    public interface Listener {
        void onStateChanged(boolean playWhenReady, int playbackState);
        void onError(Exception e);
    }


    public interface InternalErrorListener {
        void onRendererInitializationError(Exception e);
        void onAudioTrackInitializationError(AudioTrack.InitializationException e);
        void onAudioTrackWriteError(AudioTrack.WriteException e);
        void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e);
        void onCryptoError(MediaCodec.CryptoException e);
        void onLoadError(int sourceId, IOException e);
        void onDrmSessionManagerError(Exception e);
    }


    public interface InfoListener {
        void onVideoFormatEnabled(Format format, int trigger, int mediaTimeMs);
        void onAudioFormatEnabled(Format format, int trigger, int mediaTimeMs);
        void onDroppedFrames(int count, long elapsed);
        void onBandwidthSample(int elapsedMs, long bytes, long bitrateEstimate);
        void onLoadStarted(int sourceId, long length, int type, int trigger, Format format,
                           int mediaStartTimeMs, int mediaEndTimeMs);
        void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger, Format format,
                             int mediaStartTimeMs, int mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs);
        void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
                                  long initializationDurationMs);
    }


    public interface TextListener {
        void onText(String text);
    }


    public interface Id3MetadataListener {
        void onId3Metadata(Map<String, Object> metadata);
    }

    public static final int STATE_IDLE = ExoPlayer.STATE_IDLE;
    public static final int STATE_PREPARING = ExoPlayer.STATE_PREPARING;
    public static final int STATE_BUFFERING = ExoPlayer.STATE_BUFFERING;
    public static final int STATE_READY = ExoPlayer.STATE_READY;
    public static final int STATE_ENDED = ExoPlayer.STATE_ENDED;

    public static final int DISABLED_TRACK = -1;
    public static final int PRIMARY_TRACK = 0;

    public static final int RENDERER_COUNT = 5;
    public static final int TYPE_VIDEO = 0;
    public static final int TYPE_AUDIO = 1;
    public static final int TYPE_TEXT = 2;
    public static final int TYPE_TIMED_METADATA = 3;
    public static final int TYPE_DEBUG = 4;

    private static final int RENDERER_BUILDING_STATE_IDLE = 1;
    private static final int RENDERER_BUILDING_STATE_BUILDING = 2;
    private static final int RENDERER_BUILDING_STATE_BUILT = 3;

    private final RendererBuilder rendererBuilder;
    private final ExoPlayer player;
    private final PlayerControl playerControl;
    private final Handler mainHandler;
    private final CopyOnWriteArrayList<Listener> listeners;

    private int rendererBuildingState;
    private int lastReportedPlaybackState;
    private boolean lastReportedPlayWhenReady;

    private Surface surface;
    private InternalRendererBuilderCallback builderCallback;
    private TrackRenderer videoRenderer;
    private Format videoFormat;
    private int videoTrackToRestore;

    private MultiTrackChunkSource[] multiTrackSources;
    private String[][] trackNames;
    private int[] selectedTracks;
    private boolean backgrounded;

    private TextListener textListener;
    private Id3MetadataListener id3MetadataListener;
    private InternalErrorListener internalErrorListener;
    private InfoListener infoListener;

    public ThePlayer(RendererBuilder rendererBuilder) {
        this.rendererBuilder = rendererBuilder;
        player = ExoPlayer.Factory.newInstance(RENDERER_COUNT, 1000, 5000);
        player.addListener(this);
        playerControl = new PlayerControl(player);
        mainHandler = new Handler();
        listeners = new CopyOnWriteArrayList<Listener>();
        lastReportedPlaybackState = STATE_IDLE;
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        selectedTracks = new int[RENDERER_COUNT];
        selectedTracks[TYPE_TEXT] = DISABLED_TRACK;

    }

    public PlayerControl getPlayerControl() {
        return playerControl;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }



    public void setTextListener(TextListener listener) {
        textListener = listener;
    }

    public void setMetadataListener(Id3MetadataListener listener) {
        id3MetadataListener = listener;
    }


    public String[] getTracks(int type) {
        return trackNames == null ? null : trackNames[type];
    }

    public int getSelectedTrackIndex(int type) {
        return selectedTracks[type];
    }

    public void selectTrack(int type, int index) {
        if (selectedTracks[type] == index) {
            return;
        }
        selectedTracks[type] = index;
        pushTrackSelection(type, true);
        if (type == TYPE_TEXT && index == DISABLED_TRACK && textListener != null) {
            textListener.onText(null);
        }
    }

    public void prepare() {
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT) {
            player.stop();
        }
        if (builderCallback != null) {
            builderCallback.cancel();
        }
        videoFormat = null;
        videoRenderer = null;
        multiTrackSources = null;
        rendererBuildingState = RENDERER_BUILDING_STATE_BUILDING;
        maybeReportPlayerState();
        builderCallback = new InternalRendererBuilderCallback();
        rendererBuilder.buildRenderers(this, builderCallback);
    }

     void onRenderers(String[][] trackNames,
                                   MultiTrackChunkSource[] multiTrackSources, TrackRenderer[] renderers) {
        builderCallback = null;
        if (trackNames == null) {
            trackNames = new String[RENDERER_COUNT][];
        }
        if (multiTrackSources == null) {
            multiTrackSources = new MultiTrackChunkSource[RENDERER_COUNT];
        }
        for (int i = 0; i < RENDERER_COUNT; i++) {
            if (renderers[i] == null) {
                renderers[i] = new DummyTrackRenderer();
            } else if (trackNames[i] == null) {
                int trackCount = multiTrackSources[i] == null ? 1 : multiTrackSources[i].getTrackCount();
                trackNames[i] = new String[trackCount];
            }
        }
        this.trackNames = trackNames;
        this.videoRenderer = renderers[TYPE_VIDEO];
        this.multiTrackSources = multiTrackSources;
        pushSurface(false);
        pushTrackSelection(TYPE_VIDEO, true);
        pushTrackSelection(TYPE_AUDIO, true);
        pushTrackSelection(TYPE_TEXT, true);
        player.prepare(renderers);
        rendererBuildingState = RENDERER_BUILDING_STATE_BUILT;
    }

     void onRenderersError(Exception e) {
        builderCallback = null;
        if (internalErrorListener != null) {
            internalErrorListener.onRendererInitializationError(e);
        }
        for (Listener listener : listeners) {
            listener.onError(e);
        }
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        maybeReportPlayerState();
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        player.setPlayWhenReady(playWhenReady);
    }

    public void seekTo(long positionMs) {
        player.seekTo(positionMs);
    }

    public void release() {
        if (builderCallback != null) {
            builderCallback.cancel();
            builderCallback = null;
        }
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        surface = null;
        player.release();
    }


    public int getPlaybackState() {
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILDING) {
            return ExoPlayer.STATE_PREPARING;
        }
        int playerState = player.getPlaybackState();
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT
                && rendererBuildingState == RENDERER_BUILDING_STATE_IDLE) {
            return ExoPlayer.STATE_PREPARING;
        }
        return playerState;
    }

    public long getCurrentPosition() {
        return player.getCurrentPosition();
    }

    public long getDuration() {
        return player.getDuration();
    }

    public int getBufferedPercentage() {
        return player.getBufferedPercentage();
    }

    public boolean getPlayWhenReady() {
        return player.getPlayWhenReady();
    }

     Looper getPlaybackLooper() {
        return player.getPlaybackLooper();
    }

     Handler getMainHandler() {
        return mainHandler;
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int state) {
        maybeReportPlayerState();
    }

    @Override
    public void onPlayerError(ExoPlaybackException exception) {
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        for (Listener listener : listeners) {
            listener.onError(exception);
        }
    }


    @Override
    public void onBandwidthSample(int elapsedMs, long bytes, long bitrateEstimate) {
        if (infoListener != null) {
            infoListener.onBandwidthSample(elapsedMs, bytes, bitrateEstimate);
        }
    }

    @Override
    public void onDownstreamFormatChanged(int sourceId, Format format, int trigger, int mediaTimeMs) {
        if (infoListener == null) {
            return;
        }
        if (sourceId == TYPE_VIDEO) {
            videoFormat = format;
            infoListener.onVideoFormatEnabled(format, trigger, mediaTimeMs);
        } else if (sourceId == TYPE_AUDIO) {
            infoListener.onAudioFormatEnabled(format, trigger, mediaTimeMs);
        }
    }


    @Override
    public void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e) {

    }

    @Override
    public void onCryptoError(MediaCodec.CryptoException e) {

    }

    @Override
    public void onDecoderInitialized(
            String decoderName,
            long elapsedRealtimeMs,
            long initializationDurationMs) {
        if (infoListener != null) {
            infoListener.onDecoderInitialized(decoderName, elapsedRealtimeMs, initializationDurationMs);
        }
    }

    @Override
    public void onLoadError(int sourceId, IOException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onLoadError(sourceId, e);
        }
    }

    @Override
    public void onText(String text) {
        processText(text);
    }

     MetadataTrackRenderer.MetadataRenderer<Map<String, Object>>
    getId3MetadataRenderer() {
        return new MetadataTrackRenderer.MetadataRenderer<Map<String, Object>>() {
            @Override
            public void onMetadata(Map<String, Object> metadata) {
                if (id3MetadataListener != null) {
                    id3MetadataListener.onId3Metadata(metadata);
                }
            }
        };
    }

    @Override
    public void onPlayWhenReadyCommitted() {
    }

    @Override
    public void onLoadStarted(int sourceId, long length, int type, int trigger, Format format,
                              int mediaStartTimeMs, int mediaEndTimeMs) {
        if (infoListener != null) {
            infoListener.onLoadStarted(sourceId, length, type, trigger, format, mediaStartTimeMs,
                    mediaEndTimeMs);
        }
    }

    @Override
    public void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger, Format format,
                                int mediaStartTimeMs, int mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs) {
        if (infoListener != null) {
            infoListener.onLoadCompleted(sourceId, bytesLoaded, type, trigger, format, mediaStartTimeMs,
                    mediaEndTimeMs, elapsedRealtimeMs, loadDurationMs);
        }
    }

    @Override
    public void onLoadCanceled(int sourceId, long bytesLoaded) {
    }

    @Override
    public void onUpstreamDiscarded(int sourceId, int mediaStartTimeMs, int mediaEndTimeMs) {
    }

    private void maybeReportPlayerState() {
        boolean playWhenReady = player.getPlayWhenReady();
        int playbackState = getPlaybackState();
        if (lastReportedPlayWhenReady != playWhenReady || lastReportedPlaybackState != playbackState) {
            for (Listener listener : listeners) {
                listener.onStateChanged(playWhenReady, playbackState);
            }
            lastReportedPlayWhenReady = playWhenReady;
            lastReportedPlaybackState = playbackState;
        }
    }

    private void pushSurface(boolean blockForSurfacePush) {
        if (videoRenderer == null) {
            return;
        }

        if (blockForSurfacePush) {
            player.blockingSendMessage(
                    videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface);
        } else {
            player.sendMessage(
                    videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface);
        }
    }

    private void pushTrackSelection(int type, boolean allowRendererEnable) {
        if (multiTrackSources == null) {
            return;
        }

        int trackIndex = selectedTracks[type];
        if (trackIndex == DISABLED_TRACK) {
            player.setRendererEnabled(type, false);
        } else if (multiTrackSources[type] == null) {
            player.setRendererEnabled(type, allowRendererEnable);
        } else {
            boolean playWhenReady = player.getPlayWhenReady();
            player.setPlayWhenReady(false);
            player.setRendererEnabled(type, false);
            player.sendMessage(multiTrackSources[type], MultiTrackChunkSource.MSG_SELECT_TRACK,
                    trackIndex);
            player.setRendererEnabled(type, allowRendererEnable);
            player.setPlayWhenReady(playWhenReady);

        }
    }

     void processText(String text) {
        if (textListener == null || selectedTracks[TYPE_TEXT] == DISABLED_TRACK) {
            return;
        }
        textListener.onText(text);
    }

    private class InternalRendererBuilderCallback implements RendererBuilderCallback {

        private boolean canceled;

        public void cancel() {
            canceled = true;
        }

        @Override
        public void onRenderers(String[][] trackNames, MultiTrackChunkSource[] multiTrackSources,
                                TrackRenderer[] renderers) {
            if (!canceled) {
                ThePlayer.this.onRenderers(trackNames, multiTrackSources, renderers);
            }
        }

        @Override
        public void onRenderersError(Exception e) {
            if (!canceled) {
                ThePlayer.this.onRenderersError(e);
            }
        }


    }

}