package pt.ulisboa.tecnico.cmu.locmess.features.posts.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessApplication;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.PostLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.activities.PostDetailActivity;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.adapters.PostAdapter;

public class PostFragment extends Fragment implements PostAdapter.ListItemClickListener {
    public static final String TAG = PostFragment.class.getCanonicalName();
    public static final String EXTRA_POST =
            "pt.ulisboa.tecnico.cmu.locmess.features.posts.fragments.EXTRA_POST";
    public static final String EXTRA_POST_TYPE =
            "pt.ulisboa.tecnico.cmu.locmess.features.posts.fragments.EXTRA_POST_TYPE";
    public static final String SYNCHRONIZE_POSTS = "pt.ulisboa.tecnico.cmu.locmess.features.locations.activites.NEW_POSTS";


    public static final int NEARBY_POST = 0;
    public static final int POSTED_POST = 1;
    public static final int SAVED_POST = 2;

    private RecyclerView mPostList;
    private PostAdapter mPostAdapter;
    private int mCurrentSection;

    private BroadcastReceiver br;

    private static LocMessApplication applicationContext;
    private BroadcastReceiver receiver;

    @Override
    public void onResume() {
        super.onResume();
        mPostAdapter.notifyDataSetChanged();
        registerBroadcastReceiver();
    }

    public static void init(LocMessApplication applicationContext) {
        PostFragment.applicationContext = applicationContext;
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static PostFragment newInstance(int sectionNumber) {
        PostFragment fragment = new PostFragment();

        switch (sectionNumber) {
            case NEARBY_POST:
                fragment.mCurrentSection = NEARBY_POST;
                fragment.mPostAdapter = new PostAdapter(applicationContext.locMessBackgroundService.postsManager.getNearbyPosts(), fragment);
                break;
            case POSTED_POST:
                fragment.mCurrentSection = POSTED_POST;
                fragment.mPostAdapter = new PostAdapter(applicationContext.locMessBackgroundService.postsManager.getPostedPosts(), fragment);
                break;
            case SAVED_POST:
                fragment.mCurrentSection = SAVED_POST;
                fragment.mPostAdapter = new PostAdapter(applicationContext.locMessBackgroundService.postsManager.getSavedPosts(), fragment);
                break;
            default:
                Log.wtf(TAG, "Section don't exist");
        }

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_post, container, false);

        mPostList = (RecyclerView) rootView.findViewById(R.id.rv_posts);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mPostList.setLayoutManager(layoutManager);
        mPostList.setHasFixedSize(true);

        mPostList.setAdapter(mPostAdapter);


        setupBroadcastReceiver();

        return rootView;
    }

    private void setupBroadcastReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG,"INTENT RECEIVED");
                mPostAdapter.notifyDataSetChanged();
            }
        };
    }
    public void registerBroadcastReceiver() {

        //Responsible for refreshing List if inserted on Server
        IntentFilter intentFilter = new IntentFilter(SYNCHRONIZE_POSTS);
        getActivity().registerReceiver(receiver, intentFilter);
    }



    @Override
    public void onListItemClick(PostLocMess post) {
        Intent intent = new Intent(getContext(), PostDetailActivity.class);
        intent.putExtra(EXTRA_POST, post);

        switch (mCurrentSection) {
            case NEARBY_POST:
                intent.putExtra(EXTRA_POST_TYPE, NEARBY_POST);
                break;
            case POSTED_POST:
                intent.putExtra(EXTRA_POST_TYPE, POSTED_POST);
                break;
            case SAVED_POST:
                intent.putExtra(EXTRA_POST_TYPE, SAVED_POST);
                break;
            default:
                Log.wtf(TAG, "Section don't exist");
        }

        startActivity(intent);
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(receiver);
        super.onPause();
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public static class SectionsPostAdapter extends FragmentStatePagerAdapter {

        public SectionsPostAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).

            return PostFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case PostFragment.NEARBY_POST:
                    return "Nearby";
                case PostFragment.POSTED_POST:
                    return "Posted";
                case PostFragment.SAVED_POST:
                    return "Saved";
                default:
                    return null;
            }
        }
    }
}
