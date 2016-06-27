package com.udacity.garyshem.booklisting;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    EditText queryField;

    // Create class that handles the requests.
    // This way we can simply change the request string with every search
    private ConnectivityManager mConnectivityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        queryField = (EditText) findViewById(R.id.edit_query);
    }

    public void startSearch(View view) {
        final String queryText = queryField.getText().toString();
        // If the query field is empty, do not start the search
        // Since this function is of return type void, we don't lose anything
        // by returning prematurely
        if (queryText.trim().equals("")) {
            printError("Empty query");
            return;
        }
        // Check internet connection
        // If there's no connection
        if (isNetworkConnected() == false) {
            printError("No internet connection");
            return;
        }
        // Otherwise, start the search
        // Reform the query to match Google Books API
        // For this, make an anonymous AsyncTask, so it has direct access to activity
        AsyncTask<String, Object, ArrayList<Book>> request =
                new AsyncTask<String, Object, ArrayList<Book>>() {
            @Override
            protected ArrayList<Book> doInBackground(String... strings) {
                // Stop if cancelled
                if (isCancelled()) {
                    return null;
                }
                ArrayList<Book> books = null;
                String apiUrlString = "https://www.googleapis.com/books/v1/volumes?q=" + strings[0];
                HttpsURLConnection connection = null;
                // Build Connection.
                try {
                    URL url = new URL(apiUrlString);
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    connection.setReadTimeout(5000); // 5 seconds
                    connection.setConnectTimeout(5000); // 5 seconds

                    int responseCode = connection.getResponseCode();
                    if (responseCode != 200) {
                        Log.w(getClass().getName(), "GoogleBooksAPI request failed. Response Code: " + responseCode);
                        connection.disconnect();
                        return null;
                    }
                    // Read data from response.
                    StringBuilder builder = new StringBuilder();
                    BufferedReader responseReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line = responseReader.readLine();
                    while (line != null) {
                        builder.append(line);
                        line = responseReader.readLine();
                    }
                    String responseString = builder.toString();
                    Log.d(getClass().getName(), "Response String: " + responseString);
                    // Close connection and return response code.
                    connection.disconnect();

                    JSONObject responseJson = new JSONObject(responseString);
                    Log.i(getClass().getName(), responseJson.toString());
                    JSONArray items = responseJson.getJSONArray("items");

                    books = new ArrayList<>();

                    for (int i = 0; i < items.length(); i += 1) {
                        // Get a book from the array
                        JSONObject currentBook = items.getJSONObject(i);
                        // Get volumeInfo which contains all interesting book info
                        JSONObject bookInfo = currentBook.getJSONObject("volumeInfo");

                        // Note that there may be several authors, stored in the array
                        // But I think that reading one of them is enough for our purposes
                        String author = bookInfo.getJSONArray("authors").getString(0);
                        String title = bookInfo.getString("title");
                        books.add(new Book(author, title));
                    }
                } catch (MalformedURLException e) {
                    Log.i(getClass().getName(), e.getMessage());
                } catch (ProtocolException e) {
                    Log.i(getClass().getName(), e.getMessage());
                } catch (IOException e) {
                    Log.i(getClass().getName(), e.getMessage());
                } catch (JSONException e) {
                    Log.i(getClass().getName(), e.getMessage());
                }
                return books;
            }


            @Override
            protected void onPostExecute(ArrayList<Book> books) {
                ListView list = (ListView) findViewById(R.id.list_books);
                TextView helpTextView = (TextView) findViewById(R.id.help_text_view);
                // in case we haven't received any books
                if (books == null) {
                    Log.i(getClass().getName(), "Books is null");
                    return;
                } else {
                    helpTextView.setVisibility(View.GONE);
                    list.setVisibility(View.VISIBLE);
                    list.setAdapter(new BookAdapter(MainActivity.this, books));
                    list.invalidateViews();
                }
            }
        };
        request.execute(queryText);
    }


    private void printError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        Log.i(getClass().getName(), errorMessage);
    }

    protected boolean isNetworkConnected() {

        // Instantiate mConnectivityManager if necessary
        if (mConnectivityManager == null) {
            mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        // Is device connected to the Internet?
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }
}
