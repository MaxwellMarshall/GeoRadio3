package maxmarshall.com.georadio3.exoplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCodec;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecUtil;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.chunk.ChunkSampleSource;
import com.google.android.exoplayer.chunk.ChunkSource;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.chunk.FormatEvaluator;
import com.google.android.exoplayer.chunk.MultiTrackChunkSource;
import com.google.android.exoplayer.chunk.VideoFormatSelectorUtil;
import com.google.android.exoplayer.dash.DashChunkSource;
import com.google.android.exoplayer.dash.mpd.AdaptationSet;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescription;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescriptionParser;
import com.google.android.exoplayer.dash.mpd.Period;
import com.google.android.exoplayer.dash.mpd.Representation;
import com.google.android.exoplayer.dash.mpd.UtcTimingElement;
import com.google.android.exoplayer.dash.mpd.UtcTimingElementResolver;
import com.google.android.exoplayer.drm.MediaDrmCallback;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.text.ttml.TtmlParser;
import com.google.android.exoplayer.text.webvtt.WebvttParser;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Max on 09/07/2015.
 */
public class DashRendererBuilder implements ThePlayer.RendererBuilder,
        ManifestFetcher.ManifestCallback<MediaPresentationDescription>, UtcTimingElementResolver.UtcTimingCallback {

    private static final String TAG = "DashRendererBuilder";

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int AUDIO_BUFFER_SEGMENTS = 60;
    private static final int LIVE_EDGE_LATENCY_MS = 30000;


    /**
     * Passthrough audio formats (encodings) in order of decreasing priority.
     */
    private static final int[] PASSTHROUGH_ENCODINGS_PRIORITY =
            new int[]{C.ENCODING_E_AC3, C.ENCODING_AC3};
    /**
     * Passthrough audio codecs corresponding to the encodings in
     * {@link #PASSTHROUGH_ENCODINGS_PRIORITY}.
     */
    private static final String[] PASSTHROUGH_CODECS_PRIORITY =
            new String[]{"ec-3", "ac-3"};

    private final Context context;
    private final String userAgent;
    private final String url;
    private final MediaDrmCallback drmCallback;
    private final AudioCapabilities audioCapabilities;


    private ThePlayer player;
    private ThePlayer.RendererBuilderCallback callback;
    private ManifestFetcher<MediaPresentationDescription> manifestFetcher;
    private UriDataSource manifestDataSource;

    private MediaPresentationDescription manifest;
    private long elapsedRealtimeOffset;

    public DashRendererBuilder(Context context, String userAgent, String url,
                               MediaDrmCallback drmCallback, TextView debugTextView, AudioCapabilities audioCapabilities) {
        this.context = context;
        this.userAgent = userAgent;
        this.url = url;
        this.drmCallback = drmCallback;
        this.audioCapabilities = audioCapabilities;
    }

    @Override
    public void buildRenderers(ThePlayer player, ThePlayer.RendererBuilderCallback callback) {
        this.player = player;
        this.callback = callback;
        MediaPresentationDescriptionParser parser = new MediaPresentationDescriptionParser();
        manifestDataSource = new DefaultUriDataSource(context, userAgent);
        manifestFetcher = new ManifestFetcher<MediaPresentationDescription>(url, manifestDataSource,
                parser);
        manifestFetcher.singleLoad(player.getMainHandler().getLooper(), this);

    }

    @Override
    public void onSingleManifest(MediaPresentationDescription manifest) {
        this.manifest = manifest;
        if (manifest.dynamic && manifest.utcTiming != null) {
            UtcTimingElementResolver.resolveTimingElement(manifestDataSource, manifest.utcTiming,
                    manifestFetcher.getManifestLoadTimestamp(), this);
        } else {
            buildRenderers();
        }

    }
    @Override
    public void onSingleManifestError(IOException e) {
        callback.onRenderersError(e);
    }

    @Override
    public void onTimestampResolved(UtcTimingElement utcTiming, long elapsedRealtimeOffset) {
        this.elapsedRealtimeOffset = elapsedRealtimeOffset;
        buildRenderers();
    }

    @Override
    public void onTimestampError(UtcTimingElement utcTiming, IOException e) {
        Log.e(TAG, "Failed to resolve UtcTiming element [" + utcTiming + "]", e);
        buildRenderers();
    }

    private void buildRenderers() {
        Period period = manifest.periods.get(0);
        Handler mainHandler = player.getMainHandler();
        LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(mainHandler, player);
        boolean hasContentProtection = false;
        int audioAdaptationSetIndex = period.getAdaptationSetIndex(AdaptationSet.TYPE_AUDIO);
        AdaptationSet audioAdaptationSet = null;
        if (audioAdaptationSetIndex != -1) {
            audioAdaptationSet = period.adaptationSets.get(audioAdaptationSetIndex);
            hasContentProtection |= audioAdaptationSet.hasContentProtection();

        }

        // Fail if we have neither video or audio.
        if (audioAdaptationSet == null) {
            callback.onRenderersError(new IllegalStateException("audio adaptation sets"));
            return;
        }

        // Build the audio chunk sources.
        List<ChunkSource> audioChunkSourceList = new ArrayList<ChunkSource>();
        List<String> audioTrackNameList = new ArrayList<String>();
        if (audioAdaptationSet != null) {
            DataSource audioDataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
            FormatEvaluator audioEvaluator = new FormatEvaluator.FixedEvaluator();
            List<Representation> audioRepresentations = audioAdaptationSet.representations;
            List<String> codecs = new ArrayList<String>();
            for (int i = 0; i < audioRepresentations.size(); i++) {
                Format format = audioRepresentations.get(i).format;
                audioTrackNameList.add(format.id + " (" + format.numChannels + "ch, " +
                        format.audioSamplingRate + "Hz)");
                audioChunkSourceList.add(new DashChunkSource(manifestFetcher, audioAdaptationSetIndex,
                        new int[]{i}, audioDataSource, audioEvaluator, LIVE_EDGE_LATENCY_MS,
                        elapsedRealtimeOffset));
                codecs.add(format.codecs);
            }

            if (audioCapabilities != null) {
                // If there are any passthrough audio encodings available, select the highest priority
                // supported format (e.g. E-AC-3) and remove other tracks.
                for (int i = 0; i < PASSTHROUGH_CODECS_PRIORITY.length; i++) {
                    String codec = PASSTHROUGH_CODECS_PRIORITY[i];
                    int encoding = PASSTHROUGH_ENCODINGS_PRIORITY[i];
                    if (codecs.indexOf(codec) == -1 || !audioCapabilities.supportsEncoding(encoding)) {
                        continue;
                    }

                    for (int j = audioRepresentations.size() - 1; j >= 0; j--) {
                        if (!audioRepresentations.get(j).format.codecs.equals(codec)) {
                            audioTrackNameList.remove(j);
                            audioChunkSourceList.remove(j);
                        }
                    }
                }
            }
        }

        // Build the audio renderer.
        final String[] audioTrackNames;
        final MultiTrackChunkSource audioChunkSource;
        final TrackRenderer audioRenderer1;
        if (audioChunkSourceList.isEmpty()) {
            audioTrackNames = null;
            audioChunkSource = null;
            audioRenderer1 = null;
        } else {
            audioTrackNames = new String[audioTrackNameList.size()];
            audioTrackNameList.toArray(audioTrackNames);
            audioChunkSource = new MultiTrackChunkSource(audioChunkSourceList);
            SampleSource audioSampleSource = new ChunkSampleSource(audioChunkSource, loadControl,
                    AUDIO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, true, mainHandler, player,
                    ThePlayer.TYPE_AUDIO);
            audioRenderer1 = new MediaCodecAudioTrackRenderer(audioSampleSource, null, true,
                    mainHandler, player);

        }
        // Build the text chunk sources.
        DataSource textDataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
        FormatEvaluator textEvaluator = new FormatEvaluator.FixedEvaluator();
        List<ChunkSource> textChunkSourceList = new ArrayList<ChunkSource>();
        List<String> textTrackNameList = new ArrayList<String>();
        for (int i = 0; i < period.adaptationSets.size(); i++) {
            AdaptationSet adaptationSet = period.adaptationSets.get(i);
            if (adaptationSet.type == AdaptationSet.TYPE_TEXT) {
                List<Representation> representations = adaptationSet.representations;
                for (int j = 0; j < representations.size(); j++) {
                    Representation representation = representations.get(j);
                    textTrackNameList.add(representation.format.id);
                    textChunkSourceList.add(new DashChunkSource(manifestFetcher, i, new int[]{j},
                            textDataSource, textEvaluator, LIVE_EDGE_LATENCY_MS, elapsedRealtimeOffset));
                }
            }
        }
        // Invoke the callback.
        String[][] trackNames = new String[ThePlayer.RENDERER_COUNT][];
        trackNames[ThePlayer.TYPE_AUDIO] = audioTrackNames;

        MultiTrackChunkSource[] multiTrackChunkSources =
                new MultiTrackChunkSource[ThePlayer.RENDERER_COUNT];
        multiTrackChunkSources[ThePlayer.TYPE_AUDIO] = audioChunkSource;

        TrackRenderer[] renderers = new TrackRenderer[ThePlayer.RENDERER_COUNT];
        renderers[ThePlayer.TYPE_AUDIO] = audioRenderer1;
        callback.onRenderers(trackNames, multiTrackChunkSources, renderers);

    }


}



