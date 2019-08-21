package pt.ulisboa.tecnico.cmu.locmess.features.shared.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.InterestLocMess;
import pt.ulisboa.tecnico.cmu.locmess.utils.NetworkUtils;


public class InterestAdapter extends ArrayAdapter<InterestLocMess> {
    public static final String TAG = InterestAdapter.class.getCanonicalName();

    public interface NoticeDeleteInterestListener {
        void onDeleteInterestClick(InterestLocMess interest);
    }

    private final List<InterestLocMess> mInterestList;
    private final Context context;
    private Object listener;

    public InterestAdapter(Context context, int resource, List<InterestLocMess> interestList) {
        super(context, resource, interestList);
        this.context = context;
        this.mInterestList = interestList;
    }

    public List<InterestLocMess> getInterestList() {
        return mInterestList;
    }

    public void setListener(Object listener) {
        this.listener = listener;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final InterestLocMess interest = mInterestList.get(position);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.item_interest_list, null);

        TextView nameTextView = (TextView) view.findViewById(R.id.tv_location_name);
        nameTextView.setText(interest.getName());

        TextView value_textView = (TextView) view.findViewById(R.id.tv_profile_value);
        value_textView.setText(interest.getValue());

        ImageButton deleteBtn = (ImageButton) view.findViewById(R.id.ab_frequent_location_remove);

        if (NetworkUtils.isNetworkAvailable()) {
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((NoticeDeleteInterestListener) listener).onDeleteInterestClick(interest);
                }
            });
        } else {
            deleteBtn.setVisibility(View.INVISIBLE);
        }

        return view;
    }
}
