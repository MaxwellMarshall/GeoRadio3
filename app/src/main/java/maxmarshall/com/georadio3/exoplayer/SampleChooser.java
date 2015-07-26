package maxmarshall.com.georadio3.exoplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import maxmarshall.com.georadio3.R;


/**
 * Created by Max on 08/07/2015.
 */
public class SampleChooser extends Activity  {

    private static final String TAG = "SampleChooserActivity";

    private Button geofencing;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_chooser_activity);

        ListView sampleList = (ListView) findViewById(R.id.sample_list);
        final SampleAdapter sampleAdapter = new SampleAdapter(this);

        sampleAdapter.add(new Header("YouTube DASH"));
        sampleAdapter.addAll((Object[]) Samples.YOUTUBE_DASH_MP4);

        sampleList.setAdapter(sampleAdapter);
        sampleList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object item = sampleAdapter.getItem(position);
                if (item instanceof Samples.Sample) {
                    onSampleSelected((Samples.Sample) item);
                }
            }
        });


    }

    private void onSampleSelected(Samples.Sample sample) {
        Intent mpdIntent = new Intent(this, PlayerActivity.class)
                .setData(Uri.parse(sample.uri))
                .putExtra(PlayerActivity.CONTENT_ID_EXTRA, sample.contentId)
                .putExtra(PlayerActivity.CONTENT_TYPE_EXTRA, sample.type);
        startActivity(mpdIntent);
    }

    private static class SampleAdapter extends ArrayAdapter<Object> {

        public SampleAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                int layoutId = getItemViewType(position) == 1 ? android.R.layout.simple_list_item_1
                        : R.layout.sample_chooser_inline_header;
                view = LayoutInflater.from(getContext()).inflate(layoutId, null, false);
            }
            Object item = getItem(position);
            String name = null;
            if (item instanceof Samples.Sample) {
                name = ((Samples.Sample) item).name;
            } else if (item instanceof Header) {
                name = ((Header) item).name;
            }
            ((TextView) view).setText(name);
            return view;
        }
        @Override
        public int getItemViewType(int position) {
            return (getItem(position) instanceof Samples.Sample) ? 1 : 0;
        }



    @Override
    public int getViewTypeCount() {
        return 2;
    }

}

    private static class Header {

        public final String name;

        public Header(String name) {
            this.name = name;
        }

    }



}

