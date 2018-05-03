package com.abhishek.oneminvideo;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.VideoView;

import java.util.ArrayList;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity implements VideoTimelineView.VideoTimelineViewDelegate {
    private static final String TAG = "MainActivity";

    private VideoTimelineView vd1, vd2, vd3;
    private VideoView videoView;
    private HorizontalScrollView horizontalScrollView;
    private int PAUSED = 0, PLAYING = 1;
    boolean started = false;
    private Long videoLength;

    int size = 0;
    int curr_vid = 0;
    Handler vidHandler = new Handler();
    Runnable mRunnable;

    private float prog;
    private int currentDuration;

    ArrayList<VideoTimelineView> videos = new ArrayList<>();

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

    }

    /**
     * Manages video playback
     *
     * @param stopExistingPlaying bool to stop complete playback
     * @throws Exception for any error in playing , must update logs to developer to improve app
     */
    public void startPlaying(Boolean stopExistingPlaying) {
        if (stopExistingPlaying) {
            videoView.stopPlayback();
        }
        if (size == 0) {
            videoView.stopPlayback();
            return;
        }
        if (curr_vid == size) {
            return;
        }
        videoView.setVideoPath(videos.get(curr_vid).path);
        videoView.seekTo((int) videos.get(curr_vid).start_duration * 1000);
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                curr_vid++;
                startPlaying(false);
            }
        });
        mRunnable = new Runnable() {
            public void run() {
                int currentPostion = videoView.getCurrentPosition();
                if (currentPostion >= 30 * 1000 || currentPostion == videoView.getDuration()) {
                    // Play next video
                }
                if ( (currentPostion >= videos.get(curr_vid).end_duration * 1000) && (currentPostion != videos.get(curr_vid).duration * 1000) ) {
                    curr_vid++;
                    MainActivity.this.startPlaying(false);
                }
                vidHandler.postDelayed(this, 250);
            }
        };
        vidHandler.post(mRunnable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        videoView = findViewById(R.id.vv);
        horizontalScrollView = findViewById(R.id.sv);

        vd1 = findViewById(R.id.vd1);
        vd2 = findViewById(R.id.vd2);
        vd3 = findViewById(R.id.vd3);

        videos.add(vd1);
        videos.add(vd2);
        videos.add(vd3);


        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                final boolean running = true;
                final int duration = videoView.getDuration();

                new Thread(new Runnable() {
                    public void run() {
                        do {
                            vd1.post(new Runnable() {
                                public void run() {
                                    int time = (duration - videoView.getCurrentPosition()) / 1000;
                                    if (videoView.isPlaying()) {
                                        vd1.setTranslationX(time * 10);
                                    }
                                    Log.d(TAG, "run: " + time);
                                }
                            });
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (!running) break;
                        }
                        while (videoView.getCurrentPosition() < duration);
                    }
                }).start();
            }
        });


        vd1.setDelegate(this);
        vd2.setDelegate(this);
        vd3.setDelegate(this);
        pickVideo();


    }


    public void pickVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 1);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            //do something
            String path = getPath(data.getData());
            Log.d("PATH", path);
            try {
                vd1.setVideoPath(path);
                vd2.setVideoPath(path);
                vd3.setVideoPath(path);


                videoView.setVideoPath(path);
                videoView.setMediaController(new MediaController(this));
                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(path);
                String duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                videoLength = Long.parseLong(duration);
                Log.d(TAG, "onActivityResult: " + duration);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            //do something else
        }
    }

    public String getPath(Uri uri) {
        String[] projection = {MediaStore.Video.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            // HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
            // THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else
            return null;
    }


    @Override
    public void onLeftProgressChanged(float progress) {

        Log.d(TAG, "onLeftProgressChanged: progress  " + progress);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) vd1.getLayoutParams();

//            if (prog < progress) {
//                Log.d(TAG, "onLeftProgressChanged: before  " + vd1.getWidth());
//                params.width = vd1.getWidth() - ((int) (progress * vd1.getWidth()));
//                Log.d(TAG, "onLeftProgressChanged: after   " + params.width);
//                vd1.requestLayout();
//                prog = progress;
//
//        }

//        else {
//            ViewGroup.LayoutParams params = vd1.getLayoutParams();
//            Log.d(TAG, "onLeftProgressChanged: before  " + params.width);
//            params.width = params.width+((int) (progress * 600));
//            Log.d(TAG, "onLeftProgressChanged: after   " + params.width);
//            vd1.requestLayout();
//            prog = progress;
//        }
    }

    @Override
    public void onRightProgressChanged(float progress) {

    }

    @Override
    public void didStartDragging() {

    }

    @Override
    public void didStopDragging() {

    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
