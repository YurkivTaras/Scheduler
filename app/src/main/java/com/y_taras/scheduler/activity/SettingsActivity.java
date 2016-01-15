package com.y_taras.scheduler.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.y_taras.scheduler.R;

import com.y_taras.scheduler.other.StringKeys;
import yuku.ambilwarna.AmbilWarnaDialog;

public class SettingsActivity extends AppCompatActivity {

    private static int defaultNotStartedTaskColor;
    private static int defaultStartedTaskColor;
    private static int defaultCompletedTaskColor;

    private int mCompletedTaskColor;
    private int mStartedTaskColor;
    private int mNotStartedTaskColor;
    private int mMaxRuntimeForTask;

    private static final int NotStartedTask = 1;
    private static final int StartedTask = 2;
    private static final int CompletedTask = 3;

    private Toast mToast;

    private Button mCompletedTaskBtn;
    private Button mStartedTaskBtn;
    private Button mNotStartedTaskBtn;
    private EditText mMaxRuntimeEditTxt;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initUI(savedInstanceState);
    }

    private void initUI(Bundle savedInstanceState) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarForSettingsActivity);
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
            mMaxRuntimeForTask = intent.getIntExtra(StringKeys.MAX_RUNTIME_FOR_TASK, 60);
        } else {
            mNotStartedTaskColor = savedInstanceState.getInt(StringKeys.NOT_STARTED_TASK);
            mStartedTaskColor = savedInstanceState.getInt(StringKeys.STARTED_TASK);
            mCompletedTaskColor = savedInstanceState.getInt(StringKeys.COMPLETED_TASK);
            mMaxRuntimeForTask = savedInstanceState.getInt(StringKeys.MAX_RUNTIME_FOR_TASK);
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

        mMaxRuntimeEditTxt = (EditText) findViewById(R.id.editTxtMaxRuntime);
        mMaxRuntimeEditTxt.setText(String.format("%d", mMaxRuntimeForTask));
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
        outState.putInt(StringKeys.MAX_RUNTIME_FOR_TASK, Integer.parseInt(mMaxRuntimeEditTxt.getText().toString()));
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
        String maxRuntime = mMaxRuntimeEditTxt.getText().toString();
        if (maxRuntime.length() == 0) {
            if (mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(this, R.string.error_edit_txt_for_maxRuntime, Toast.LENGTH_SHORT);
            mToast.show();
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(StringKeys.NOT_STARTED_TASK, mNotStartedTaskColor);
        intent.putExtra(StringKeys.STARTED_TASK, mStartedTaskColor);
        intent.putExtra(StringKeys.COMPLETED_TASK, mCompletedTaskColor);
        intent.putExtra(StringKeys.MAX_RUNTIME_FOR_TASK, Integer.parseInt(maxRuntime));
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }
}
