package co.realtime.plugins.android.cordovapush;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.fy.popuptest.MainActivity;
import com.fy.popuptest.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

import ibt.ortc.extensibility.GcmOrtcBroadcastReceiver;

public class GcmReceiver extends GcmOrtcBroadcastReceiver {

    private static final String TAG = "GcmReceiver";

    public GcmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Extract the payload from the message
        Bundle extras = intent.getExtras();
        if (extras != null) {
            // if we are in the foreground, just surface the payload, else post it to the statusbar
            if (OrtcPushPlugin.isInForeground()) {
                extras.putBoolean("foreground", true);
                OrtcPushPlugin.sendExtras(extras);
                //if (extras.getString("M") != null && extras.getString("M").length() != 0)
                    //showPopup(context, extras.getString("M").substring(13));
            } else {
                extras.putBoolean("foreground", false);

                // Send a notification if there is a message
                if (extras.getString("M") != null && extras.getString("M").length() != 0) {
                    createNotification(context, extras);
                    showPopup(context, extras.getString("M").substring(13));
                }
            }
        }
    }

    private void showPopup(final Context contextArg, final String messageArg)
    {
        Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                String messageStr = messageArg;
                String messageOutput = "";
                boolean isJSON = true;
                JSONObject jObject = null;

                try {
                    jObject = new JSONObject(messageStr);
                } catch (JSONException e) {
                    e.printStackTrace();
                    isJSON = false;
                }

                if(isJSON)
                {
                    try {
                        messageOutput = jObject.getString("message");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else
                {
                    messageOutput = messageStr;
                }

                LayoutInflater inflater = (LayoutInflater) contextArg.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View view = inflater.inflate( R.layout.dialog_view, null );
                TextView messageTextView = (TextView)view.findViewById(R.id.messageTextView);
                messageTextView.setText(messageOutput);

				final AlertDialog alertDialog = new AlertDialog.Builder(contextArg)
						.setView(view)
						.create();

                Button okButton = (Button)view.findViewById(R.id.okButton);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.cancel();
                        Intent mainActivityIntent = new Intent(contextArg, MainActivity.class);
                        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        contextArg.startActivity(mainActivityIntent);
                    }
                });

                Button cancelButton = (Button)view.findViewById(R.id.cancelButton);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.cancel();
                    }
                });

				alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
				alertDialog.show();
			}
		};
		
		mHandler.sendEmptyMessage(1);
    }

    public void createNotification(Context context, Bundle extras)
    {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String appName = getAppName(context);

        Intent notificationIntent = new Intent(context, OrtcPushHandlerActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtra("pushBundle", extras);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        int defaults = Notification.DEFAULT_ALL;

        if (extras.getString("defaults") != null) {
            try {
                defaults = Integer.parseInt(extras.getString("defaults"));
            } catch (NumberFormatException e) {}
        }

        String channel = extras.getString("C");
        String message = extras.getString("message");

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setDefaults(defaults)
                        .setSmallIcon(context.getApplicationInfo().icon)
                        .setWhen(System.currentTimeMillis())
                        .setContentTitle(context.getString(context.getApplicationInfo().labelRes))
                        .setContentIntent(contentIntent)
                        .setAutoCancel(true);


        if (message != null) {
            mBuilder.setContentText(message);
        } else {
            mBuilder.setContentText("<missing message content>");
        }

        int notId = 0;

        try {
            notId = new Random().nextInt();
        }
        catch(NumberFormatException e) {
            Log.e(TAG, "Number format exception - Error parsing Notification ID: " + e.getMessage());
        }
        catch(Exception e) {
            Log.e(TAG, "Number format exception - Error parsing Notification ID" + e.getMessage());
        }

        mNotificationManager.notify(appName, notId, mBuilder.build());
    }

    private static String getAppName(Context context)
    {
        CharSequence appName =
                context
                        .getPackageManager()
                        .getApplicationLabel(context.getApplicationInfo());

        return (String)appName;
    }
}
