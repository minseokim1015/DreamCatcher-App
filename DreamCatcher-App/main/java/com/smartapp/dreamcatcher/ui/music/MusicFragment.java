package com.smartapp.dreamcatcher.ui.music;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smartapp.dreamcatcher.MusicService;
import com.smartapp.dreamcatcher.R;
import com.smartapp.dreamcatcher.data.Music;
import com.smartapp.dreamcatcher.data.MusicAdapter;

import java.util.ArrayList;

public class MusicFragment extends Fragment {
    private ArrayList<Music> musicList;

    private void startMusicService(Music music) {
        Toast.makeText(getContext(), String.format("Now playing %s.", music.title), Toast.LENGTH_SHORT).show();
        Context context = getContext();
        if (context == null) {
            return;
        }
        context = context.getApplicationContext();
        Intent intent = new Intent(context, MusicService.class); //Create new intent and
        intent.putExtra("id", music.musicID);
        intent.putExtra("title", music.title);
        context.stopService(intent);
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private static ArrayList<Music> getMusicList() {
        ArrayList<Music> ret = new ArrayList<>();
        for (int i=0; i<10; i++) {
            ret.add(new Music("Pitter Patter", "Natural pink noise", R.raw.rain));
            ret.add(new Music("Ocean Waves", "Drift into deep sleep", R.raw.music));
            ret.add(new Music("DreamCatcher OST", "Have a good night, sweet dreams", R.raw.music));
        }
        return ret;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_music, container, false);

        RecyclerView rv = root.findViewById(R.id.music_list_music);
        rv.setLayoutManager(new LinearLayoutManager(root.getContext()));
        musicList = getMusicList();
        MusicAdapter adapter = new MusicAdapter(musicList);
        adapter.setOnItemClickListener(new MusicAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int pos, Music item) {
                startMusicService(item);
            }
        });
        rv.setAdapter(adapter);

        ((Button)root.findViewById(R.id.music_list_stop_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getContext();
                if (context == null) {
                    return;
                }
                context = context.getApplicationContext();
                Intent intent = new Intent(context, MusicService.class);
                context.stopService(intent);
            }
        });

        return root;
    }
}