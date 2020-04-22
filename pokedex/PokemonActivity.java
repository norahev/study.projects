package edu.harvard.cs50.pokedex;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

public class PokemonActivity extends AppCompatActivity {

    public void toggleCatch(View view) {
        if (catched) {
            releasePokemon();
        } else {
            catchPokemon();
        }
    }
    private void catchPokemon() {
        catched = true;
        catch_button.setText("Release");
        editor.putBoolean(nameTextView.getText().toString(), true);
        editor.commit();
    }
    private void releasePokemon() {
        catched = false;
        catch_button.setText("Catch");
        editor.remove(nameTextView.getText().toString());
        editor.commit();
    }
    private TextView nameTextView;
    private TextView numberTextView;
    private TextView type1TextView;
    private TextView type2TextView;
    private TextView description;
    private String url;
    private String urlpic;
    private String urldes;
    private RequestQueue requestQueue;
    private TextView catch_button;
    private Boolean catched;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private ImageView sprite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokemon);
        sharedPreferences = getApplicationContext().getSharedPreferences("PokeCatcher", 0);
        editor = sharedPreferences.edit();
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        url = getIntent().getStringExtra("url");
        nameTextView = findViewById(R.id.pokemon_name);
        numberTextView = findViewById(R.id.pokemon_number);
        type1TextView = findViewById(R.id.pokemon_type1);
        type2TextView = findViewById(R.id.pokemon_type2);
        description = findViewById(R.id.pokemon_description);
        catch_button = findViewById(R.id.button_catch);
        catched = false;
        sprite = findViewById(R.id.sprite);

        load();
    }

    private void load() {
        type1TextView.setText("");
        type2TextView.setText("");
        description.setText("");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    nameTextView.setText(response.getString("name"));
                    numberTextView.setText(String.format("#%03d", response.getInt("id")));

                    JSONArray typeEntries = response.getJSONArray("types");
                    for (int i = 0; i < typeEntries.length(); i++) {
                        JSONObject typeEntry = typeEntries.getJSONObject(i);
                        int slot = typeEntry.getInt("slot");
                        String type = typeEntry.getJSONObject("type").getString("name");
                        if (slot == 1) {
                            type1TextView.setText(type);
                        }
                        else if (slot == 2) {
                            type2TextView.setText(type);
                        }
                    }
                    if (sharedPreferences.getBoolean(response.getString("name"), false)) {
                        catchPokemon();
                    } else {
                        releasePokemon();
                    }
                    JSONObject sprites = response.getJSONObject("sprites");
                    urlpic = sprites.getString("front_default");
                    new DownloadSpriteTask().execute(urlpic);

                    JSONObject species = response.getJSONObject("species");
                    urldes = species.getString("url");
                    JsonObjectRequest descriptionrequest = new JsonObjectRequest(Request.Method.GET, urldes, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONArray fte = response.getJSONArray("flavor_text_entries");
                                for (int i = 0; i < fte.length(); i++) {
                                    JSONObject Lang = (JSONObject) fte.get(i);
                                    String language = Lang.getJSONObject("language").getString("name");
                                    if (language.equals("en")) {
                                        description.setText(Lang.getString("flavor_text"));
                                    }
                                }

                            } catch (JSONException e) {
                                Log.e("cs50", "descriptionfaul", e);
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error){
                            Log.e("cs50", "Pokemondetailserror", error);
                        }
                    });
                    requestQueue.add(descriptionrequest);


                } catch (JSONException e) {
                    Log.e("cs50", "Pokemon json error", e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("cs50", "Pokemon details error", error);
            }
        });
        requestQueue.add(request);

    }

 private class DownloadSpriteTask extends AsyncTask<String, Void, Bitmap> {
     @Override
     protected Bitmap doInBackground(String... strings) {
         try {
             URL url = new URL(strings[0]);
             return BitmapFactory.decodeStream(url.openStream());
         } catch (IOException e) {
             Log.e("cs50", "Download sprite error", e);
             return null;
         }
     }

     @Override
     protected void onPostExecute(Bitmap bitmap) {
         sprite.setImageBitmap(bitmap);
     }
 }
}
