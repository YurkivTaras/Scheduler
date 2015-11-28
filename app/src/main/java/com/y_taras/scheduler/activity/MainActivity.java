package com.y_taras.scheduler.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.example.scheduler.R;

import java.util.ArrayList;

import adapter.AdapterForTaskList;
import other.ExtraName;
import other.Task;

public class MainActivity extends ActionBarActivity {
    private static final String Task = "name";
    private static final int RequestCodeAddTask = 1;
    private ArrayList<Task> mTasks;
    private AdapterForTaskList listAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI(savedInstanceState);
    }

    private void initUI(Bundle savedInstanceState) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //toolbar.setTitle("toolbar");sd
        setSupportActionBar(toolbar);

        Button addButton = (Button) findViewById(R.id.addTask);
        addButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddScheduleActivity.class);
                startActivityForResult(intent, RequestCodeAddTask);
            }
        });

        if (savedInstanceState == null) {
            mTasks = new ArrayList<Task>();
            for (int i = 0; i < 20; i++)
                mTasks.add(new Task("Title" + i, "Comment" + i));
        } else if (savedInstanceState.containsKey(Task))
            mTasks = (ArrayList) savedInstanceState.getSerializable(Task);

        ListView listView = (ListView) findViewById(R.id.listView);
        listAdapter = new AdapterForTaskList(this, mTasks);
        listView.setAdapter(listAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RequestCodeAddTask && resultCode == Activity.RESULT_OK) {
            String name = data.getStringExtra(ExtraName.TITLE_TASK);
            String comment = data.getStringExtra(ExtraName.Comment_TASK);
            mTasks.add(new Task(name, comment));
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(Task, mTasks);
        listAdapter.notifyDataSetChanged();
    }
}