package com.syzible.loinnir.fragments.portal;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.messages.MessageHolders;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;
import com.syzible.loinnir.R;
import com.syzible.loinnir.activities.MainActivity;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.GetImage;
import com.syzible.loinnir.network.NetworkCallback;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.objects.Message;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.services.CachingUtil;
import com.syzible.loinnir.utils.BitmapUtils;
import com.syzible.loinnir.utils.BroadcastFilters;
import com.syzible.loinnir.utils.DisplayUtils;
import com.syzible.loinnir.utils.EncodingUtils;
import com.syzible.loinnir.utils.JSONUtils;
import com.syzible.loinnir.utils.LanguageUtils;
import com.syzible.loinnir.utils.LocalStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 07/05/2017.
 */

public class PartnerConversationFrag extends Fragment {

    private Date lastLoadedDate;
    private int loadedCount;

    private User partner;
    private View view;

    private ArrayList<Message> messages = new ArrayList<>();
    private MessagesListAdapter<Message> adapter;
    private BroadcastReceiver newPartnerMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BroadcastFilters.new_partner_message.toString())) {
                String partnerId = intent.getStringExtra("partner_id");

                RestClient.post(getActivity(), Endpoints.GET_PARTNER_MESSAGES,
                        JSONUtils.getPartnerInteractionPayload(partnerId, getActivity()),
                        new BaseJsonHttpResponseHandler<JSONArray>() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                                try {
                                    JSONObject latestPayload = response.getJSONObject(response.length() - 1);
                                    User sender = new User(latestPayload.getJSONObject("user"));
                                    Message message = new Message(sender, latestPayload.getJSONObject("message"));
                                    adapter.addToStart(message, true);

                                    System.out.println(sender.getName() + " has sent you a new message");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONArray errorResponse) {
                                System.out.println(rawJsonData);
                            }

                            @Override
                            protected JSONArray parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                                return new JSONArray(rawJsonData);
                            }
                        });
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.conversation_frag, container, false);
        setupAdapter(view);

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(partner.getName());
        ((AppCompatActivity) getActivity()).getSupportActionBar()
                .setSubtitle(partner.getLocality() + ", " + partner.getCounty());

        return view;
    }

    @Override
    public void onResume() {
        loadMessages();
        getActivity().registerReceiver(newPartnerMessageReceiver,
                new IntentFilter(BroadcastFilters.new_partner_message.toString()));
        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(newPartnerMessageReceiver);
        super.onPause();
    }


    private void setupAdapter(View view) {
        MessageHolders holdersConfig = new MessageHolders();
        holdersConfig.setIncomingTextConfig(IncomingMessage.class, R.layout.chat_message_layout);

        adapter = new MessagesListAdapter<>(LocalStorage.getID(getActivity()), holdersConfig, loadImage());
        adapter.setOnMessageViewLongClickListener(new MessagesListAdapter.OnMessageViewLongClickListener<Message>() {
            @Override
            public void onMessageViewLongClick(View view, final Message message) {
                // should not be able to block yourself
                if (!message.getUser().getId().equals(LocalStorage.getID(getActivity())))
                    DisplayUtils.generateBlockDialog(getActivity(), (User) message.getUser(), new DisplayUtils.OnCallback() {
                        @Override
                        public void onCallback() {
                            DisplayUtils.generateSnackbar(getActivity(), "Cuireadh cosc go rathúil ar " + LanguageUtils.lenite(((User) message.getUser()).getForename()));
                            loadMessages();
                        }
                    });
            }
        });
        MessagesList messagesList = (MessagesList) view.findViewById(R.id.messages_list);
        messagesList.setAdapter(adapter);

        MessageInput messageInput = (MessageInput) view.findViewById(R.id.message_input);
        messageInput.setInputListener(new MessageInput.InputListener() {
            @Override
            public boolean onSubmit(final CharSequence input) {
                RestClient.post(getActivity(), Endpoints.GET_USER, JSONUtils.getIdPayload(getActivity()),
                        new BaseJsonHttpResponseHandler<JSONObject>() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                try {
                                    final User me = new User(response);

                                    JSONObject payload = new JSONObject();
                                    payload.put("my_id", me.getId());
                                    payload.put("partner_id", partner.getId());

                                    RestClient.post(getActivity(), Endpoints.GET_PARTNER_MESSAGES_COUNT, payload, new BaseJsonHttpResponseHandler<JSONObject>() {
                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                            try {
                                                if (response.getInt("count") == 0)
                                                    matchPartner(partner);

                                                String messageContent = input.toString();

                                                Message message = new Message(LocalStorage.getID(getActivity()), me, System.currentTimeMillis(), messageContent);
                                                adapter.addToStart(message, true);

                                                // send to server
                                                JSONObject messagePayload = new JSONObject();
                                                messagePayload.put("from_id", LocalStorage.getID(getActivity()));
                                                messagePayload.put("to_id", partner.getId());
                                                messagePayload.put("message", EncodingUtils.encodeText(message.getText()));

                                                RestClient.post(getActivity(), Endpoints.SEND_PARTNER_MESSAGE, messagePayload, new BaseJsonHttpResponseHandler<JSONObject>() {
                                                    @Override
                                                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                                        System.out.println(response);
                                                    }

                                                    @Override
                                                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                                                        System.out.println(rawJsonData);
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
                                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                                            System.out.println(rawJsonData);
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
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {

                            }

                            @Override
                            protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                                return new JSONObject(rawJsonData);
                            }
                        });

                return true;
            }
        });
    }

    private void loadMessages() {
        setupAdapter(view);
        RestClient.post(getActivity(), Endpoints.GET_PARTNER_MESSAGES,
                JSONUtils.getPartnerInteractionPayload(partner, getActivity()),
                new BaseJsonHttpResponseHandler<JSONArray>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                        messages.clear();
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject data = response.getJSONObject(i);
                                JSONObject dataMessage = data.getJSONObject("message");
                                User sender = new User(data.getJSONObject("user"));
                                Message message = new Message(sender, dataMessage);
                                messages.add(message);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        adapter.addToEnd(messages, true);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONArray errorResponse) {
                        System.out.println("failed?");
                    }

                    @Override
                    protected JSONArray parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                        return new JSONArray(rawJsonData);
                    }
                });

        // also demarcate any messages seen up to this point
        RestClient.post(getActivity(), Endpoints.MARK_SEEN,
                JSONUtils.getPartnerInteractionPayload(partner, getActivity()),
                new BaseJsonHttpResponseHandler<JSONObject>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {

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

    public PartnerConversationFrag setPartner(User partner) {
        this.partner = partner;
        return this;
    }

    private ImageLoader loadImage() {
        return new ImageLoader() {
            @Override
            public void loadImage(final ImageView imageView, final String url) {
                // can only use Facebook to sign up so use the embedded id in the url
                final String fileName = url.split("/")[3];

                if (!CachingUtil.doesImageExist(getActivity(), fileName)) {
                    new GetImage(new NetworkCallback<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap response) {
                            Bitmap croppedImage = BitmapUtils.getCroppedCircle(response);
                            Bitmap scaledAvatar = BitmapUtils.scaleBitmap(croppedImage, BitmapUtils.BITMAP_SIZE_SMALL);
                            imageView.setImageBitmap(scaledAvatar);
                            CachingUtil.cacheImage(getActivity(), fileName, scaledAvatar);
                        }

                        @Override
                        public void onFailure() {
                            System.out.println("dl failure on chat pic");
                        }
                    }, url, true).execute();
                } else {
                    Bitmap cachedImage = CachingUtil.getCachedImage(getActivity(), fileName);
                    imageView.setImageBitmap(cachedImage);
                }
            }
        };
    }

    // TODO what is this for??? Unread messages??? Pagination???
    private int getMessageCount(User me, User partner) throws JSONException {
        RestClient.post(getActivity(), Endpoints.GET_PARTNER_MESSAGES_COUNT,
                JSONUtils.getPartnerInteractionPayload(partner, getActivity()),
                new BaseJsonHttpResponseHandler<JSONObject>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                        try {
                            int count = response.getInt("count");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {

                    }

                    @Override
                    protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                        return null;
                    }
                });

        // TODO make an interface for extracting these posts into one nice method to call with
        // TODO a payload and an endpoint for returning at a later point in time
        return -1;
    }

    private void matchPartner(User partner) {
        RestClient.post(getActivity(), Endpoints.SUBSCRIBE_TO_PARTNER,
                JSONUtils.getPartnerInteractionPayload(partner, getActivity()),
                new BaseJsonHttpResponseHandler<JSONObject>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                        System.out.println(rawJsonResponse);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                        System.out.println(rawJsonData);
                    }

                    @Override
                    protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                        return new JSONObject(rawJsonData);
                    }
                });
    }
}
