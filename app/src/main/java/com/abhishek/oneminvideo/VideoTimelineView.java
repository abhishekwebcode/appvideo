package com.abhishek.oneminvideo;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Xfermode;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;


import java.util.ArrayList;

@TargetApi(10)
public class VideoTimelineView extends View {

    private static final String TAG = "VideoTimelineView";
    public Float orignalWidth;

    public int order;
    public float duration;
    public float start_handle;
    public float end_handle;
    public float current_duration;
    public float start_duration;
    public float end_duration;
    public String path;


    private long videoLength;
    private float progressLeft;
    private float progressRight = 1;
    private Paint paint;
    private Paint paint2;
    private boolean pressedLeft;
    private boolean pressedRight;
    private float pressDx;
    private static MediaMetadataRetriever mediaMetadataRetriever;
    private VideoTimelineViewDelegate delegate;
    private ArrayList<Bitmap> frames = new ArrayList<>();
    private AsyncTask<Integer, Integer, Bitmap> currentTask;
    private static final Object sync = new Object();
    private long frameTimeOffset;
    private int frameWidth;
    private int frameHeight;
    private int framesToLoad;
    private float maxProgressDiff = 1.0f;
    private float minProgressDiff = 0.0f;
    private boolean isRoundFrames;
    private Rect rect1;
    private Rect rect2;

    public VideoTimelineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public interface VideoTimelineViewDelegate {
        void onLeftProgressChanged(float progress);

        void onRightProgressChanged(float progress);

        void didStartDragging();

        void didStopDragging();

    }

    public VideoTimelineView(Context context) {
        super(context);
        init();
    }

