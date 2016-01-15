package com.y_taras.scheduler.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.y_taras.scheduler.R;

public class CustomSpinnerAdapter extends ArrayAdapter<String> {

    private Context mContext;
    private String mMenuItems[];
    private LayoutInflater mInflater;

    public CustomSpinnerAdapter(Context context, String[] objects) {
        super(context, R.layout.spinner_row, objects);
        mContext = context;
        mMenuItems = objects;
        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {
        View row = mInflater.inflate(R.layout.spinner_row, parent, false);
        TextView tvCategory = (TextView) row.findViewById(R.id.tvCategory);
        tvCategory.setText(mMenuItems[position]);
        return row;
    }
}