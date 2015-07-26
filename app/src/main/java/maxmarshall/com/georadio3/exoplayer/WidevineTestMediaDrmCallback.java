package maxmarshall.com.georadio3.exoplayer;

import android.media.MediaDrm;
import android.text.TextUtils;

import com.google.android.exoplayer.drm.MediaDrmCallback;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Max on 09/07/2015.
 */
/*
public class WidevineTestMediaDrmCallback implements MediaDrmCallback {

    private static final String WIDEVINE_GTS_DEFAULT_BASE_URI =
            "http://wv-staging-proxy.appspot.com/proxy?provider=YouTube&video_id=";

    private final String defaultUri;

    public WidevineTestMediaDrmCallback(String videoId) {
        defaultUri = WIDEVINE_GTS_DEFAULT_BASE_URI + videoId;
    }

    @Override
    public byte[] executeProvisionRequest(UUID uuid, MediaDrm.ProvisionRequest request) throws IOException {
        String url = request.getDefaultUrl() + "&signedRequest=" + new String(request.getData());
        return GeoUtil.executePost(url, null, null);
    }

    @Override
    public byte[] executeKeyRequest(UUID uuid, MediaDrm.KeyRequest request) throws IOException {
        String url = request.getDefaultUrl();
        if (TextUtils.isEmpty(url)) {
            url = defaultUri;
        }
        return GeoUtil.executePost(url, request.getData(), null);
    }

}
*/