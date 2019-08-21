package pt.ulisboa.tecnico.cmu.locmess.utils;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.features.authentication.login.LoginActivity;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.activities.PostActivity;

public class NotificationSender {
    private static final int NOTIFICATION_ID = 123456;
    private static boolean notificationEnabled;
    public static NotificationManager notificationManager;

    public static void createNotification(Context context){
        disableNotification(context);                   // remove existent notification

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.mipmap.ic_lm_notification);
        builder.setContentTitle("New posts");
        builder.setContentText("Check nearby posts");

        Intent resultIntent = new Intent(context, LoginActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(LoginActivity.class);

        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
        notificationEnabled = true;
    }

    public static void disableNotification(Context context){
        if(notificationEnabled){
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATION_ID);
            notificationEnabled = false;
        }
    }
}
