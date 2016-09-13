package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class WatchFace extends CanvasWatchFaceService implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {



    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<WatchFace.Engine> mWeakReference;

        public EngineHandler(WatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            WatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mHandPaint;
        boolean mAmbient;
        Time mTime;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        int mTapCount;


        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
        }

        @Override
        public void onConnected(Bundle bundle) {

        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {


        }


        setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFace.this)

        .

        setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)

        .

        setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)

        .

        setShowSystemUiTime(false)

        .

        setAcceptsTapEvents(true)

        .

        build()

        );

        Resources resources = WatchFace.this.getResources();

        mBackgroundPaint=new

        Paint();

        mBackgroundPaint.setColor(resources.getColor(R.color.background));

        mHandPaint=new

        Paint();

        mHandPaint.setColor(resources.getColor(R.color.analog_hands));
        mHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_hand_stroke));
        mHandPaint.setAntiAlias(true);
        mHandPaint.setStrokeCap(Paint.Cap.ROUND);

        mTime=new

        Time();
    }

    @Override
    public void onDestroy() {
        mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
        super.onDestroy();
    }

    @Override
    public void onPropertiesChanged(Bundle properties) {
        super.onPropertiesChanged(properties);
        mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
    }

    @Override
    public void onTimeTick() {
        super.onTimeTick();
        invalidate();
    }

    @Override
    public void onAmbientModeChanged(boolean inAmbientMode) {
        super.onAmbientModeChanged(inAmbientMode);
        if (mAmbient != inAmbientMode) {
            mAmbient = inAmbientMode;
            if (mLowBitAmbient) {
                mHandPaint.setAntiAlias(!inAmbientMode);
            }
            invalidate();
        }

        // Whether the timer should be running depends on whether we're visible (as well as
        // whether we're in ambient mode), so we may need to start or stop the timer.
        updateTimer();
    }

    /**
     * Captures tap event (and tap type) and toggles the background color if the user finishes
     * a tap.
     */
    @Override
    public void onTapCommand(int tapType, int x, int y, long eventTime) {
        Resources resources = WatchFace.this.getResources();
        switch (tapType) {
            case TAP_TYPE_TOUCH:
                // The user has started touching the screen.
                break;
            case TAP_TYPE_TOUCH_CANCEL:
                // The user has started a different gesture or otherwise cancelled the tap.
                break;
            case TAP_TYPE_TAP:
                // The user has completed the tap gesture.
                mTapCount++;
                mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                        R.color.background : R.color.background2));
                break;
        }
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas, Rect bounds) {
        mTime.setToNow();

        // Draw the background.
        if (isInAmbientMode()) {
            canvas.drawColor(Color.BLACK);
        } else {
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);
        }

        // Find the center. Ignore the window insets so that, on round watches with a
        // "chin", the watch face is centered on the entire screen, not just the usable
        // portion.
        float centerX = bounds.width() / 2f;
        float centerY = bounds.height() / 2f;

        float secRot = mTime.second / 30f * (float) Math.PI;
        int minutes = mTime.minute;
        float minRot = minutes / 30f * (float) Math.PI;
        float hrRot = ((mTime.hour + (minutes / 60f)) / 6f) * (float) Math.PI;

        float secLength = centerX - 20;
        float minLength = centerX - 40;
        float hrLength = centerX - 80;

        if (!mAmbient) {
            float secX = (float) Math.sin(secRot) * secLength;
            float secY = (float) -Math.cos(secRot) * secLength;
            canvas.drawLine(centerX, centerY, centerX + secX, centerY + secY, mHandPaint);
        }

        float minX = (float) Math.sin(minRot) * minLength;
        float minY = (float) -Math.cos(minRot) * minLength;
        canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mHandPaint);

        float hrX = (float) Math.sin(hrRot) * hrLength;
        float hrY = (float) -Math.cos(hrRot) * hrLength;
        canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mHandPaint);
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        super.onVisibilityChanged(visible);

        if (visible) {
            registerReceiver();

            // Update time zone in case it changed while we weren't visible.
            mTime.clear(TimeZone.getDefault().getID());
            mTime.setToNow();
        } else {
            unregisterReceiver();
        }

        // Whether the timer should be running depends on whether we're visible (as well as
        // whether we're in ambient mode), so we may need to start or stop the timer.
        updateTimer();
    }

    private void registerReceiver() {
        if (mRegisteredTimeZoneReceiver) {
            return;
        }
        mRegisteredTimeZoneReceiver = true;
        IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
        WatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
    }

    private void unregisterReceiver() {
        if (!mRegisteredTimeZoneReceiver) {
            return;
        }
        mRegisteredTimeZoneReceiver = false;
        WatchFace.this.unregisterReceiver(mTimeZoneReceiver);
    }

    /**
     * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
     * or stops it if it shouldn't be running but currently is.
     */
    private void updateTimer() {
        mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
        if (shouldTimerBeRunning()) {
            mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
        }
    }

    /**
     * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
     * only run when we're visible and in interactive mode.
     */
    private boolean shouldTimerBeRunning() {
        return isVisible() && !isInAmbientMode();
    }

    /**
     * Handle updating the time periodically in interactive mode.
     */
    private void handleUpdateTimeMessage() {
        invalidate();
        if (shouldTimerBeRunning()) {
            long timeMs = System.currentTimeMillis();
            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
        }
    }
}
}
