package com.dotzenrod.notekeeper;

import android.app.LoaderManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.dotzenrod.notekeeper.NoteKeeperDatabaseContract.CourseInfoEntry;
import com.dotzenrod.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry;

import java.util.List;
import java.util.PropertyResourceBundle;

import static com.dotzenrod.notekeeper.NoteActivity.LOADER_NOTES;
import static com.dotzenrod.notekeeper.NoteKeeperProviderContract.*;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, LoaderManager.LoaderCallbacks<Cursor>  {
    public static final int NOTE_UPLOADER_JOB_ID = 1;
    private NoteRecyclerAdapter mNoteRecyclerAdapter;
    private RecyclerView mRecyclerItems;
    private LinearLayoutManager mNotesLayoutManager;
    private CourseRecyclerAdapter mCourseRecyclerAdapter;
    private GridLayoutManager mCoursesLayoutManager;
    private NoteKeeperOpenHelper mDbOpenHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        enableStrictMode();

        mDbOpenHelper = new NoteKeeperOpenHelper(this);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, NoteActivity.class));
            }
        });

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, false);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        initializeDisplayContent();
    }

    private void enableStrictMode() {
        if(BuildConfig.DEBUG) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build();
            StrictMode.setThreadPolicy(policy);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(LOADER_NOTES, null, this);
        updateNavHeader();
        openDrawer();
    }

    public void openDrawer() {
        SharedPreferences sp = getApplicationContext().getSharedPreferences("yoursharedprefs", 0);
        boolean isFirstStart = sp.getBoolean("key", true);

        if(isFirstStart) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                    drawer.openDrawer(Gravity.START);
                }
            }, 500);
            SharedPreferences.Editor e = sp.edit();
            //we save the value "false", indicating that it is no longer the first appstart
            e.putBoolean("key", false);
            e.commit();
        }
    }

    private void loadNotes() {
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
        final String[] noteColumns = {
                NoteInfoEntry.COLUMN_NOTE_TITLE,
                NoteInfoEntry.COLUMN_COURSE_ID,
                NoteInfoEntry._ID};
        String noteOrderBy = NoteInfoEntry.COLUMN_COURSE_ID + "," + NoteInfoEntry.COLUMN_NOTE_TITLE;
        final Cursor noteCursor = db.query(NoteInfoEntry.TABLE_NAME, noteColumns,
                null, null, null, null, noteOrderBy);
        mNoteRecyclerAdapter.changeCursor(noteCursor);
    }

    @Override
    protected void onDestroy() {
        mDbOpenHelper.close();
        super.onDestroy();
    }

    private void updateNavHeader() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        TextView textUserName = (TextView)headerView.findViewById(R.id.text_user_name);
        TextView textEmailAddress = (TextView)headerView.findViewById(R.id.text_email_address);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String userName = pref.getString("user_display_name", "");
        String emailAddress = pref.getString("user_email_address", "");

        textUserName.setText(userName);
        textEmailAddress.setText(emailAddress);
    }

    private void initializeDisplayContent() {
        DataManager.loadFromDatabase(mDbOpenHelper);
        mRecyclerItems = (RecyclerView) findViewById(R.id.list_items);
        mNotesLayoutManager = new LinearLayoutManager(this);
        mCoursesLayoutManager = new GridLayoutManager(this, getResources().getInteger(R.integer.course_grid_span));

        mNoteRecyclerAdapter = new NoteRecyclerAdapter(this, null);

        List<CourseInfo> courses = DataManager.getInstance().getCourses();
        mCourseRecyclerAdapter = new CourseRecyclerAdapter(this, courses);

        displayNotes();
    }

    private void displayNotes() {
        mRecyclerItems.setLayoutManager(mNotesLayoutManager);
        mRecyclerItems.setAdapter(mNoteRecyclerAdapter);

        selectNavigationMenuItem(R.id.nav_notes);
    }

    private void selectNavigationMenuItem(int id) {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        Menu menu = navigationView.getMenu();
        menu.findItem(id).setChecked(true);
    }

    private void displayCourses(){
        mRecyclerItems.setLayoutManager(mCoursesLayoutManager);
        mRecyclerItems.setAdapter(mCourseRecyclerAdapter);

        selectNavigationMenuItem(R.id.nav_courses);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if ( id == R.id.action_backup_note){
            backupNotes();
        }

        return super.onOptionsItemSelected(item);
    }

    private void scheduledNoteUpload(){
        PersistableBundle extras = new PersistableBundle();
        extras.putString(NoteUploaderJobService.EXTRA_DATA_URI, Notes.CONTENT_URI.toString());

        ComponentName componentName = new ComponentName(this, NoteUploaderJobService.class);
        JobInfo jobInfo = new JobInfo.Builder(NOTE_UPLOADER_JOB_ID, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setExtras(extras)
                .build();

        JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(jobInfo);
    }

    private void backupNotes() {
        Intent intent = new Intent(this, NoteBackupService.class);
        intent.putExtra(NoteBackupService.EXTRA_COURSE_ID, NoteBackup.ALL_COURSES);
        startService(intent);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_notes) {
            displayNotes();
        } else if (id == R.id.nav_courses) {
            displayCourses();
        }  else if (id == R.id.nav_share) {
            handleShare();            
        } else if (id == R.id.nav_send) {
            handleSelection(R.string.nav_send_message);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void handleShare() {
        View view = findViewById(R.id.list_items);
        Snackbar.make(view, "Share to - " +
                PreferenceManager.getDefaultSharedPreferences(this).getString("user_favorite_social", ""),
                Snackbar.LENGTH_LONG).show();
    }

    private void handleSelection(int message_id) {
        View view = findViewById(R.id.list_items);
        Snackbar.make(view, message_id, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = null;
        if(id == LOADER_NOTES) {
            final String[] noteColumns = {
                    Notes._ID,
                    Notes.COLUMN_NOTE_TITLE,
                    Notes.COLUMN_COURSE_TITLE
            };
            final String noteOrderBy = Notes.COLUMN_COURSE_TITLE +
                    "," + Notes.COLUMN_NOTE_TITLE;

            loader = new CursorLoader(this, Notes.CONTENT_EXPANDED_URI, noteColumns,
                    null, null, noteOrderBy);
        }
        return loader;
    }


    @Override
    public void onLoadFinished(Loader loader, Cursor data) {
        if(loader.getId() == LOADER_NOTES)  {
            mNoteRecyclerAdapter.changeCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        if(loader.getId() == LOADER_NOTES)  {
            mNoteRecyclerAdapter.changeCursor(null);
        }

    }
}
