package com.y_taras.scheduler.activity;

import com.example.scheduler.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import android.view.View;

import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import other.ExtraName;


public class AddScheduleActivity extends Activity {
    private EditText mTitle;
    private EditText mComment;

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
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(ExtraName.TITLE_TASK, mTitle.getText().toString());
                intent.putExtra(ExtraName.Comment_TASK, mComment.getText().toString());
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