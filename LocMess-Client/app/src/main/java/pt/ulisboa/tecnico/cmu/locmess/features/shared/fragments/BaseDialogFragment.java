package pt.ulisboa.tecnico.cmu.locmess.features.shared.fragments;

import android.app.DialogFragment;
import android.content.Context;
import android.content.res.ObbInfo;
import android.os.Bundle;

import pt.ulisboa.tecnico.cmu.locmess.app.LocMessApplication;
import pt.ulisboa.tecnico.cmu.locmess.background.managers.InterestManager;
import pt.ulisboa.tecnico.cmu.locmess.background.managers.LocationManager;
import pt.ulisboa.tecnico.cmu.locmess.background.services.LocMessBackgroundService;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.activities.BaseActivity;

public class BaseDialogFragment extends DialogFragment {
    public interface NoticeDialogListener {
        void onDialogPositiveClick(DialogFragment dialog);
    }

    protected Context mContext;
    protected NoticeDialogListener mListener;

    protected InterestManager mInterestManager;
    protected LocationManager mLocationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocMessApplication application = (LocMessApplication) getActivity().getApplicationContext();
        LocMessBackgroundService locMessBackgroundService;
        while(true) {
            if (!((locMessBackgroundService = application.locMessBackgroundService) == null)) break;
        }
        mInterestManager = locMessBackgroundService.interestManager;
        mLocationManager = locMessBackgroundService.locationsManager;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");
        }
    }

}
