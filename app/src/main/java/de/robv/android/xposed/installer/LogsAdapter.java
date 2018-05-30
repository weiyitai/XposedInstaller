package de.robv.android.xposed.installer;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
/**
 * @author wWX407408
 * @Created at 2017/11/21  16:29
 * @des
 */

public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.LogHolder> {

    private List<String> mList = new ArrayList<>();

    @Override
    public LogHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
        return new LogHolder(view);
    }

    @Override
    public void onBindViewHolder(LogHolder holder, int position) {
        holder.mTvLog.setText(mList.get(position));
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public List<String> getData() {
        return mList;
    }

    public void setData(List<String> list) {
        mList = list;
        notifyDataSetChanged();
    }


    static class LogHolder extends RecyclerView.ViewHolder {

        private final TextView mTvLog;

        public LogHolder(View itemView) {
            super(itemView);
            mTvLog = (TextView) itemView.findViewById(R.id.tv_log);
        }
    }
}
