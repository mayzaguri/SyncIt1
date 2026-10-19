
package com.example.syncit1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    public interface OnJoinClickListener {
        void onJoinClick(Map<String, Object> group);
    }

    private final OnJoinClickListener listener;
    private final List<Map<String, Object>> groups = new ArrayList<>();

    public GroupAdapter(OnJoinClickListener listener) {
        this.listener = listener;
    }

    public void setGroups(List<Map<String, Object>> newGroups) {
        groups.clear();
        groups.addAll(newGroups);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group, parent, false);

        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull GroupViewHolder holder,
            int position) {

        Map<String, Object> group = groups.get(position);

        String groupName = getValue(group, "groupName");
        String destination = getValue(group, "destination");
        String startDate = getValue(group, "startDate");
        String endDate = getValue(group, "endDate");
        String matchInfo = getValue(group, "matchInfo");

        holder.tvGroupName.setText(
                groupName.isEmpty() ? "טיול חדש ✈️" : groupName);

        holder.tvGroupDestination.setText(
                destination.isEmpty()
                        ? "יעד לא צוין"
                        : "📍 " + destination);

        String dates = "";

        if (!startDate.isEmpty()) {
            dates = startDate;
        }

        if (!endDate.isEmpty()) {
            dates += dates.isEmpty() ? endDate : " - " + endDate;
        }

        holder.tvGroupDates.setText(
                dates.isEmpty() ? "התאריכים טרם נקבעו" : "📅 " + dates);

        holder.tvMatchInfo.setText(
                matchInfo.isEmpty()
                        ? "מוזמנים להצטרף לטיול ✈️"
                        : matchInfo);

        holder.btnJoinGroup.setOnClickListener(
                v -> listener.onJoinClick(group));
    }

    private String getValue(Map<String, Object> group, String key) {
        Object value = group.get(key);

        if (value == null) {
            Object answers = group.get("answers");

            if (answers instanceof Map<?, ?>) {
                value = ((Map<?, ?>) answers).get(key);
            }
        }

        return value == null ? "" : value.toString().trim();
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {

        TextView tvGroupName;
        TextView tvGroupDestination;
        TextView tvGroupDates;
        TextView tvMatchInfo;
        Button btnJoinGroup;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);

            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvGroupDestination = itemView.findViewById(R.id.tvGroupDestination);
            tvGroupDates = itemView.findViewById(R.id.tvGroupDates);
            tvMatchInfo = itemView.findViewById(R.id.tvMatchInfo);
            btnJoinGroup = itemView.findViewById(R.id.btnJoinGroup);
        }
    }
}
