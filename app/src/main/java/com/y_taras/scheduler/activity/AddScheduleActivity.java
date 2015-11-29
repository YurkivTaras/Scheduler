package com.y_taras.scheduler.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.example.scheduler.R;

import other.StringConstForIntent;

public class AddScheduleActivity extends Activity {
    private EditText mTitle;
    private EditText mComment;
    private String mAction;
    private int mPosTask;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.add_schedule);
        initUI();
    }

    private void initUI() {
        mTitle = (EditText) findViewById(R.id.editTxtTitle);
        mComment = (EditText) findViewById(R.id.editTextComment);
        Button saveButton = (Button) findViewById(R.id.saveButton);
        Button exitButton = (Button) findViewById(R.id.exitButton);
        Intent intent = getIntent();
        mAction = intent.getAction();
        if (mAction.equals(StringConstForIntent.EDIT_TASK)) {
            mPosTask = intent.getIntExtra(StringConstForIntent.TASK_POSITION, -1);
            mTitle.setText(intent.getStringExtra(StringConstForIntent.TASK_TITLE));
            mComment.setText(intent.getStringExtra(StringConstForIntent.TASK_COMMENT));
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                if (mAction.equals(StringConstForIntent.EDIT_TASK))
                    intent.putExtra(StringConstForIntent.TASK_POSITION, mPosTask);
                intent.putExtra(StringConstForIntent.TASK_TITLE, mTitle.getText().toString());
                intent.putExtra(StringConstForIntent.TASK_COMMENT, mComment.getText().toString());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        
        exitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
    }
}