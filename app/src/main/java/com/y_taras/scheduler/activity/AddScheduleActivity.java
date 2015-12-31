package com.y_taras.scheduler.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.speech.RecognizerIntent;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.scheduler.R;
import com.soundcloud.android.crop.Crop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.drakeet.materialdialog.MaterialDialog;
import other.StringKeys;
import other.Task;
import utils.ImageLoader;

public class AddScheduleActivity extends AppCompatActivity {

    private TextInputLayout mInputLayoutTitle, mInputLayoutComment;
    private EditText mTitle, mComment, mMaxRuntime;
    private ImageView mAvatar;
    private CheckBox mTypeOfTask;
    private Bitmap mBtmAvatar;
    private Bitmap mEditBtmAvatar;
    private String mAction;
    private int mPosTask;
    private String mEditAvatarUri;
    private Uri cropImgUri;
    private MaterialDialog mAlertForImgPick;
    private ImageButton mBtn_titleVoiceInput, mBtn_CommentVoiceInput;
    private boolean mIfNotAvailableVoiceInput;
    private static final int requestCodeForTitle = 1;
    private static final int requestCodeForComment = 2;
    private static final int requestCodeForGallery = 3;
    private static final int requestCodeForCamera = 4;
    private Toast mToast;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_schedule);
        initUI(savedInstanceState);
    }

    private void initUI(Bundle savedInstanceState) {
        mInputLayoutTitle = (TextInputLayout) findViewById(R.id.inputLayoutTitle);
        mInputLayoutComment = (TextInputLayout) findViewById(R.id.inputLayoutComment);

        mTitle = (EditText) findViewById(R.id.editTxtTitle);
        mComment = (EditText) findViewById(R.id.editTxtComment);
        mMaxRuntime = (EditText) findViewById(R.id.editTxtMaxRuntime);
        mAvatar = (ImageView) findViewById(R.id.imageViewAvatar);
        mTypeOfTask = (CheckBox) findViewById(R.id.typeOfTaskCheckBox);

        mAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showAlertDialogForPickAvatar();

            }
        });
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(StringKeys.BITMAP_AVATAR)) {
                mBtmAvatar = savedInstanceState.getParcelable(StringKeys.BITMAP_AVATAR);
                mAvatar.setImageBitmap(mBtmAvatar);
            } else if (savedInstanceState.containsKey(StringKeys.EDIT_BITMAP_AVATAR)) {
                mEditBtmAvatar = savedInstanceState.getParcelable(StringKeys.EDIT_BITMAP_AVATAR);
                mAvatar.setImageBitmap(mEditBtmAvatar);
            }
        }
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
                startVoiceRecognitionActivity(requestCodeForTitle);
            }
        });
        mBtn_CommentVoiceInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceRecognitionActivity(requestCodeForComment);
            }
        });
        mTitle.addTextChangedListener(new MyTextWatcher(mTitle));
        mComment.addTextChangedListener(new MyTextWatcher(mComment));

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbarForAddScheduleActivity);
        Intent intent = getIntent();
        mMaxRuntime.setText(String.format("%d", intent.getIntExtra(StringKeys.MAX_RUNTIME_FOR_TASK, 60)));
        mAction = intent.getAction();
        if (mAction.equals(StringKeys.EDIT_TASK)) {
            mToolbar.setTitle(R.string.addScheduleToolbarTitleEditTask);
            mPosTask = intent.getIntExtra(StringKeys.TASK_POSITION, -1);
            mTitle.setText(intent.getStringExtra(StringKeys.TASK_TITLE));
            mComment.setText(intent.getStringExtra(StringKeys.TASK_COMMENT));
            mEditAvatarUri = intent.getStringExtra(StringKeys.BITMAP_AVATAR);
            mTypeOfTask.setChecked(intent.getBooleanExtra(StringKeys.TYPE_OF_TASK, false));
            mTypeOfTask.setEnabled(false);
            if (savedInstanceState == null && !mEditAvatarUri.equals(Task.DEFAULT_AVATAR_URI)) {
                mEditBtmAvatar = ImageLoader.loadImage(mEditAvatarUri);
                mAvatar.setImageBitmap(mEditBtmAvatar);
            }
        } else
            mToolbar.setTitle(R.string.addScheduleToolbarTitleAddTask);
        setSupportActionBar(mToolbar);
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mBtmAvatar != null)
            outState.putParcelable(StringKeys.BITMAP_AVATAR, mBtmAvatar);
        else if (mEditBtmAvatar != null)
            outState.putParcelable(StringKeys.EDIT_BITMAP_AVATAR, mEditBtmAvatar);

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
            ArrayList<String> textMatchList;
            switch (requestCode) {
                case requestCodeForTitle:
                    textMatchList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (!textMatchList.isEmpty()) {
                        String Query = textMatchList.get(0);
                        mTitle.setText(Query);
                    }
                    break;
                case requestCodeForComment:
                    textMatchList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (!textMatchList.isEmpty()) {
                        String Query = textMatchList.get(0);
                        mComment.setText(Query);
                    }
                    break;
                case requestCodeForCamera:
                case requestCodeForGallery:
                    Uri selectedImage = data.getData();
                    cropImgUri = Uri.fromFile(new File(getCacheDir(), "cropped"));
                    Crop.of(selectedImage, cropImgUri).asSquare().start(this);
                    break;
                case Crop.REQUEST_CROP:
                    mBtmAvatar = null;
                    try {
                        cropImgUri = Uri.fromFile(new File(getCacheDir(), "cropped"));
                        mBtmAvatar = Media.getBitmap(getContentResolver(), cropImgUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Resources resources = getResources();
                    //конвертуєм 100dp в px
                    int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, resources.getDisplayMetrics());
                    mBtmAvatar = Bitmap.createScaledBitmap(mBtmAvatar, px, px, true);
                    //повернення іконки
                   /* Matrix matrix = new Matrix();
                    matrix.postRotate(270);
                    mBtmAvatar = Bitmap.createBitmap(mBtmAvatar, 0, 0, mBtmAvatar.getWidth(), mBtmAvatar.getHeight(), matrix, true);*/
                    mAvatar.setImageBitmap(mBtmAvatar);
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
                    if (mBtmAvatar != null) {
                        //якщо було змінено іконку завдання - видаляєм попередню з памяті
                        if (mEditAvatarUri != null && !mEditAvatarUri.equals(Task.DEFAULT_AVATAR_URI))
                            ImageLoader.delete(mEditAvatarUri);
                        //зберігаєм нову іконку
                        String avatarUri = ImageLoader.saveImageFile(mBtmAvatar, this);
                        intent.putExtra(StringKeys.BITMAP_AVATAR, avatarUri);
                    }
                    String maxRuntime = mMaxRuntime.getText().toString().trim();
                    intent.putExtra(StringKeys.MAX_RUNTIME_FOR_TASK, maxRuntime.length() == 0 ? 0 : Integer.parseInt(maxRuntime));
                    intent.putExtra(StringKeys.TASK_TITLE, mTitle.getText().toString());
                    intent.putExtra(StringKeys.TASK_COMMENT, mComment.getText().toString());
                    intent.putExtra(StringKeys.TYPE_OF_TASK, mTypeOfTask.isChecked());
                    setResult(RESULT_OK, intent);
                    finish();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //показ діалога для встановлення способу завантаження іконки для завдання
    private void showAlertDialogForPickAvatar() {
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        arrayAdapter.add(getString(R.string.chooseFromGallery));
        arrayAdapter.add(getString(R.string.chooseFromCamera));
        ListView listView = new ListView(this);
        listView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        listView.setDividerHeight(1);
        listView.setAdapter(arrayAdapter);

        final MaterialDialog alert = new MaterialDialog(this).
                setContentView(listView);
        alert.setNegativeButton(getString(R.string.cancle), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alert.dismiss();
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position,
                                    long arg3) {
                alert.dismiss();
                if (position == 0) {
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, requestCodeForGallery);
                } else {
                    Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(takePicture, requestCodeForCamera);
                }
            }
        });
        alert.show();
    }

    private boolean validateTitle() {
        int length = mTitle.getText().toString().trim().length();
        if (!mIfNotAvailableVoiceInput)
            if (length != 0)
                mBtn_titleVoiceInput.setVisibility(View.INVISIBLE);
            else
                mBtn_titleVoiceInput.setVisibility(View.VISIBLE);
        if (length < 5) {
            mInputLayoutTitle.setError(getString(R.string.errTitle2));
            requestFocus(mTitle);
            return false;
        } else
            mInputLayoutTitle.setErrorEnabled(false);
        return true;
    }

    private boolean validateComment() {
        int length = mComment.getText().toString().trim().length();
        if (!mIfNotAvailableVoiceInput)
            if (length != 0)
                mBtn_CommentVoiceInput.setVisibility(View.INVISIBLE);
            else
                mBtn_CommentVoiceInput.setVisibility(View.VISIBLE);
        mInputLayoutComment.setErrorEnabled(false);
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
                case R.id.editTxtComment:
                    validateComment();
                    break;
            }
        }
    }
}
