package com.y_taras.scheduler.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.y_taras.scheduler.R;

import java.util.ArrayList;
import java.util.HashMap;

import com.y_taras.scheduler.other.Task;

public class CustomExpandableListAdapter extends BaseExpandableListAdapter {

    private ArrayList<ArrayList<Task>> mGroups;
    private ArrayList<String> mGroupsName;
    private Context mContext;

    public CustomExpandableListAdapter(Context context, ArrayList<ArrayList<Task>> groups, ArrayList<String> groupsName) {
        mContext = context;
        mGroupsName = groupsName;
        mGroups = groups;
    }

    @Override
    public int getGroupCount() {
        return mGroups.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mGroups.get(groupPosition).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mGroups.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mGroups.get(groupPosition).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.group_view_exp_list, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.addView(convertView.findViewById(R.id.textGroup));
            convertView.setTag(holder);
        }
        ViewHolder holder = (ViewHolder) convertView.getTag();
        TextView textGroup = (TextView) holder.getView(R.id.textGroup);
        textGroup.setText(mGroupsName.get(groupPosition));
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.child_view_exp_list, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.addView(convertView.findViewById(R.id.expTxtTitle));
            holder.addView(convertView.findViewById(R.id.expTxtComment));
            holder.addView(convertView.findViewById(R.id.expTxtRuntime));
            holder.addView(convertView.findViewById(R.id.expListAvatar));
            convertView.setTag(holder);
        }
        Task task = mGroups.get(groupPosition).get(childPosition);

        ViewHolder holder = (ViewHolder) convertView.getTag();
        TextView title = (TextView) holder.getView(R.id.expTxtTitle);
        TextView comment = (TextView) holder.getView(R.id.expTxtComment);
        TextView runtime = (TextView) holder.getView(R.id.expTxtRuntime);
        ImageView avatar = (ImageView) holder.getView(R.id.expListAvatar);
        //загрузка іконки для завдання
        if (!task.getAvatarUri().equals(Task.DEFAULT_AVATAR_URI)) {
            Glide.with(mContext)
                    .load(task.getAvatarUri())
                    .placeholder(R.drawable.pause_btn_normal)
                    .crossFade()
                    .into(avatar);
        } else
            avatar.setImageResource(R.drawable.default_avatar);
        title.setText(task.getTitle());
        comment.setText(task.getComment());
        long spentTime = task.getRuntime();
        int spentHours = (int) (spentTime / (1000 * 60 * 60));
        int spentMinute = (int) (spentTime - spentHours * 1000 * 60 * 60) / 60000;
        int spentSeconds = (int) (spentTime - spentHours * 1000 * 60 * 60 - spentMinute * 60000) / 1000;
        runtime.setText(String.format("Час виконання: %02d:%02d:%02d", spentHours, spentMinute, spentSeconds));
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public class ViewHolder {
        private HashMap<Integer, View> mStoredViews;

        public ViewHolder() {
            mStoredViews = new HashMap<>();
        }

        public ViewHolder addView(View view) {
            int id = view.getId();
            mStoredViews.put(id, view);
            return this;
        }

        public View getView(int id) {
            return mStoredViews.get(id);
        }
    }
}