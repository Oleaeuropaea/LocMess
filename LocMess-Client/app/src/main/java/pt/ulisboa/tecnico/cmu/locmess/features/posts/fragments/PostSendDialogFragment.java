package pt.ulisboa.tecnico.cmu.locmess.features.posts.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessApplication;
import pt.ulisboa.tecnico.cmu.locmess.background.managers.InterestManager;
import pt.ulisboa.tecnico.cmu.locmess.background.services.LocMessBackgroundService;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.InterestLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.adapters.InterestAdapter;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.fragments.BaseDialogFragment;
import pt.ulisboa.tecnico.cmu.locmess.utils.NetworkUtils;

public class PostSendDialogFragment extends BaseDialogFragment
        implements InterestAdapter.NoticeDeleteInterestListener {

    InterestAdapter mInterestAdapter;
    ListView mPostInterestsListView;
    List<InterestLocMess> mRestrictionsList = new ArrayList<>();
    private ToggleButton mRestrictionListToggle;

    private View mAddInterestInclude;
    private AutoCompleteTextView mInterestName;
    private EditText mInterestValue;
    private ImageButton mInterestCancelBtn;
    private ImageButton mInterestSaveBtn;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mInterestManager.fetchGlobalInterestsTask(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), getTheme());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_send_post, null);

        mRestrictionListToggle = (ToggleButton) view.findViewById(R.id.toggle);
        mRestrictionListToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRestrictionList();
            }
        });

        mAddInterestInclude = view.findViewById(R.id.add_interest_include);

        ///Launch new dialog to add interest
        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fb_add_restriction);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAddInterestInclude.setVisibility(View.VISIBLE);
            }
        });

        mInterestCancelBtn = (ImageButton) view.findViewById(R.id.ib_cancel);
        mInterestCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInterestName.setError(null);
                mInterestValue.setError(null);
                mInterestName.setText("");
                mInterestValue.setText("");
                mAddInterestInclude.setVisibility(View.GONE);
            }
        });

        mInterestSaveBtn = (ImageButton) view.findViewById(R.id.ib_save);
        mInterestSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveInterest();
            }
        });


        mInterestName = (AutoCompleteTextView) view.findViewById(R.id.actv_name);
        ArrayList<InterestLocMess> interests = new ArrayList<>();
        interests.addAll(mInterestManager.getInterests());
        ArrayAdapter<InterestLocMess> adapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_dropdown_item_1line,
                interests);
        mInterestName.setAdapter(adapter);
        mInterestName.setThreshold(1);
        mInterestName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (mInterestName.getError() == null) {
                    mInterestName.showDropDown();
                }
            }
        });

        mInterestValue = (EditText) view.findViewById(R.id.et_value);

        mPostInterestsListView = (ListView) view.findViewById(R.id.list_restrictions);
        mInterestAdapter = new InterestAdapter(mContext, 0, mRestrictionsList);
        mInterestAdapter.setListener(this);
        mPostInterestsListView.setAdapter(mInterestAdapter);

        RadioButton centralizeMode = (RadioButton) view.findViewById(R.id.rb_spost_centralize);
        RadioButton decentralizeMode = (RadioButton) view.findViewById(R.id.rb_spost_decentralize);
        if (!NetworkUtils.isNetworkAvailable()) {
            centralizeMode.setVisibility(View.GONE);
            decentralizeMode.toggle();
        }


        builder.setView(view)
                .setTitle("Post Parameters")
                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogPositiveClick(PostSendDialogFragment.this);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        getDialog().cancel();
                    }
                });

        return builder.create();
    }

    private void toggleRestrictionList() {
        if (mPostInterestsListView.getVisibility() == View.GONE) {
            mPostInterestsListView.setVisibility(View.VISIBLE);
        } else {
            mPostInterestsListView.setVisibility(View.GONE);
        }
    }

    private void saveInterest() {
        if (TextUtils.isEmpty(mInterestName.getText())) {
            mInterestName.setError(getString(R.string.error_field_required));
            mInterestName.requestFocus();
            return;
        } else if (TextUtils.isEmpty(mInterestValue.getText())) {
            mInterestValue.setError(getString(R.string.error_field_required));
            mInterestValue.requestFocus();
            return;
        }

        String name = mInterestName.getText().toString();
        String value = mInterestValue.getText().toString();

        InterestLocMess interest = new InterestLocMess(name, value);

        if(!mRestrictionsList.contains(interest)) {
            mRestrictionsList.add(interest);
            mInterestAdapter.notifyDataSetChanged();
        }

        // Hide Keyboard
        Dialog dialog = getDialog();
        View view = dialog.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm =
                    (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        mRestrictionListToggle.setText(String.valueOf(mRestrictionsList.size()));
        mInterestName.setText("");
        mInterestValue.setText("");
        mAddInterestInclude.setVisibility(View.GONE);
    }

    public List<InterestLocMess> getRestrictionsList() {
        return mRestrictionsList;
    }

    @Override
    public void onDeleteInterestClick(InterestLocMess interest) {
        mRestrictionsList.remove(interest);
        mInterestAdapter.notifyDataSetChanged();

        mRestrictionListToggle.setText(String.valueOf(mRestrictionsList.size()));
    }
}
