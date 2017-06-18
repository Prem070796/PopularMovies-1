package com.cyril.udacity.moviepop.remote;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.util.Log;

import com.cyril.udacity.moviepop.BuildConfig;
import com.cyril.udacity.moviepop.model.Movie;
import com.cyril.udacity.moviepop.model.Review;
import com.cyril.udacity.moviepop.model.Trailer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to API Service Call.
 */
public class APIServiceCall {
	private static final String TAG = APIServiceCall.class.getSimpleName();

	public <T> List<T> call(final Activity activity, final String featurePath) {
		List<T> result = null;
		if (activity == null) {
			return null;
		}
		// Check if the internet connection is available
		final ConnectivityManager conMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (conMgr.getActiveNetworkInfo() == null
			|| !conMgr.getActiveNetworkInfo().isAvailable()
			|| !conMgr.getActiveNetworkInfo().isConnected()) {
			return result;
		}
		// These two need to be declared outside the try/catch
		// so that they can be closed in the finally block.
		HttpURLConnection urlConnection = null;
		BufferedReader reader = null;
		try {
			// Construct the URL for the TheMovieDB query
			// Build URI
			Uri uri = Uri.parse(TheMovieDbApi.Config.URL).buildUpon()
				.appendPath(TheMovieDbApi.VERSION_3.VERSION_ID)
				.appendPath(TheMovieDbApi.Config.MOVIE_PATH)
				.appendEncodedPath(featurePath)
				.appendQueryParameter(TheMovieDbApi.Config.API_KEY_PARAM, BuildConfig.THE_MOVIE_DB_API_KEY)
				.build();
			Log.d(TAG, "Built URI is " + uri.toString());
			// Create the request to TheMovieDB, and open the connection
			urlConnection = (HttpURLConnection) (new URL(uri.toString())).openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.connect();
			// Read the input stream into a String
			InputStream inputStream = urlConnection.getInputStream();
			StringBuffer buffer = new StringBuffer();
			if (inputStream == null) {
				// Nothing to do.
				return null;
			}
			reader = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			while ((line = reader.readLine()) != null) {
				// Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
				// But it does make debugging a *lot* easier if you print out the completed
				// buffer for debugging.
				buffer.append(line + "\n");
			}
			if (buffer.length() == 0) {
				// Stream was empty.  No point in parsing.
				return null;
			}
			// Will contain the raw JSON response as a string.
			final String movieJsonStr = buffer.toString();
			Log.i(TAG, movieJsonStr);
			// Conversion of response string to Multiple String for display
			result = new ArrayList<>();
			try {
				JSONObject responseJson = new JSONObject(movieJsonStr);
				JSONArray responseArray = responseJson.getJSONArray(TheMovieDbApi.VERSION_3.RESPONSE.LIST);
				for (int i = 0; i < responseArray.length(); i++) {
					final JSONObject obj = responseArray.getJSONObject(i);
					if (obj.has(TheMovieDbApi.VERSION_3.RESPONSE.VIDEO_KEY)) {
						result.add((T) new Trailer(obj));
					} else if (obj.has(TheMovieDbApi.VERSION_3.RESPONSE.REVIEW_AUTHOR)) {
						result.add((T) new Review(obj));
					} else {
						result.add((T) new Movie(obj));
					}
				}
			} catch (final JSONException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			Log.e(TAG, "Error ", e);
			// If the code didn't successfully get the weather data, there's no point in attemping
			// to parse it.
			return null;
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException e) {
					Log.e(TAG, "Error closing stream", e);
				}
			}
		}
		return result;
	}
}
