package com.example.neutrino.maze;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by Greg Stein on 9/9/2016.
 */
public class MazeServer {
    private static final String POST_METHOD = "POST";
    private static URL mUrl;
    private static final MazeServer instance = new MazeServer();
    private MazeServer() {}

    public static MazeServer getServer() {
        return instance;
    }

    public static boolean connectionAvailable(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public void downloadFloorPlan(Context context, AsyncResponse response) {
        // TODO: Use async http client library instead
        new DownloadFloorPlanTask(context, response).execute();
    }

    public void uploadFloorPlan(Context context, String jsonString) {
        // TODO: Use async http client library instead
        new UploadFloorPlanTask(context).execute(jsonString);
    }

    private String extractLines(InputStream iStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(iStream));
        StringBuilder result = new StringBuilder();
        String line;

        while((line = reader.readLine()) != null) {
            result.append(line);
        }

        return result.toString();
    }

    public interface AsyncResponse {
        void processFinish(String jsonString);
    }

    private class DownloadFloorPlanTask extends AsyncTask<Void, Void, String> {
        private static final String FUNC = "get_data";
        private static final String GET_METHOD = "GET";
        private Context mContext;

        public AsyncResponse delegate = null;

        public DownloadFloorPlanTask(Context context, AsyncResponse delegate){
            this.mContext = context;
            this.delegate = delegate;
        }

        @Override
        protected void onPostExecute(String result) {
            if (delegate != null) {
                delegate.processFinish(result);
            }

            Toast.makeText(mContext, result, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(Void... voids) {
            String response = null;

            Uri.Builder queryBuilder = new Uri.Builder()
                    .appendQueryParameter("collection", "FloorPlan");
            String query = queryBuilder.build().getEncodedQuery();

            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(mContext.getString(R.string.maze_server_api_url))
                    .append(FUNC).append('?')
                    .append(query);

            try {
                URL url = new URL(urlBuilder.toString());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod(GET_METHOD);
                conn.setDoInput(true);
                conn.setDoOutput(true);

                int responseCode=conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream responseData = conn.getInputStream();
                    response = extractLines(responseData);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return response;
        }
    }

    private class UploadFloorPlanTask extends AsyncTask<String, Void, String> {
        private static final String FUNC = "insert_data";
        private static final String POST_METHOD = "POST";
        private final Context mContext;

        public UploadFloorPlanTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(mContext, result, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(String... jsonStrings) {
            String jsonString = jsonStrings[0];
            String response;

            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(mContext.getString(R.string.maze_server_api_url))
                    .append(FUNC);

            try {
                URL url = new URL(urlBuilder.toString());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod(POST_METHOD);
                conn.setDoOutput(true);

                OutputStream oStream = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(oStream, "UTF-8"));

                Uri.Builder queryBuilder = new Uri.Builder()
                        .appendQueryParameter("collection", "FloorPlan")
                        .appendQueryParameter("data", jsonString);
                String query = queryBuilder.build().getEncodedQuery();

                writer.write(query);
                writer.close();
                oStream.close();

                int responseCode=conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream responseData = conn.getInputStream();
                    response = extractLines(responseData);
                }
                else {
                    response = "HTTP: " + Integer.toString(responseCode);
                }
            } catch (IOException e) {
                e.printStackTrace();
                response = e.getMessage();
            }

            return response;
        }
    }

    void gettestAndroidAsyncHttpClientLib(JsonHttpResponseHandler handler) {
        String url = "https://ajax.googleapis.com/ajax/services/search/images";
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("q", "android");
        params.put("rsz", "8");
        client.get(url, params, handler);
    }
}
