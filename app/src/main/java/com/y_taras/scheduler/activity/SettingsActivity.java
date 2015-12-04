package com.y_taras.scheduler.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.example.scheduler.R;

import other.StringKeys;
import yuku.ambilwarna.AmbilWarnaDialog;

public class SettingsActivity extends ActionBarActivity {
    private Toolbar toolbar;

    private static int defaultNotStartedTaskColor;
    private static int defaultStartedTaskColor;
    private static int defaultCompletedTaskColor;

    private int mCompletedTaskColor;
    private int mStartedTaskColor;
    private int mNotStartedTaskColor;

    private static final int NotStartedTask = 1;
    private static final int StartedTask = 2;
    private static final int CompletedTask = 3;


    private Button mCompletedTaskBtn;
    private Button mStartedTaskBtn;
    private Button mNotStartedTaskBtn;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initUI(savedInstanceState);
    }

    private void initUI(Bundle savedInstanceState) {
        toolbar = (Toolbar) findViewById(R.id.toolbarForSettingsActivity);
        toolbar.setTitle(R.string.settingsToolbarTitle);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        defaultNotStartedTaskColor = getResources().getColor(R.color.not_started_task);
        defaultStartedTaskColor = getResources().getColor(R.color.started_task);
        defaultCompletedTaskColor = getResources().getColor(R.color.completed_task);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            mNotStartedTaskColor = intent.getIntExtra(StringKeys.NOT_STARTED_TASK, defaultNotStartedTaskColor);
            mStartedTaskColor = intent.getIntExtra(StringKeys.STARTED_TASK, defaultStartedTaskColor);
            mCompletedTaskColor = intent.getIntExtra(StringKeys.COMPLETED_TASK, defaultCompletedTaskColor);
        } else {
            mNotStartedTaskColor = savedInstanceState.getInt(StringKeys.NOT_STARTED_TASK);
            mStartedTaskColor = savedInstanceState.getInt(StringKeys.STARTED_TASK);
            mCompletedTaskColor = savedInstanceState.getInt(StringKeys.COMPLETED_TASK);
        }

        mNotStartedTaskBtn = (Button) findViewById(R.id.btnNotStartedTask);
        mNotStartedTaskBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getColor(mNotStartedTaskColor, NotStartedTask);
            }
        });
        mStartedTaskBtn = (Button) findViewById(R.id.btnStartedTask);
        mStartedTaskBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getColor(mStartedTaskColor, StartedTask);
            }
        });
        mCompletedTaskBtn = (Button) findViewById(R.id.btnCompletedTask);
        mCompletedTaskBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getColor(mCompletedTaskColor, CompletedTask);
            }
        });
        commitColors();
    }

    public void getColor(int color, final int stateOfTask) {
        AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, color, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int newColor) {
                switch (stateOfTask) {
                    case NotStartedTask:
                        mNotStartedTaskColor = newColor;
                        break;
                    case StartedTask:
                        mStartedTaskColor = newColor;
                        break;
                    case CompletedTask:
                        mCompletedTaskColor = newColor;
                        break;
                }
                commitColors();
            }
        });
        dialog.show();
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(StringKeys.NOT_STARTED_TASK, mNotStartedTaskColor);
        outState.putInt(StringKeys.STARTED_TASK, mStartedTaskColor);
        outState.putInt(StringKeys.COMPLETED_TASK, mCompletedTaskColor);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_reset_colors:
                mNotStartedTaskColor = defaultNotStartedTaskColor;
                mStartedTaskColor = defaultStartedTaskColor;
                mCompletedTaskColor = defaultCompletedTaskColor;
                break;
        }
        commitColors();
        return super.onOptionsItemSelected(item);
    }

    private void commitColors() {
        mNotStartedTaskBtn.setBackgroundColor(mNotStartedTaskColor);
        mStartedTaskBtn.setBackgroundColor(mStartedTaskColor);
        mCompletedTaskBtn.setBackgroundColor(mCompletedTaskColor);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(StringKeys.NOT_STARTED_TASK, mNotStartedTaskColor);
        intent.putExtra(StringKeys.STARTED_TASK, mStartedTaskColor);
        intent.putExtra(StringKeys.COMPLETED_TASK, mCompletedTaskColor);
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }
}
