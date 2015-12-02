package com.y_taras.scheduler.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.example.scheduler.R;

import java.util.ArrayList;
import java.util.Date;

import adapter.AdapterForTaskList;
import other.StringConstForIntent;
import other.Task;
import utils.TasksLoader;

public class MainActivity extends ActionBarActivity {
    private static final String ArrayOfTasks = "tasks";
    private static final int RequestCodeAddTask = 1;
    private static final int RequestCodeEditTask = 2;
    private ArrayList<Task> mTasks;
    private AdapterForTaskList mListAdapter;

    private TasksLoader tasksReader;
    private TasksLoader tasksSaver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initUI(Bundle savedInstanceState) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button addButton = (Button) findViewById(R.id.addTask);
        addButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddScheduleActivity.class);
                intent.setAction(StringConstForIntent.ADD_TASK);
                startActivityForResult(intent, RequestCodeAddTask);
            }
        });

        if (savedInstanceState == null)
            mTasks = new ArrayList<Task>();
        else if (savedInstanceState.containsKey(ArrayOfTasks))
            mTasks = savedInstanceState.getParcelableArrayList(ArrayOfTasks);

        final ListView listView = (ListView) findViewById(R.id.listView);
        listView.setEmptyView(findViewById(R.id.emptyList));

        mListAdapter = new AdapterForTaskList(this, mTasks);
        listView.setAdapter(mListAdapter);

        tasksReader = TasksLoader.createReader(mTasks, this, mListAdapter);
        tasksSaver = TasksLoader.createSaver(mTasks, this);
        //спроба загрузити таски збережені в SharedPreference
        if (savedInstanceState == null)
            tasksReader.execute();

        //обробка одиничного кліку по елементу із списку завдань
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                Task clickTask = mTasks.get(position);
                if (clickTask.getDateStart() == null) {
                    clickTask.setDateStart(new Date());
                    mListAdapter.notifyDataSetChanged();
                    //Збереження змін в списку завдань
                    tasksSaver.execute();
                } else if (clickTask.getDateEnd() == null) {
                    clickTask.setDateEnd(new Date());
                    clickTask.calcTimeSpent();
                    mListAdapter.notifyDataSetChanged();
                    //Збереження змін в списку завдань
                    tasksSaver.execute();
                }
            }
        });
        //обробка longClick по елементу із списку завдань
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Task clickTask = mTasks.get(position);
                Intent intent = new Intent(MainActivity.this, AddScheduleActivity.class);

                intent.setAction(StringConstForIntent.EDIT_TASK);

                intent.putExtra(StringConstForIntent.TASK_POSITION, position);
                intent.putExtra(StringConstForIntent.TASK_TITLE, clickTask.getTitle());
                intent.putExtra(StringConstForIntent.TASK_COMMENT, clickTask.getComment());
                startActivityForResult(intent, RequestCodeEditTask);
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String name = data.getStringExtra(StringConstForIntent.TASK_TITLE);
            String comment = data.getStringExtra(StringConstForIntent.TASK_COMMENT);
            switch (requestCode) {
                case RequestCodeAddTask:
                    mTasks.add(new Task(name, comment));
                    break;
                case RequestCodeEditTask:
                    Task editTask = mTasks.get(data.getIntExtra(StringConstForIntent.TASK_POSITION, -1));
                    editTask.setTitle(name);
                    editTask.setComment(comment);
                    break;
            }
            mListAdapter.notifyDataSetChanged();
            //Збереження змін в списку завдань
            tasksSaver.execute();
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(ArrayOfTasks, mTasks);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //генерація 30 тасків
        if (id == R.id.action_generate) {
            for (int i = 0; i < 30; i++)
                mTasks.add(new Task("Title" + (i + 1), "Comment" + (i + 1)));
            tasksSaver.execute();
            mListAdapter.notifyDataSetChanged();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}