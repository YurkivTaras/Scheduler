package com.y_taras.scheduler.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.scheduler.R;

import java.util.ArrayList;
import java.util.List;

import other.StringKeys;

public class AddScheduleActivity extends AppCompatActivity {
    private TextInputLayout inputLayoutTitle, inputLayoutComment;
    private EditText mTitle;
    private EditText mComment;
    private String mAction;
    private int mPosTask;

    private ImageButton mBtn_titleVoiceInput;
    private ImageButton mBtn_CommentVoiceInput;
    private boolean mIfNotAvailableVoiceInput;
    private static final int REQUEST_CODE_FOR_TITLE = 103;
    private static final int REQUEST_CODE_FOR_COMMENT = 104;
    private Toast mToast;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_schedule);
        initUI();
    }

    private void initUI() {
        inputLayoutTitle = (TextInputLayout) findViewById(R.id.inputLayoutTitle);
        inputLayoutComment = (TextInputLayout) findViewById(R.id.inputLayoutComment);
        mTitle = (EditText) findViewById(R.id.editTxtTitle);
        mComment = (EditText) findViewById(R.id.editTextComment);

        mBtn_titleVoiceInput = (ImageButton) findViewById(R.id.voice_input_title_btn);
        mBtn_CommentVoiceInput = (ImageButton) findViewById(R.id.voice_input_comment_btn);

        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() == 0) {
            mIfNotAvailableVoiceInput = true;
            mBtn_titleVoiceInput.setVisibility(View.INVISIBLE);
            mBtn_CommentVoiceInput.setVisibility(View.INVISIBLE);
        }
        mBtn_titleVoiceInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceRecognitionActivity(REQUEST_CODE_FOR_TITLE);
            }
        });
        mBtn_CommentVoiceInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceRecognitionActivity(REQUEST_CODE_FOR_COMMENT);
            }
        });
        mTitle.addTextChangedListener(new MyTextWatcher(mTitle));
        mComment.addTextChangedListener(new MyTextWatcher(mComment));

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbarForAddScheduleActivity);
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

    private void startVoiceRecognitionActivity(int requestCode) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak the word");
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            ArrayList<String> textMatchList = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            switch (requestCode) {
                case REQUEST_CODE_FOR_TITLE:
                    if (!textMatchList.isEmpty()) {
                        String Query = textMatchList.get(0);
                        mTitle.setText(Query);
                    }
                    break;
                case REQUEST_CODE_FOR_COMMENT:
                    if (!textMatchList.isEmpty()) {
                        String Query = textMatchList.get(0);
                        mComment.setText(Query);
                    }
                    break;
            }
        } else if (resultCode == RecognizerIntent.RESULT_NETWORK_ERROR) {
            if (mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT);
            mToast.show();
        } else if (resultCode == RecognizerIntent.RESULT_NO_MATCH) {
            if (mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(this, "No Match", Toast.LENGTH_SHORT);
            mToast.show();
        } else if (resultCode == RecognizerIntent.RESULT_SERVER_ERROR) {
            if (mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(this, "Server Error", Toast.LENGTH_SHORT);
            mToast.show();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
                if (validateTitle() && validateComment()) {
                    Intent intent = new Intent();
                    if (mAction.equals(StringKeys.EDIT_TASK))
                        intent.putExtra(StringKeys.TASK_POSITION, mPosTask);
                    intent.putExtra(StringKeys.TASK_TITLE, mTitle.getText().toString());
                    intent.putExtra(StringKeys.TASK_COMMENT, mComment.getText().toString());
                    setResult(RESULT_OK, intent);
                    finish();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean validateTitle() {
        int length = mTitle.getText().toString().trim().length();
        if (!mIfNotAvailableVoiceInput)
            if (length != 0)
                mBtn_titleVoiceInput.setVisibility(View.INVISIBLE);
            else
                mBtn_titleVoiceInput.setVisibility(View.VISIBLE);
        if (length < 5) {
            inputLayoutTitle.setError(getString(R.string.errTitle2));
            requestFocus(mTitle);
            return false;
        } else
            inputLayoutTitle.setErrorEnabled(false);
        return true;
    }

    private boolean validateComment() {
        int length = mTitle.getText().toString().trim().length();
        if (!mIfNotAvailableVoiceInput)
            if (length != 0)
                mBtn_CommentVoiceInput.setVisibility(View.INVISIBLE);
            else
                mBtn_CommentVoiceInput.setVisibility(View.VISIBLE);
        inputLayoutComment.setErrorEnabled(false);
        return true;
    }

    private void requestFocus(EditText editText) {
        if (editText.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private class MyTextWatcher implements TextWatcher {
        private View view;

        private MyTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.editTxtTitle:
                    validateTitle();

                    break;
                case R.id.editTextComment:
                    validateComment();
                    mBtn_CommentVoiceInput.setVisibility(View.INVISIBLE);
                    break;
            }
        }
    }
}
