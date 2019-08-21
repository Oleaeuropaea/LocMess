package pt.ulisboa.tecnico.cmu.locmess.features.locations.adapters;


import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.features.locations.LocationLocMess;

public class LocationsAdapter extends ArrayAdapter<LocationLocMess> {
    private Context context;
    private ArrayList<LocationLocMess> locations;


    public LocationsAdapter(Context context, int resource, ArrayList<LocationLocMess> locations) {
        super(context, resource, locations);
        this.context = context;
        this.locations = locations;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LocationLocMess location = locations.get(position);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.item_location_list, null);
        TextView locationName = (TextView) view.findViewById(R.id.location_name);
        locationName.setText(location.getName());
        TextView locationType = (TextView) view.findViewById(R.id.location_type);
        locationType.setText(location.getType());
        return view;
    }
}
