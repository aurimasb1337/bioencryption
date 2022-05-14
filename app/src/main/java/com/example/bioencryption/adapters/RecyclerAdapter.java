package com.example.bioencryption.adapters;



import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bioencryption.R;
import com.example.bioencryption.models.FileModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    String[] docs = {"pdf", "csv", "xls", "xlsx"};
    String[] video = {"mp3", "mp4", "wav", "avi"};
    String[] images = {"png", "jpg", "gif", "jpeg"};
    public interface OnItemClickListener {
        void onItemClick(FileModel fileModel);
    }
    ArrayList<FileModel> data = new ArrayList<>();
    private final OnItemClickListener listener;
    public RecyclerAdapter(ArrayList<FileModel> data, OnItemClickListener listener) {
        this.data = data;
        this.listener = listener;
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //    data = sortByAge(data);
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.file_item, null));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter.ViewHolder holder, int position) {
    
        holder.title.setText(data.get(position).getName());
        holder.desc.setText(data.get(position).getTimestamp());
        holder.bind(data.get(position), listener);
        Log.d("Debug", data.get(position).toString());


      if(  Arrays.asList(docs).contains(data.get(position).getType()))
            holder.image.setImageResource(R.drawable.fileimg);

        if(  Arrays.asList(video).contains(data.get(position).getType()))
            holder.image.setImageResource(R.drawable.videoimg);

        if(  Arrays.asList(images).contains(data.get(position).getType()))
            holder.image.setImageResource(R.drawable.pictureimg);

            Log.d("java kurwa", data.get(position).getType() + Arrays.asList(video).contains(data.get(position).getType()));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, desc;
        ImageView image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            desc = itemView.findViewById(R.id.desc);
            image = itemView.findViewById(R.id.image);
        }

        public void bind(FileModel fileModel, OnItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClick(fileModel);
                }
            });
        }
    }


}