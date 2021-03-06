package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static android.R.attr.textSize;
import static android.R.attr.width;
import static android.R.attr.x;
import static android.R.attr.y;

public class WatchFaceDigital extends CanvasWatchFaceService {

    public static final String PATH = "/location";
    public String WEATHER = "weather";
    public String HIGH_TEMP = "high_temp";
    public String LOW_TEMP = "low_temp";
    public String DESC = "desc";
    public String ICON = "icon";
    public String TIME = "time";
    public final String TAG = "Data_item_set";

    public int WeatherId;
    public String High_Temp = "";
    public String Low_Temp = "";
    public String Desc = "";
    public Bitmap bitmap;

    public static final String TAG_1 = "onConnected";
    private static final String TAG_2 = "onConnectionSuspended";
    private static final String TAG_3 = "onConnectionFailed";
    private static final String TAG_4 = "onDataChanged";
    private static final String LOG_TAG = "WatchFaceAnalog";
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private static final int MSG_UPDATE_TIME = 0;

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<Engine> mWeakReference;

        public EngineHandler(WatchFaceDigital.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            WatchFaceDigital.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {

        boolean mIsRound;
        int mChinSize;

        float Ht;
        float Lt;
        float Description;
        float IconW;
        float IconH;

        GoogleApiClient mGoogleApiClient;

        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mTextPaint;
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

        float mXOffset;
        float mYOffset;
        float offsetY;

        boolean mLowBitAmbient;

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {

            for (DataEvent event : dataEvents) {
                Log.d(TAG_4, "onDataChanged: " + dataEvents);

                if (event.getType() == DataEvent.TYPE_CHANGED) {

                    DataItem item = event.getDataItem();
                    if (item.getUri().getPath().compareTo(PATH) == 0) {

                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        WeatherId = dataMap.getInt(WEATHER);
                        High_Temp = dataMap.getString(HIGH_TEMP);
                        Low_Temp = dataMap.getString(LOW_TEMP);
                        Desc = dataMap.getString(DESC);
                        final Asset profileAsset = dataMap.getAsset(ICON);
                        bitmap = loadBitmapFromAsset(profileAsset);
                    }
                }
            }
        }

        public Bitmap loadBitmapFromAsset(Asset asset) {
            if (asset == null) {
                throw new IllegalArgumentException("Asset must be non-null");
            }

            long TIMEOUT_MS = 1;
            ConnectionResult result =
                    mGoogleApiClient.blockingConnect(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!result.isSuccess()) {
                return null;
            }

            InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                    mGoogleApiClient, asset).await().getInputStream();

            if (assetInputStream == null) {
                String TAG = "Unknown asset";
                Log.w(TAG, "Requested an unknown asset");
                return null;
            }
            return BitmapFactory.decodeStream(assetInputStream);
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Wearable.DataApi.addListener(mGoogleApiClient, this);
            Log.d(TAG_1, "onConnected: " + bundle);
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG_2, "onConnectionSuspended: " + i);
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult result) {
            Log.d(TAG_3, "onConnectionFailed: " + result);
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceDigital.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = WatchFaceDigital.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);
            offsetY = resources.getDimension(R.dimen.offset_y);

            Ht = resources.getDimension(R.dimen.high_Temp);
            Lt = resources.getDimension(R.dimen.low_Temp);
            Description = resources.getDimension(R.dimen.desc);
            IconW = resources.getDimension(R.dimen.bitmapWidth);
            IconH = resources.getDimension(R.dimen.bitmapHeight);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mTime = new Time();

            mGoogleApiClient = new GoogleApiClient.Builder(WatchFaceDigital.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(WatchFaceDigital.Engine.this)
                    .addOnConnectionFailedListener(WatchFaceDigital.Engine.this)
                    .build();
            mGoogleApiClient.connect();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WatchFaceDigital.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WatchFaceDigital.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            mIsRound = insets.isRound();
            mChinSize = insets.getSystemWindowInsetBottom();

            Resources resources = WatchFaceDigital.this.getResources();

            mXOffset = resources.getDimension(mIsRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(mIsRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mTextPaint.setTextSize(textSize);
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
                    mTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            updateTimer();
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = WatchFaceDigital.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:

                    break;
                case TAP_TYPE_TOUCH_CANCEL:

                    break;
                case TAP_TYPE_TAP:

                    mTapCount++;
                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                            R.color.background : R.color.background2));
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);

            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

                int width = bounds.width();
                int height = bounds.height();

                float x = width / 1.7f;
                float y = height / 5.8f;
                float a = width / 1.7f;
                float b = height / 4.5f;
                float c = width / 2.9f;
                float d = height / 4.5f;
                float e = width / 2.8f;
                float f = height / 14f;

                Paint paint = new Paint();

                Paint highTemp = new Paint();
                highTemp.setTextSize(Ht);
                highTemp.setAntiAlias(true);
                canvas.drawText((High_Temp), x, y, highTemp);

                Paint lowTemp = new Paint();
                lowTemp.setTextSize(Lt);
                lowTemp.setAntiAlias(true);
                canvas.drawText((Low_Temp), a, b, lowTemp);

                Paint descPaint = new Paint();
                descPaint.setTextSize(Description);
                descPaint.setAntiAlias(true);

                canvas.drawText(Desc, c, d, descPaint);
                Log.d(Desc, "Receiving description");

                if (bitmap != null) {

                    int width1 = (int) IconW;
                    int height1 = (int) IconH;
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width1, height1, true);
                    canvas.drawBitmap(scaledBitmap, e, f, paint);
                }

                mTime.setToNow();
                String text = mAmbient
                        ? String.format("%d:%02d", mTime.hour, mTime.minute)
                        : String.format("%d:%02d:%02d", mTime.hour, mTime.minute, mTime.second);
                canvas.drawText(text, mXOffset, mYOffset, mTextPaint);
            }
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

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
