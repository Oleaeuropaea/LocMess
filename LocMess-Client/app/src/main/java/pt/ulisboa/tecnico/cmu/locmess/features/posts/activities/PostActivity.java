package pt.ulisboa.tecnico.cmu.locmess.features.posts.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.View;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessApplication;
import pt.ulisboa.tecnico.cmu.locmess.background.services.LocMessBackgroundService;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.fragments.PostFragment;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.activities.BaseActivity;
import pt.ulisboa.tecnico.cmu.locmess.utils.NotificationSender;


public class PostActivity extends BaseActivity {
    public static final String TAG = PostActivity.class.getCanonicalName();
    static final int CREATE_NEW_POST = 1;

    private PostFragment.SectionsPostAdapter mSectionsPostAdapter;
    private ViewPager mViewPager;
    private int mDrawerItemId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        super.onCreateDrawer(toolbar);
        // Hack to fix drawer menu highlight
        mDrawerItemId = BaseActivity.sCurrDrawerItemId;

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), PostCreationActivity.class);
                startActivityForResult(intent, CREATE_NEW_POST);
            }
        });

        PostFragment.init((LocMessApplication) getApplicationContext());

        mViewPager = (ViewPager) findViewById(R.id.container);
        setSectionsPostAdapter(PostFragment.NEARBY_POST);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);


        LocMessApplication application = (LocMessApplication) getApplicationContext();
        NotificationSender.disableNotification(application);

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Hack to fix drawer menu highlight
        BaseActivity.sCurrDrawerItemId = mDrawerItemId;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CREATE_NEW_POST) {
            if (resultCode == RESULT_OK) {
                setSectionsPostAdapter(PostFragment.POSTED_POST);
            }
        }
    }

    public void setSectionsPostAdapter(int position) {
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPostAdapter = new PostFragment.SectionsPostAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager.setAdapter(mSectionsPostAdapter);
        mViewPager.setCurrentItem(position);
        mViewPager.getAdapter().notifyDataSetChanged();
    }
}
