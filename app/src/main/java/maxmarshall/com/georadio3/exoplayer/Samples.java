package maxmarshall.com.georadio3.exoplayer;


import java.util.Locale;

/**
 * Created by Max on 08/07/2015.
 */


 class Samples {

    public static final int TYPE_DASH = 0;
    public static class Sample {


        public final String name;
        public final String contentId;
        public final String uri;
        public final int type;


        public Sample(String name, String uri, int type) {
            this(name, name.toLowerCase(Locale.UK).replaceAll("\\s", ""), uri, type);
        }

        public Sample(String name, String contentId, String uri, int type) {
            this.name = name;
            this.contentId = contentId;
            this.uri = uri;
            this.type = type;
        }
    }


 public static final Sample[] YOUTUBE_DASH_MP4 = new Sample[] {
         new Sample("Google Glass sample",
                 "http://www.youtube.com/api/manifest/dash/id/bf5bb2419360daf1/source/youtube?"
                         + "as=fmp4_audio_clear,fmp4_sd_hd_clear&sparams=ip,ipbits,expire,source,id,as&ip=0.0.0.0&"
                         + "ipbits=0&expire=19000000000&signature=51AF5F39AB0CEC3E5497CD9C900EBFEAECCCB5C7."
                         + "8506521BFC350652163895D4C26DEE124209AA9E&key=ik0", TYPE_DASH),
         new Sample("Google Play",
                 "http://www.youtube.com/api/manifest/dash/id/3aa39fa2cc27967f/source/youtube?"
                         + "as=fmp4_audio_clear,fmp4_sd_hd_clear&sparams=ip,ipbits,expire,source,id,as&ip=0.0.0.0&"
                         + "ipbits=0&expire=19000000000&signature=A2716F75795F5D2AF0E88962FFCD10DB79384F29."
                         + "84308FF04844498CE6FBCE4731507882B8307798&key=ik0", TYPE_DASH),
         new Sample("my test",
                 "http://maxmarshall.ddns.net/segments/TEST_dash.mpd", TYPE_DASH)
 };
}