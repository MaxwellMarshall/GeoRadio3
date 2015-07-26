package maxmarshall.com.georadio3.exoplayer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Created by Max on 09/07/2015.
 */
public class GeoUtil {
    public static final int TYPE_DASH = 0;


    private static final CookieManager defaultCookieManager;

    static {
        defaultCookieManager = new CookieManager();
        defaultCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    public static byte[] executePost(String url, byte[] data, Map<String, String> requestProperties)
            throws IOException {
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(data != null);
            urlConnection.setDoInput(true);
            if (requestProperties != null) {
                for (Map.Entry<String, String> requestProperty : requestProperties.entrySet()) {
                    urlConnection.setRequestProperty(requestProperty.getKey(), requestProperty.getValue());
                }
            }
            if (data != null) {
                OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                out.write(data);
                out.close();
            }
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            return convertInputStreamToByteArray(in);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private static byte[] convertInputStreamToByteArray(InputStream inputStream) throws IOException {
        byte[] bytes = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte data[] = new byte[1024];
        int count;
        while ((count = inputStream.read(data)) != -1) {
            bos.write(data, 0, count);
        }
        bos.flush();
        bos.close();
        inputStream.close();
        bytes = bos.toByteArray();
        return bytes;
    }

    public static void setDefaultCookieManager() {
        CookieHandler currentHandler = CookieHandler.getDefault();
        if (currentHandler != defaultCookieManager) {
            CookieHandler.setDefault(defaultCookieManager);
        }
    }

}


