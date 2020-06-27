/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amazonaws.services.chime.sdk.meetings.utils.Versioning
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.trustingsocial.tvsdk.TVDetectionError
import com.trustingsocial.tvsdk.internal.TrustVisionSDK
import com.trustingsocial.tvsdk.internal.TrustVisionSDK.TVInitializeListener
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MeetingHomeActivity : AppCompatActivity() {
    private val logger = ConsoleLogger(LogLevel.INFO)
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val WEBRTC_PERMISSION_REQUEST_CODE = 1
    private val MEETING_REGION = "us-east-1"
    private val TAG = "MeetingHomeActivity"

    private val WEBRTC_PERM = arrayOf(
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
        Manifest.permission.RECORD_AUDIO
    )

    private var meetingEditText: EditText? = null
    private var nameEditText: EditText? = null
    private var authenticationProgressBar: ProgressBar? = null
    private var meetingID: String? = null
    private var yourName: String? = null

    companion object {
        const val MEETING_RESPONSE_KEY = "MEETING_RESPONSE"
        const val MEETING_ID_KEY = "MEETING_ID"
        const val NAME_KEY = "NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meeting_home)
        meetingEditText = findViewById(R.id.editMeetingId)
        nameEditText = findViewById(R.id.editName)
        authenticationProgressBar = findViewById(R.id.progressAuthentication)

        findViewById<Button>(R.id.buttonContinue)?.setOnClickListener { joinMeeting() }

        val versionText: TextView = findViewById(R.id.versionText) as TextView
        versionText.text = "${getString(R.string.version_prefix)}${Versioning.sdkVersion()}"
    }

    private fun joinMeeting() {
        meetingID = meetingEditText?.text.toString().trim().replace("\\s+".toRegex(), "+")
        yourName = nameEditText?.text.toString().trim().replace("\\s+".toRegex(), "+")

        if (meetingID.isNullOrBlank()) {
            Toast.makeText(
                this,
                getString(R.string.user_notification_meeting_id_invalid),
                Toast.LENGTH_LONG
            ).show()
        } else if (yourName.isNullOrBlank()) {
            Toast.makeText(
                this,
                getString(R.string.user_notification_attendee_name_invalid),
                Toast.LENGTH_LONG
            ).show()
        } else {
            if (hasPermissionsAlready()) {
                TrustVisionSDK.init(this, object : TVInitializeListener {
                    override fun onInitSuccess() {
                        authenticate(getString(R.string.test_url), meetingID, yourName)
                    }

                    override fun onInitError(error: TVDetectionError) {
                        Log.e("VKNLog", error.errorDescription)
                    }
                })
            } else {
                ActivityCompat.requestPermissions(this, WEBRTC_PERM, WEBRTC_PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun hasPermissionsAlready(): Boolean {
        return WEBRTC_PERM.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissionsList: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            WEBRTC_PERMISSION_REQUEST_CODE -> {
                val isMissingPermission: Boolean =
                    grantResults.isEmpty() || grantResults.any { PackageManager.PERMISSION_GRANTED != it }

                if (isMissingPermission) {
                    Toast.makeText(
                        this,
                        getString(R.string.user_notification_permission_error),
                        Toast.LENGTH_LONG
                    )
                        .show()
                    return
                }
                authenticate(getString(R.string.test_url), meetingID, yourName)
            }
        }
    }

    private fun authenticate(
        meetingUrl: String,
        meetingId: String?,
        attendeeName: String?
    ) =
        uiScope.launch {
            authenticationProgressBar?.visibility = View.VISIBLE
            logger.info(TAG, "Joining meeting. meetingUrl: $meetingUrl, meetingId: $meetingId, attendeeName: $attendeeName")

            val meetingResponseJson: String? = joinMeeting(meetingUrl, meetingId, attendeeName)

            authenticationProgressBar?.visibility = View.INVISIBLE

            if (meetingResponseJson == null) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.user_notification_meeting_start_error),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val intent = Intent(applicationContext, InMeetingActivity::class.java)
                intent.putExtra(MEETING_RESPONSE_KEY, meetingResponseJson)
                intent.putExtra(MEETING_ID_KEY, meetingId)
                intent.putExtra(NAME_KEY, attendeeName)
                startActivity(intent)
            }
        }

    private suspend fun joinMeeting(
        meetingUrl: String,
        meetingId: String?,
        attendeeName: String?
    ): String? {
        return withContext(ioDispatcher) {
            val serverUrl =
                URL(
                    "${meetingUrl}join?title=${encodeURLParam(meetingId)}&name=${encodeURLParam(
                        attendeeName
                    )}&region=${encodeURLParam(MEETING_REGION)}"
                )

            try {
                val response = StringBuffer()
                with(serverUrl.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true

                    BufferedReader(InputStreamReader(inputStream)).use {
                        var inputLine = it.readLine()
                        while (inputLine != null) {
                            response.append(inputLine)
                            inputLine = it.readLine()
                        }
                        it.close()
                    }

                    if (responseCode == 200) {
                        response.toString()
                    } else {
                        logger.error(TAG, "Unable to join meeting. Response code: $responseCode")
                        null
                    }
                }
            } catch (exception: Exception) {
                logger.error(TAG, "There was an exception while joining the meeting: $exception")
                null
            }
        }
    }
}
