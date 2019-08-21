package pt.ulisboa.tecnico.cmu.locmess.features.shared.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessApplication;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessPreferences;
import pt.ulisboa.tecnico.cmu.locmess.background.managers.InterestManager;
import pt.ulisboa.tecnico.cmu.locmess.background.managers.LocationManager;
import pt.ulisboa.tecnico.cmu.locmess.background.services.LocMessBackgroundService;
import pt.ulisboa.tecnico.cmu.locmess.features.authentication.login.LoginActivity;
import pt.ulisboa.tecnico.cmu.locmess.features.locations.activites.LocationActivity;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.activities.PostActivity;
import pt.ulisboa.tecnico.cmu.locmess.features.profile.activities.ProfileActivity;
import pt.ulisboa.tecnico.cmu.locmess.features.configuration.activites.SettingsActivity;
import pt.ulisboa.tecnico.cmu.locmess.utils.NetworkUtils;


public abstract class BaseActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    public static final String TAG = BaseActivity.class.getCanonicalName();

    protected static int sCurrDrawerItemId = R.id.nav_posts;
    protected NavigationView mNavigationView;

    protected InterestManager mInterestManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        chooseStatusBarColor();
    }

    protected void onCreateDrawer(Toolbar toolbar) {
        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        LocMessApplication application = (LocMessApplication) getApplicationContext();
        LocMessBackgroundService locMessBackgroundService;
        while(true) {
            if (!((locMessBackgroundService = application.locMessBackgroundService) == null)) break;
        }
        mInterestManager = locMessBackgroundService.interestManager;

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            // Hack to fix drawer menu highlight because we use activity's instead of fragments
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                mNavigationView.getMenu().findItem(sCurrDrawerItemId).setChecked(true);
            }
        };
        drawer.setDrawerListener(toggle);
        toggle.syncState();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        sCurrDrawerItemId = item.getItemId();

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        switch (item.getItemId()) {
            case R.id.nav_posts:
                intent.setClass(this, PostActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_locations:
                intent.setClass(this, LocationActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_profile:
                intent.setClass(this, ProfileActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_settings:
                intent.setClass(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_logout:
                sCurrDrawerItemId = R.id.nav_posts; // Hack because drawer uses activity
                handleLogOut();
                break;
            default:
                Log.wtf(TAG, "Menu item don't exist");
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void chooseStatusBarColor() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        if (NetworkUtils.isNetworkAvailable()) {
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        } else {
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.offlineMode));
        }
    }

    private void handleLogOut() {
        LocMessApplication application = (LocMessApplication) getApplicationContext();
        application.disableLocMessBackgroundService();

        LocMessPreferences.getInstance().clearPreferences();
        NetworkUtils.logoutUserTask(this);

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        finish();
    }
}
