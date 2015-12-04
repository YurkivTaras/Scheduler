package com.y_taras.scheduler.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.example.scheduler.R;

import other.StringKeys;

public class AddScheduleActivity extends ActionBarActivity {
    private EditText mTitle;
    private EditText mComment;
    private String mAction;
    private int mPosTask;
    private Toolbar mToolbar;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_schedule);
        initUI();
    }

    private void initUI() {
        mTitle = (EditText) findViewById(R.id.editTxtTitle);
        mComment = (EditText) findViewById(R.id.editTextComment);

        mToolbar = (Toolbar) findViewById(R.id.toolbarForAddScheduleActivity);
        Intent intent = getIntent();
        mAction = intent.getAction();
        if (mAction.equals(StringKeys.EDIT_TASK)) {
            mToolbar.setTitle(R.string.addScheduleToolbarTitleEditTask);
            mPosTask = intent.getIntExtra(StringKeys.TASK_POSITION, -1);
            mTitle.setText(intent.getStringExtra(StringKeys.TASK_TITLE));
            mComment.setText(intent.getStringExtra(StringKeys.TASK_COMMENT));
        } else
            mToolbar.setTitle(R.string.addScheduleToolbarTitleAddTask);
        setSupportActionBar(mToolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_shedule, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_exit:
                finish();
                break;
            case R.id.action_save:
                Intent intent = new Intent();
                if (mAction.equals(StringKeys.EDIT_TASK))
                    intent.putExtra(StringKeys.TASK_POSITION, mPosTask);
                intent.putExtra(StringKeys.TASK_TITLE, mTitle.getText().toString());
                intent.putExtra(StringKeys.TASK_COMMENT, mComment.getText().toString());
                setResult(RESULT_OK, intent);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}