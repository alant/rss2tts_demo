package com.ahtang.rss_demo;

import android.app.Fragment;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;
import com.amazonaws.services.polly.model.Voice;
import com.amazonaws.services.securitytoken.model.PackedPolicyTooLargeException;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Created by alant on 4/2/17.
 */

public class ButtonFragment extends Fragment {
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // Amazon Polly permissions.
    private static final String COGNITO_POOL_ID = "REPLACE_WITH_YOUR_ID";
    // Region of Amazon Polly.
    private static final Regions MY_REGION = Regions.US_WEST_2;

    private CognitoCachingCredentialsProvider credentialsProvider;
    private AmazonPollyPresigningClient client;
    private MediaPlayer mediaPlayer;
    private static final String TAG = "rss2tts_demo_fbtnf";
    private Button playButton;
    private Context c;
    private List<Voice> voices;

    private Spinner voicesSpinner;

    public  View onCreateView(LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.button, container, false);
        initPollyClient();
        new GetPollyVoices().execute();
        setupNewMediaPlayer();

        Button fetchButton = (Button) view.findViewById(R.id.fetchButton);
        fetchButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.i(TAG,"clicked fetch hahaha ======");
                fetchRSS();
            }
        });

        playButton = (Button) view.findViewById(R.id.playButton);
        playButton.setEnabled(false);

        return view;
    }

    private void fetchRSS() {
        new RetrieveFeedTask(this).execute("http://blog.samaltman.com/posts.atom");
    }

    private void initPollyClient() {
        // Initialize the Amazon Cognito credentials provider.
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getActivity(),
                COGNITO_POOL_ID,
                MY_REGION
        );

        // Create a client that supports generation of presigned URLs.
        client = new AmazonPollyPresigningClient(credentialsProvider);
    }

    private void setupNewMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
                setupNewMediaPlayer();
            }
        });
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
                playButton.setEnabled(true);
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                playButton.setEnabled(true);
                return false;
            }
        });
    }


    private class GetPollyVoices extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            if (voices != null) {
                return null;
            }

            // Create describe voices request.
            DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();

            DescribeVoicesResult describeVoicesResult;
            try {
                // Synchronously ask the Polly Service to describe available TTS voices.
                describeVoicesResult = client.describeVoices(describeVoicesRequest);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to get available voices. " + e.getMessage());
                return null;
            }

            // Get list of voices from the result.
            voices = describeVoicesResult.getVoices();

            // Log a message with a list of available TTS voices.
            Log.i(TAG, "Available Polly voices: " + voices);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (voices == null) {
                Log.e(TAG, "voices is null ");
                return;
            }
        }
    }


    public void playerReady(final List<RssFeedModel> mFeedModelList) {
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playButton.setEnabled(false);
                Voice selectedVoice = null;
                
                if (!voices.isEmpty()) {
                    selectedVoice = voices.get(0);
                } else {
                    Log.i(TAG, "voices empty!");
                }
                
                String textToRead = "";
                if (!mFeedModelList.isEmpty()) {
                    textToRead = mFeedModelList.get(0).description;
                } else {
                    Log.i(TAG, "mFeedModelLIst is empty!");
                }
               
                // Use voice's sample text if user hasn't provided any text to read.
                if (textToRead.trim().isEmpty()) {
                    textToRead = "too bad nothing to read";
                }

                // Create speech synthesis request.
                SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest =
                        new SynthesizeSpeechPresignRequest()
                                // Set text to synthesize.
                                .withText(textToRead)
                                // Set voice selected by the user.
                                .withVoiceId(selectedVoice.getId())
                                // Set format to MP3.
                                .withOutputFormat(OutputFormat.Mp3);

                // Get the presigned URL for synthesized speech audio stream.
                URL presignedSynthesizeSpeechUrl =
                        client.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest);

                Log.i(TAG, "Playing speech from presigned URL: " + presignedSynthesizeSpeechUrl);

                // Create a media player to play the synthesized audio stream.
                if (mediaPlayer.isPlaying()) {
                    setupNewMediaPlayer();
                }
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                try {
                    // Set media player's data source to previously obtained URL.
                    mediaPlayer.setDataSource(presignedSynthesizeSpeechUrl.toString());
                } catch (IOException e) {
                    Log.e(TAG, "Unable to set data source for the media player! " + e.getMessage());
                }

                // Start the playback asynchronously (since the data source is a network stream).
                mediaPlayer.prepareAsync();
            }
        });
        playButton.setEnabled(true);
    }
}
