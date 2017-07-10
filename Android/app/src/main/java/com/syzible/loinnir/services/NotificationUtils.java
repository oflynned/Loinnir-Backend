package com.syzible.loinnir.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;

import com.google.firebase.messaging.RemoteMessage;
import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.R;
import com.syzible.loinnir.activities.MainActivity;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.GetImage;
import com.syzible.loinnir.network.NetworkCallback;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.objects.Message;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.utils.BitmapUtils;
import com.syzible.loinnir.utils.EncodingUtils;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 22/05/2017.
 */

public class NotificationUtils {

    private static final int VIBRATION_INTENSITY = 500;

    private static void vibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(VIBRATION_INTENSITY);
    }

    public static void generateNotification(Context context, String title, String content) {
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context)
                        .setLargeIcon(icon)
                        .setSmallIcon(R.drawable.logo_small)
                        .setContentTitle(title)
                        .setContentText(content);

        Intent resultingIntent = new Intent(context, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultingIntent);

        PendingIntent resultingPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(resultingPendingIntent);

        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify(0, notificationBuilder.build());
        vibrate(context);
    }

    public static void generateMessageNotification(final Context context, final User user,
                                                   final Message message) throws JSONException {
        new GetImage(new NetworkCallback<Bitmap>() {
            @Override
            public void onResponse(Bitmap icon) {
                Bitmap circularIcon = BitmapUtils.getCroppedCircle(icon);

                NotificationCompat.Builder notificationBuilder =
                        new NotificationCompat.Builder(context)
                                .setLargeIcon(circularIcon)
                                .setSmallIcon(R.drawable.logo_small)
                                .setContentTitle(user.getName())
                                .setContentText(EncodingUtils.decodeText(message.getText()));

                // intent for opening partner conversation window
                Intent resultingIntent = new Intent(context, MainActivity.class);
                resultingIntent.putExtra("invoker", "notification");
                resultingIntent.putExtra("user", user.getId());

                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addParentStack(MainActivity.class);
                stackBuilder.addNextIntent(resultingIntent);

                PendingIntent resultingPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                notificationBuilder.setContentIntent(resultingPendingIntent);

                NotificationManager manager = (NotificationManager)
                        context.getSystemService(Context.NOTIFICATION_SERVICE);

                // TODO all facebook ids seem to be too big to be integers?
                // TODO crashes notification as it's greater than Integer.MAX_VALUE
                manager.notify(1, notificationBuilder.build());
                vibrate(context);
            }

            @Override
            public void onFailure() {

            }
        }, user.getAvatar(), true).execute();
    }
}
