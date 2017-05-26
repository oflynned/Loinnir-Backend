package com.syzible.loinnir.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.R;
import com.syzible.loinnir.fragments.portal.LocalityConversationFrag;
import com.syzible.loinnir.fragments.portal.ConversationsListFrag;
import com.syzible.loinnir.fragments.portal.MapFrag;
import com.syzible.loinnir.fragments.portal.PartnerConversationFrag;
import com.syzible.loinnir.fragments.portal.RouletteFrag;
import com.syzible.loinnir.location.LocationClient;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.GetJSONArray;
import com.syzible.loinnir.network.GetJSONObject;
import com.syzible.loinnir.network.NetworkCallback;
import com.syzible.loinnir.network.GetImage;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.objects.Message;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.services.NotificationUtils;
import com.syzible.loinnir.utils.BitmapUtils;
import com.syzible.loinnir.utils.DisplayUtils;
import com.syzible.loinnir.utils.EmojiUtils;
import com.syzible.loinnir.utils.FacebookUtils;
import com.syzible.loinnir.utils.LanguageUtils;
import com.syzible.loinnir.utils.LocalStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(0).setChecked(true);

        String name = LocalStorage.getPref(LocalStorage.Pref.name, this);
        name = name.split(" ")[0];
        DisplayUtils.generateSnackbar(this,
                "Fáilte romhat, a " + LanguageUtils.getVocative(name) + "! " +
                        EmojiUtils.getEmoji(EmojiUtils.HAPPY));

        // set up nav bar header for personalisation
        final View headerView = navigationView.getHeaderView(0);

        TextView userName = (TextView) headerView.findViewById(R.id.nav_header_name);
        userName.setText(LocalStorage.getPref(LocalStorage.Pref.name, this));

        JSONObject payload = new JSONObject();
        try {
            payload.put("fb_id", LocalStorage.getID(getApplicationContext()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RestClient.post(getApplicationContext(), Endpoints.GET_USER, payload, new BaseJsonHttpResponseHandler<JSONObject>() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                try {
                    double lat = response.getDouble("lat");
                    double lng = response.getDouble("lng");
                    String localityUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?radius=1&language=en&key=" +
                            getResources().getString(R.string.places_api_key) + "&location=" + lat + "," + lng;

                    new GetJSONObject(new NetworkCallback<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                TextView localityName = (TextView) headerView.findViewById(R.id.nav_header_locality);
                                String locality = response.getJSONArray("results").getJSONObject(0).getString("name");
                                localityName.setText(locality);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure() {

                        }
                    }, localityUrl, true).execute();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {

            }

            @Override
            protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                return new JSONObject(rawJsonData);
            }
        });

        final ImageView profilePic = (ImageView) headerView.findViewById(R.id.nav_header_pic);
        String picUrl = LocalStorage.getPref(LocalStorage.Pref.profile_pic, this);

        new GetImage(new NetworkCallback<Bitmap>() {
            @Override
            public void onResponse(Bitmap pic) {
                Bitmap croppedPic = BitmapUtils.getCroppedCircle(pic);
                profilePic.setImageBitmap(croppedPic);
            }

            @Override
            public void onFailure() {

            }
        }, picUrl, true).execute();

        // check for invocation by notification
        String invocationType = getIntent().getStringExtra("invoker");
        if (invocationType != null) {
            switch (invocationType) {
                case "notification":
                    String partnerId = getIntent().getStringExtra("user");
                    JSONObject chatPayload = new JSONObject();
                    try {
                        chatPayload.put("fb_id", partnerId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // get partner details and then open the chat fragment
                    // TODO poll messages automatically on opening from partner
                    RestClient.post(this, Endpoints.GET_USER, chatPayload, new BaseJsonHttpResponseHandler<JSONObject>() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                            try {
                                User partner = new User(response);
                                PartnerConversationFrag frag = new PartnerConversationFrag()
                                        .setPartner(partner);
                                MainActivity.clearBackstack(getFragmentManager());
                                MainActivity.setFragment(getFragmentManager(), frag);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {

                        }

                        @Override
                        protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                            return new JSONObject(rawJsonData);
                        }
                    });
            }
        } else {
            // TODO reset to MapFrag()
            setFragment(getFragmentManager(), new RouletteFrag());
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            // if there's only one fragment on the stack we should prevent the default
            // popping to ask for the user's permission to close the app
            if (getFragmentManager().getBackStackEntryCount() == 0) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("An Aip a Dhúnadh?")
                        .setMessage("Má bhrúitear an chnaipe \"Dún\", dúnfar an aip. An bhfuil tú cinnte go bhfuil sé seo ag teastáil uait a dhéanamh?")
                        .setPositiveButton("Dún", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MainActivity.this.finish();
                            }
                        })
                        .setNegativeButton("Ná dún", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_around_me) {
            clearBackstack(getFragmentManager());
            setFragment(getFragmentManager(), new MapFrag());
        } else if (id == R.id.nav_conversations) {
            clearBackstack(getFragmentManager());
            setFragment(getFragmentManager(), new ConversationsListFrag());
        } else if (id == R.id.nav_roulette) {
            clearBackstack(getFragmentManager());
            setFragment(getFragmentManager(), new RouletteFrag());
        } else if (id == R.id.nav_nearby) {
            clearBackstack(getFragmentManager());
            setFragment(getFragmentManager(), new LocalityConversationFrag());
        } else if (id == R.id.nav_rate) {

        } else if (id == R.id.nav_log_out) {
            FacebookUtils.deleteToken(this);
            finish();
            startActivity(new Intent(this, AuthenticationActivity.class));
        }

        // TODO DEV OPTIONS
        else if (id == R.id.force_post) {

            String localityUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?radius=1&language=en&key=" +
                    getResources().getString(R.string.places_api_key) + "&location=" +
                    LocationClient.GOOSEBERRY_HILL.latitude + "," + LocationClient.GOOSEBERRY_HILL.longitude;


            new GetJSONObject(new NetworkCallback<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        String locality = response.getJSONArray("results").getJSONObject(0).getString("name");

                        JSONObject payload = new JSONObject();
                        try {
                            // TODO poll from GPS
                            payload.put("fb_id", LocalStorage.getID(getApplicationContext()));
                            payload.put("lng", LocationClient.GOOSEBERRY_HILL.longitude);
                            payload.put("lat", LocationClient.GOOSEBERRY_HILL.latitude);
                            payload.put("locality", locality);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        RestClient.post(getApplicationContext(), Endpoints.UPDATE_USER_LOCATION, payload, new BaseJsonHttpResponseHandler<JSONObject>() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                System.out.println(response);
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                                System.out.println("Failure");
                            }

                            @Override
                            protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                                return new JSONObject(rawJsonData);
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure() {

                }
            }, localityUrl, true).execute();
        } else if (id == R.id.force_get) {
            JSONObject getUserPayload = new JSONObject();
            try {
                getUserPayload.put("fb_id", LocalStorage.getID(this));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            RestClient.post(this, Endpoints.GET_USER, getUserPayload, new BaseJsonHttpResponseHandler<JSONObject>() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                    try {
                        User user = new User(response);
                        Message message = new Message("0", user, System.currentTimeMillis(), "Dia dhuit! Conas atá tú? " + EmojiUtils.getEmoji(EmojiUtils.HAPPY));
                        NotificationUtils.generateMessageNotification(MainActivity.this, user, message);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {

                }

                @Override
                protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                    return new JSONObject(rawJsonData);
                }
            });
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public static void setFragment(FragmentManager fragmentManager, Fragment fragment) {
        fragmentManager.beginTransaction()
                .replace(R.id.portal_frame, fragment)
                .commit();
    }

    public static void setFragmentBackstack(FragmentManager fragmentManager, Fragment fragment) {
        fragmentManager.beginTransaction()
                .replace(R.id.portal_frame, fragment)
                .addToBackStack(fragment.getClass().getName())
                .commit();
    }

    public static void removeFragment(FragmentManager fragmentManager) {
        fragmentManager.popBackStack();
    }

    public static void clearBackstack(FragmentManager fragmentManager) {
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }
}
