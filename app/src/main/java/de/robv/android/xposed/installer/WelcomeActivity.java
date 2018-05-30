package de.robv.android.xposed.installer;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.LinearLayout;
import android.widget.Toast;

import de.robv.android.xposed.installer.installation.StatusInstallerFragment;
import de.robv.android.xposed.installer.util.Loader;
import de.robv.android.xposed.installer.util.ModuleUtil;
import de.robv.android.xposed.installer.util.ModuleUtil.InstalledModule;
import de.robv.android.xposed.installer.util.ModuleUtil.ModuleListener;
import de.robv.android.xposed.installer.util.RepoLoader;
import de.robv.android.xposed.installer.util.ThemeUtil;

public class WelcomeActivity extends XposedBaseActivity implements NavigationView.OnNavigationItemSelectedListener,
        ModuleListener, Loader.Listener<RepoLoader> {

    private static final String SELECTED_ITEM_ID = "SELECTED_ITEM_ID";
    private final Handler mDrawerHandler = new Handler();
    private RepoLoader mRepoLoader;
    private DrawerLayout mDrawerLayout;
    private int mPrevSelectedId;
    private NavigationView mNavigationView;
    private int mSelectedId;
    private long mLastPressTime;
    private SparseArrayCompat<Fragment> mFragmentContainer;
    private Fragment mPreFragment;
    private SparseIntArray mTitleArray = new SparseIntArray(4);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtil.setTheme(this);
        setContentView(R.layout.activity_welcome);

        mFragmentContainer = new SparseArrayCompat<>(4);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
        assert mNavigationView != null;
        mNavigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this,
                mDrawerLayout, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                // this disables the arrow @ completed state
                super.onDrawerSlide(drawerView, 0);
                Log.d("WelcomeActivity", "onDrawerOpened");
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                // this disables the animation
                super.onDrawerSlide(drawerView, 0);
                Log.d("WelcomeActivity", "onDrawerSlide");
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                Log.d("WelcomeActivity", "onDrawerClosed");

            }
        };

        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean xposedActive = XposedApp.isXposedActive();
        mSelectedId = mNavigationView.getMenu().getItem(xposedActive ? 3 : 0).getItemId();
        mSelectedId = savedInstanceState == null ? mSelectedId : savedInstanceState.getInt(SELECTED_ITEM_ID);
        mPrevSelectedId = mSelectedId;
        mNavigationView.getMenu().findItem(mSelectedId).setChecked(true);

        Log.d("WelcomeActivity", "mSelectedId:" + mSelectedId);
        if (savedInstanceState == null) {
            mDrawerHandler.removeCallbacksAndMessages(null);
            mDrawerHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    navigate(mSelectedId);
                }
            }, 250);

            boolean openDrawer = prefs.getBoolean("open_drawer", false);

            if (openDrawer) {
                mDrawerLayout.openDrawer(GravityCompat.START);
            } else {
                mDrawerLayout.closeDrawers();
            }
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int value = extras.getInt("fragment", prefs.getInt("default_view", 0));
            switchFragment(value);
        }

        mRepoLoader = RepoLoader.getInstance();
        ModuleUtil.getInstance().addListener(this);
        mRepoLoader.addListener(this);

        notifyDataSetChanged();
    }

    public void switchFragment(int itemId) {
        mSelectedId = mNavigationView.getMenu().getItem(itemId).getItemId();
        mNavigationView.getMenu().findItem(mSelectedId).setChecked(true);
        mDrawerHandler.removeCallbacksAndMessages(null);
        mDrawerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                navigate(mSelectedId);
            }
        }, 250);
        mDrawerLayout.closeDrawers();
    }

    private void navigate(final int itemId) {
        final View elevation = findViewById(R.id.elevation);
        Fragment navFragment = mFragmentContainer.get(itemId);
        int titleId = mTitleArray.get(itemId);
        if (navFragment == null) {
            switch (itemId) {
                case R.id.nav_item_framework:
                    titleId = R.string.app_name;
                    navFragment = new StatusInstallerFragment();
                    break;
                case R.id.nav_item_modules:
                    titleId = R.string.nav_item_modules;
                    navFragment = new ModulesFragment();
                    break;
                case R.id.nav_item_downloads:
                    titleId = R.string.nav_item_download;
                    navFragment = new DownloadFragment();
                    break;
                case R.id.nav_item_logs:
                    titleId = R.string.nav_item_logs;
                    navFragment = new LogsFragment();
                    break;
                case R.id.nav_item_settings:
                    startActivity(new Intent(this, SettingsActivity.class));
                    return;
                case R.id.nav_item_support:
                    startActivity(new Intent(this, SupportActivity.class));
                    return;
                case R.id.nav_item_about:
                    startActivity(new Intent(this, AboutActivity.class));
                    return;
                default:
                    break;
            }
            if (navFragment != null) {
                mFragmentContainer.put(itemId, navFragment);
                mTitleArray.put(itemId, titleId);
            }
        }

        mPrevSelectedId = itemId;
        mNavigationView.getMenu().findItem(mPrevSelectedId).setChecked(true);
        setTitle(mTitleArray.get(itemId));

        if (navFragment != null) {
            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4));
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);
            try {
                if (mPreFragment == null) {
                    transaction.add(R.id.content_frame, navFragment).commit();
                } else {
                    if (mPreFragment == navFragment) {
                        return;
                    }
                    if (navFragment.isAdded()) {
                        transaction.hide(mPreFragment)
                                .show(navFragment)
                                .commit();
                    } else {
                        transaction.add(R.id.content_frame, navFragment)
                                .hide(mPreFragment)
                                .commit();
                    }

                }
                mPreFragment = navFragment;
                if (elevation != null) {
                    Animation a = new Animation() {
                        @Override
                        protected void applyTransformation(float interpolatedTime, Transformation t) {
                            elevation.setLayoutParams(params);
                        }
                    };
                    a.setDuration(150);
                    elevation.startAnimation(a);
                }
            } catch (IllegalStateException ignored) {
            }
        }
    }

    public int dp(float value) {
        float density = getApplicationContext().getResources().getDisplayMetrics().density;
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        menuItem.setChecked(true);
        mSelectedId = menuItem.getItemId();
        mDrawerHandler.removeCallbacksAndMessages(null);
        mDrawerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                navigate(mSelectedId);
            }
        }, 250);
        mDrawerLayout.closeDrawers();
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_ITEM_ID, mSelectedId);
    }

    @Override
    public void onBackPressed() {
        int itemId = mNavigationView.getMenu().getItem(0).getItemId();
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else if (mPrevSelectedId != itemId) {
            navigate(itemId);
        } else {
            long millis = System.currentTimeMillis();
            if ((millis - mLastPressTime) < 2000L) {
                Log.d("WelcomeActivity", "我还会再回来的");
//                Toast.makeText(this, "我还会再回来的", Toast.LENGTH_SHORT).show();
                super.onBackPressed();
            } else {
                Log.d("WelcomeActivity", "再点一次我就走");
                Toast.makeText(this, "再点一次我就走", Toast.LENGTH_SHORT).show();
                mLastPressTime = millis;
            }
        }
    }

    private void notifyDataSetChanged() {
        View parentLayout = findViewById(R.id.content_frame);
        String frameworkUpdateVersion = mRepoLoader.getFrameworkUpdateVersion();
        boolean moduleUpdateAvailable = mRepoLoader.hasModuleUpdates();

        Fragment currentFragment = getFragmentManager().findFragmentById(R.id.content_frame);
        if (currentFragment instanceof DownloadDetailsFragment) {
            if (frameworkUpdateVersion != null) {
                Snackbar.make(parentLayout, R.string.welcome_framework_update_available + " " + String.valueOf(frameworkUpdateVersion), Snackbar.LENGTH_LONG).show();
            }
        }

        boolean snackBar = XposedApp.getPreferences().getBoolean("snack_bar", true);

        if (moduleUpdateAvailable && snackBar) {
            Snackbar.make(parentLayout, R.string.modules_updates_available, Snackbar.LENGTH_SHORT).setAction(getString(R.string.view), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switchFragment(2);
                }
            }).show();
        }
    }

    @Override
    public void onInstalledModulesReloaded(ModuleUtil moduleUtil) {
        notifyDataSetChanged();
    }

    @Override
    public void onSingleInstalledModuleReloaded(ModuleUtil moduleUtil, String packageName, InstalledModule module) {
        notifyDataSetChanged();
    }

    @Override
    public void onReloadDone(RepoLoader loader) {
        notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ModuleUtil.getInstance().removeListener(this);
        mRepoLoader.removeListener(this);
    }

}
