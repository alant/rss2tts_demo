package com.ahtang.rss_demo;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;


import org.jsoup.Jsoup;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alant on 3/31/17.
 */

public class RetrieveFeedTask extends AsyncTask<String, Void, Boolean> {

    private String mFeedTitle;
    private String mFeedLink;
    private String mFeedDescription;
    List<RssFeedModel> mFeedModelList;
    ButtonFragment buttonF;
    private String TAG = "RetrieveFeedTask";

    public RetrieveFeedTask(ButtonFragment buttonFragment) {
        buttonF = buttonFragment;
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    protected Boolean doInBackground(String... urls) {
        System.out.println("urls : " + urls);
        String uri = urls[0];
        System.out.println("url to fetch: " + uri);

        try {
            URL url = new URL(uri);
            InputStream inputStream = url.openConnection().getInputStream();
            mFeedModelList = parseFeed(inputStream);
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

        return false;
    }

    public List<RssFeedModel> parseFeed(InputStream inputStream) throws XmlPullParserException,
            IOException {
        String title = null;
        String link = null;
        String description = null;
        boolean isItem = false;
        List<RssFeedModel> items = new ArrayList<>();

        try {
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            xmlPullParser.setInput(inputStream, null);

            xmlPullParser.nextTag();
            while (xmlPullParser.next() != XmlPullParser.END_DOCUMENT) {
                int eventType = xmlPullParser.getEventType();

                String name = xmlPullParser.getName();
                if(name == null)
                    continue;

                if(eventType == XmlPullParser.END_TAG) {
                    if(name.equalsIgnoreCase("item")) {
                        isItem = false;
                    }
                    continue;
                }

                if (eventType == XmlPullParser.START_TAG) {
                    if(name.equalsIgnoreCase("entry")) {
                        isItem = true;
                        continue;
                    }
                }

                Log.d("MyXmlParser", "Parsing name ==> " + name);
                String result = "";
                if (xmlPullParser.next() == XmlPullParser.TEXT) {
                    result = xmlPullParser.getText();
                    xmlPullParser.nextTag();
                }

                if (name.equalsIgnoreCase("title")) {
                    title = result;
                } else if (name.equalsIgnoreCase("link")) {
                    link = result;
                } else if (name.equalsIgnoreCase("content")) {
                    description = result;
                }

                if (title != null && link != null && description != null) {
                    if(isItem) {
                        description = Jsoup.parse(description).text();
                        Log.d("MyXmlParser", "content ==> " + description);
                        RssFeedModel item = new RssFeedModel(title, link, description);
                        items.add(item);
                    }
                    else {
                        mFeedTitle = title;
                        mFeedLink = link;
                        mFeedDescription = description;
                    }

                    title = null;
                    link = null;
                    description = null;
                    isItem = false;
                }
            }

            if (items.isEmpty()) {
                Log.d(TAG, "WTF? empty items?");
            } else {
                Log.d(TAG, "all good, got items");
            }
            return items;
        } finally {
            inputStream.close();
        }
    }

    protected void onPostExecute(Boolean success) {
        // TODO: check this.exception
        // TODO: do something with the feed
        if (success) {
            System.out.println("Good task succeeded ~");
            if (mFeedModelList.isEmpty()) {
                Log.d(TAG, "WTF? empty mFeedModelList?");
            } else {
                Log.d(TAG, "all good got mFeedMOdelLIst");
            }
            buttonF.playerReady(mFeedModelList);
        } else {
            System.out.println("Too bad! RetrieveFeedTask did not succeed :(");
        }
    }
}
