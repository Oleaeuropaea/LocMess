package pt.ulisboa.tecnico.cmu.locmess.features.configuration.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.features.locations.LocationLocMess;


public class FrequentLocationsAdapter extends ArrayAdapter<LocationLocMess> {
    public static final String TAG = FrequentLocationsAdapter.class.getCanonicalName();

    public interface NoticeDeleteFrequentLocationListener {
        void onDeleteFrequentLocationClick(LocationLocMess locationLocMess);
    }

    private final List<LocationLocMess> mLocationList;
    private final Context context;
    private Object listener;

    public FrequentLocationsAdapter(Context context, int resource, List<LocationLocMess> locationList) {
        super(context, resource, locationList);
        this.context = context;
        this.mLocationList = locationList;
    }

    public List<LocationLocMess> getInterestList() {
        return mLocationList;
    }

    public void setListener(Object listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final LocationLocMess location = mLocationList.get(position);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.item_frequent_locations_list, null);

        TextView locationNameTextView = (TextView) view.findViewById(R.id.tv_location_name);
        locationNameTextView.setText(location.getName());

        ImageButton deleteBtn = (ImageButton) view.findViewById(R.id.ab_frequent_location_remove);
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((NoticeDeleteFrequentLocationListener) listener).onDeleteFrequentLocationClick(location);
            }
        });

        return view;
    }
}
