/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import com.amazon.chime.webrtc.EglBase
import com.amazon.chime.webrtc.SurfaceViewRenderer
import com.amazon.chime.webrtc.VideoRenderer
import java.lang.Exception

class DefaultVideoRenderView : SurfaceViewRenderer, VideoRenderView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    private var capturedFrameI420Frame: VideoRenderer.I420Frame? = null

    override fun renderFrame(frame: Any) {
        val i420Frame = frame as VideoRenderer.I420Frame
        val newI420Frame = VideoRenderer.I420Frame(i420Frame.width, i420Frame.height, i420Frame.rotationDegree, i420Frame.yuvStrides, i420Frame.yuvPlanes)
        this.renderFrame(i420Frame)
        this.capturedFrameI420Frame = newI420Frame
    }

    override fun initialize(initParams: Any?) {
        this.init((initParams as EglBase).eglBaseContext, null)
    }

    override fun finalize() {
        this.release()
    }

    fun capture(): Bitmap? {
        if (capturedFrameI420Frame != null) {
            return try {
                val yuvFrame = YuvFrame(capturedFrameI420Frame)
                yuvFrame.bitmap
            } catch (e: Exception) {
                null
            }
        }
        return null
    }
}