    public void init() {

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xffffffff);
        paint2 = new Paint();
        paint2.setColor(0x7f000000);
    }

    public VideoTimelineView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }


    public float getLeftProgress() {
        return progressLeft;
    }

    public float getRightProgress() {
        return progressRight;
    }

    public void setMinProgressDiff(float value) {
        minProgressDiff = value;
    }

    public void setMaxProgressDiff(float value) {
        maxProgressDiff = value;
        if (progressRight - progressLeft > maxProgressDiff) {
            progressRight = progressLeft + maxProgressDiff;
            invalidate();
        }
    }

    public void setRoundFrames(boolean value) {
        isRoundFrames = value;
        if (isRoundFrames) {
            rect1 = new Rect(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14 + 28), AndroidUtilities.dp(14 + 28));
            rect2 = new Rect();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) {
            return (1==0);
        }
        Log.w("Sf",String.valueOf(2));
        float x = event.getX();
        float y = event.getY();

        int width = getMeasuredWidth() - AndroidUtilities.dp(32);
        int startX = (int) (width * progressLeft) + AndroidUtilities.dp(16);
        int endX = (int) (width * progressRight) + AndroidUtilities.dp(16);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
            if (mediaMetadataRetriever == null) {
                return false;
            }
            int additionWidth = AndroidUtilities.dp(12);
            if (startX - additionWidth <= x && x <= startX + additionWidth && y >= 0 && y <= getMeasuredHeight()) {
                if (delegate != null) {
                    delegate.didStartDragging();
                }
                pressedLeft = true;
                pressDx = (int) (x - startX);
                invalidate();
                return true;
            } else if (endX - additionWidth <= x && x <= endX + additionWidth && y >= 0 && y <= getMeasuredHeight()) {
                if (delegate != null) {
                    delegate.didStartDragging();
                }
                pressedRight = true;
                pressDx = (int) (x - endX);
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (pressedLeft) {
                if (delegate != null) {
                    delegate.didStopDragging();
                }
                pressedLeft = false;
                return true;
            } else if (pressedRight) {
                if (delegate != null) {
                    delegate.didStopDragging();
                }
                pressedRight = false;
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (pressedLeft) {
                startX = (int) (x - pressDx);
                if (startX < AndroidUtilities.dp(16)) {
                    startX = AndroidUtilities.dp(16);
                } else if (startX > endX) {
                    startX = endX;
                }
                progressLeft = (float) (startX - AndroidUtilities.dp(16)) / (float) width;
                if (progressRight - progressLeft > maxProgressDiff) {
                    progressRight = progressLeft + maxProgressDiff;
                } else if (minProgressDiff != 0 && progressRight - progressLeft < minProgressDiff) {
                    progressLeft = progressRight - minProgressDiff;
                    if (progressLeft < 0) {
                        progressLeft = 0;
                    }
                }
                if (delegate != null) {
                    Log.d("touch log", "onTouchEvent: " + progressLeft);
                    delegate.onLeftProgressChanged(progressLeft);
                }
                start_handle=progressLeft;
                start_duration=start_handle*duration;
                invalidate();
                return true;
            } else if (pressedRight) {
                endX = (int) (x - pressDx);
                if (endX < startX) {
                    endX = startX;
                } else if (endX > width + AndroidUtilities.dp(16)) {
                    endX = width + AndroidUtilities.dp(16);
                }
                progressRight = (float) (endX - AndroidUtilities.dp(16)) / (float) width;
                if (progressRight - progressLeft > maxProgressDiff) {
                    progressLeft = progressRight - maxProgressDiff;
                } else if (minProgressDiff != 0 && progressRight - progressLeft < minProgressDiff) {
                    progressRight = progressLeft + minProgressDiff;
                    if (progressRight > 1.0f) {
                        progressRight = 1.0f;
                    }
                }
                if (delegate != null) {
                    Log.d("touch right", "onTouchEvent: " + progressRight);
                    delegate.onRightProgressChanged(progressRight);
                }
                end_handle=progressRight;
                end_duration=progressRight*duration;
                invalidate();
                return true;
            }
        }
        return false;
    }

    public void setColor(int color) {
        paint.setColor(color);
    }

    public void setVideoPath(String path) {
        destroy();
        mediaMetadataRetriever = new MediaMetadataRetriever();
        progressLeft = 0.0f;
        progressRight = 1.0f;
        path=path;
        try {
            mediaMetadataRetriever.setDataSource(path);
            Log.d(TAG, "setVideoPath: path " + path);
            String duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            this.duration=new Float(duration);

            videoLength = Long.parseLong(duration);

            Log.d(TAG, "setVideoPath: dataRetriever " + mediaMetadataRetriever);
        } catch (Exception e) {
            Log.d(TAG, "setVideoPath: " + e);
        }
        invalidate();
    }

    public void setDelegate(VideoTimelineViewDelegate delegate) {
        this.delegate = delegate;
    }

    @SuppressLint("StaticFieldLeak")
    private void reloadFrames(int frameNum) {
        if (mediaMetadataRetriever == null) {
            return;
        }
        if (frameNum == 0) {
            if (isRoundFrames) {
                frameHeight = frameWidth = AndroidUtilities.dp(80);
                framesToLoad = (int) Math.ceil((getMeasuredWidth() - AndroidUtilities.dp(16)) / (frameHeight / 2.0f));
            } else {
                frameHeight = AndroidUtilities.dp(80);
                framesToLoad = (getMeasuredWidth() - AndroidUtilities.dp(16)) / frameHeight;
                frameWidth = (int) Math.ceil((float) (getMeasuredWidth() - AndroidUtilities.dp(16)) / (float) framesToLoad);
            }
            frameTimeOffset = videoLength / framesToLoad;
        }
        currentTask = new AsyncTask<Integer, Integer, Bitmap>() {
            private int frameNum = 0;

            @Override
            protected Bitmap doInBackground(Integer... objects) {
                frameNum = objects[0];
                Bitmap bitmap = null;
                if (isCancelled()) {
                    return null;
                }
                try {
                    bitmap = mediaMetadataRetriever.getFrameAtTime(frameTimeOffset * frameNum * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    if (isCancelled()) {
                        return null;
                    }
                    if (bitmap != null) {
                        Bitmap result = Bitmap.createBitmap(frameWidth, frameHeight, bitmap.getConfig());
                        Canvas canvas = new Canvas(result);
                        float scaleX = (float) frameWidth / (float) bitmap.getWidth();
                        float scaleY = (float) frameHeight / (float) bitmap.getHeight();
                        float scale = scaleX > scaleY ? scaleX : scaleY;
                        int w = (int) (bitmap.getWidth() * scale);
                        int h = (int) (bitmap.getHeight() * scale);
                        Rect srcRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                        Rect destRect = new Rect((frameWidth - w) / 2, (frameHeight - h) / 2, w, h);
                        canvas.drawBitmap(bitmap, srcRect, destRect, null);
                        bitmap.recycle();
                        bitmap = result;
                    }
                } catch (Exception e) {
                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (!isCancelled()) {
                    frames.add(bitmap);
                    invalidate();
                    if (frameNum < framesToLoad) {
                        reloadFrames(frameNum + 1);
                    }
                }
            }
        };
        currentTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, frameNum, null, null);
    }

    public void destroy() {
        synchronized (sync) {
            try {
                if (mediaMetadataRetriever != null) {
                    mediaMetadataRetriever.release();
                    mediaMetadataRetriever = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (Bitmap bitmap : frames) {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        frames.clear();
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
    }

    public void clearFrames() {
        for (Bitmap bitmap : frames) {
            if (bitmap != null) {
                bitmap.recycle();
            }

        }
        frames.clear();
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getMeasuredWidth() - AndroidUtilities.dp(36);
        int startX = (int) (width * progressLeft) + AndroidUtilities.dp(16);
        int endX = (int) (width * progressRight) + AndroidUtilities.dp(16);

        canvas.save();
        canvas.clipRect(AndroidUtilities.dp(16), 0, width + AndroidUtilities.dp(20), getMeasuredHeight());
        if (frames.isEmpty() && currentTask == null) {
            reloadFrames(0);
        } else {
            int offset = 0;
            for (int a = 0; a < frames.size(); a++) {
                Bitmap bitmap = frames.get(a);
                if (bitmap != null) {
                    int x = AndroidUtilities.dp(16) + offset * (isRoundFrames ? frameWidth / 2 : frameWidth);
                    int y = AndroidUtilities.dp(2);
                    if (isRoundFrames) {
                        rect2.set(x, y, x + AndroidUtilities.dp(28), y + AndroidUtilities.dp(28));
                        canvas.drawBitmap(bitmap, rect1, rect2, null);
                    } else {
                        canvas.drawBitmap(bitmap, x, y, null);
                    }
                }
                offset++;
            }
        }

        int top = AndroidUtilities.dp(2);
//      paint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));


        canvas.drawRect(AndroidUtilities.dp(16), top, startX, getMeasuredHeight() - top, paint2);
        canvas.drawRect(endX + AndroidUtilities.dp(4), top, AndroidUtilities.dp(16) + width + AndroidUtilities.dp(4), getMeasuredHeight() - top, paint2);

        //canvas.clipRect(endX + AndroidUtilities.dp(4), top, AndroidUtilities.dp(16) + width + AndroidUtilities.dp(4), getMeasuredHeight() - top, Region.Op.REPLACE);

        canvas.drawRect(startX, 0, startX + AndroidUtilities.dp(2), getMeasuredHeight(), paint);
        canvas.drawRect(endX + AndroidUtilities.dp(2), 0, endX + AndroidUtilities.dp(4), getMeasuredHeight(), paint);
        canvas.drawRect(startX + AndroidUtilities.dp(2), 0, endX + AndroidUtilities.dp(4), top, paint);
        canvas.drawRect(startX + AndroidUtilities.dp(2), getMeasuredHeight() - top, endX + AndroidUtilities.dp(4), getMeasuredHeight(), paint);

        canvas.restore();

        canvas.drawCircle(startX, getMeasuredHeight() / 2, AndroidUtilities.dp(21), paint);
        canvas.drawCircle(endX + AndroidUtilities.dp(4), getMeasuredHeight() / 2, AndroidUtilities.dp(21), paint);
        canvas.save();
        //delegate.doLeftTranslate(width,endX + AndroidUtilities.dp(4));
    }

    public float getDurationPlaying() {
        return end_duration-start_duration;
    }


}