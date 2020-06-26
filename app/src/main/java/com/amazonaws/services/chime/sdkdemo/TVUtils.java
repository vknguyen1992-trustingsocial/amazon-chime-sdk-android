package com.amazonaws.services.chime.sdkdemo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoRenderView;

public class TVUtils {
    static DefaultVideoRenderView localTileView;

    public static Bitmap loadBitmapFromView(View v) {
        Bitmap b = Bitmap.createBitmap(v.getWidth() , v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.draw(c);
        return b;
    }
}
