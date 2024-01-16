package com.ivanenko.madmusicplayer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Objects;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener{

    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int PERMISSION_REQUEST_READ_MEDIA_AUDIO = 2;

    private MediaPlayer mediaPlayer;
    private TextView artistTextView, titleTextView, albumTextView;
    Button randomSongButton;
    private ImageButton playPauseButton;
    String songPath;


    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);

        Toolbar toolbar = findViewById(R.id.appToolbar);
        setSupportActionBar(toolbar);

        artistTextView = findViewById(R.id.artistTextView);
        titleTextView = findViewById(R.id.titleTextView);
        albumTextView = findViewById(R.id.albumTextView);

        playPauseButton = findViewById(R.id.playPauseButton);
        playPauseButton.setOnClickListener(v -> togglePlayPause());

        randomSongButton = findViewById(R.id.selectRandomSongButton);
        randomSongButton.setOnClickListener(v -> selectRandomSong());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                logSongs();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                        PERMISSION_REQUEST_READ_MEDIA_AUDIO);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                logSongs();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }

    }

    @Override
    protected void onPause(){
        super.onPause();
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String artist = artistTextView.getText().toString();
        String title = titleTextView.getText().toString();
        String album = albumTextView.getText().toString();

        editor.putString("Artist", artist);
        editor.putString("Title", title);
        editor.putString("Album", album);
        editor.putString("SongPath", songPath);
        editor.apply();
    }

    @Override
    protected void onResume(){
        super.onResume();

        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        final String defaultArtist = getText(R.string.defaultArtist).toString();
        final String defaultTitle = getText(R.string.defaultTitle).toString();
        final String defaultAlbum = getText(R.string.defaultAlbum).toString();

        String savedArtist = prefs.getString("Artist", defaultArtist);
        String savedTitle = prefs.getString("Title", defaultTitle);
        String savedAlbum = prefs.getString("Album", defaultAlbum);
        songPath = prefs.getString("SongPath", null);

        artistTextView.setText(savedArtist);
        titleTextView.setText(savedTitle);
        albumTextView.setText(savedAlbum);
        mediaPlayer.reset();

        if (songPath != null) {
            try {
                mediaPlayer.setDataSource(songPath);
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this,
                        "An error occurred while searching for this song",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (requestCode == PERMISSION_REQUEST_READ_MEDIA_AUDIO) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    logSongs();
                } else {
                    Log.e("PermissionManager", "Read external storage permission denied");
                }
            }
        } else {
            if (requestCode == PERMISSION_REQUEST_READ_EXTERNAL_STORAGE) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    logSongs();
                } else {
                    Log.e("PermissionManager", "Read external storage permission denied");
                }
            }
        }
    }

    private void togglePlayPause(){
        if (!mediaPlayer.isPlaying()) {
            if (songPath != null) {
                playPauseButton.setImageResource(R.drawable.pause_icon);
                mediaPlayer.start();
            } else
                Toast.makeText(this,
                        "You should first select a random song",
                        Toast.LENGTH_SHORT).show();

        } else {
            playPauseButton.setImageResource(R.drawable.play_icon);
            mediaPlayer.pause();
        }
    }

    @SuppressLint("SetTextI18n")
    private void selectRandomSong(){
        ContentResolver cr = getContentResolver();
        String[] projection = {
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA
        };

        Cursor cur = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
                null, null, null);

        if (cur != null){
            int songs = cur.getCount();
            Random random = new Random();
            int songPos = random.nextInt(songs);
            cur.moveToPosition(songPos);
            String artist = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            String album = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            String title = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            String songPath = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));

            if (!Objects.equals(artist, "<unknown>"))
                artistTextView.setText(artist);
            else artistTextView.setText("Unknown artist");

            titleTextView.setText(title);
            albumTextView.setText("Album: " + album);
            this.songPath = songPath;

            playPauseButton.setEnabled(false);
            randomSongButton.setEnabled(false);

            try {
                if (mediaPlayer.isPlaying()) {
                    togglePlayPause();
                }
                mediaPlayer.reset();
                mediaPlayer.setDataSource(songPath);
                mediaPlayer.prepareAsync();
            } catch (IOException e){
                e.printStackTrace();
                Toast.makeText(this,
                        "An error occurred while searching for this song",
                        Toast.LENGTH_SHORT).show();
            }

            cur.close();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        playPauseButton.setEnabled(true);
        randomSongButton.setEnabled(true);
        mediaPlayer.setLooping(true);
    }

    public void logSongs(){
        ContentResolver cr = getContentResolver();
        String[] projection = {
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION
        };

        Cursor cur = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
                null, null, null);

        if (cur != null) {
            while (cur.moveToNext()) {
                String artist = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                String album = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                String title = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                long duration = cur.getLong(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));

                Log.d("SongLogging", "Artist: " + artist + ", Album: " + album +
                        ", Title: " + title + ", Duration: " + duration);
            }
            cur.close();
        }
    }
}