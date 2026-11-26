package com.spendismart.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import androidx.core.app.NotificationCompat;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BankNotificationListener extends NotificationListenerService {

    // Package di Google Wallet e principali banche italiane (aggiungi il tuo se manca)
    private static final List<String> ALLOWED_PACKAGES = Arrays.asList(
            "com.google.android.apps.walletnfcrel",     // Google Wallet
            "com.revolut.revolut",                      // Revolut
            "com.latuabanca.intesasanpaolo",            // Intesa Sanpaolo
            "com.unicredit",                            // UniCredit
            "it.bnl.apps.banking",                      // BNL
            "it.posteitaliane.bancoposta",              // BancoPosta
            "it.fineco.mobile",                         // Fineco
            "posteitaliane.posteapp.apppostepay",       // Postepay
            "com.hype.app"                              // Hype
    );

    private static final String CHANNEL_ID = "spendismart_tx_channel";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        if (!ALLOWED_PACKAGES.contains(pkg)) return;

        Bundle extras = sbn.getNotification().extras;
        if (extras == null) return;

        String title = extras.getString(Notification.EXTRA_TITLE, "");
        CharSequence textSeq = extras.getCharSequence(Notification.EXTRA_TEXT);
        String text = textSeq != null ? textSeq.toString() : "";
        String fullContent = title + " " + text;

        // Regex per intercettare importi (es. 12,50 € | € 12.50 | EUR 12,50)
        Pattern amountPattern = Pattern.compile("(?:€|EUR)?\\s*(\\d+[.,]\\d{2})\\s*(?:€|EUR)?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = amountPattern.matcher(fullContent);

        if (matcher.find()) {
            String rawAmount = matcher.group(1).replace(",", ".");
            double amount = Double.parseDouble(rawAmount);

            // Esercente: su Google Wallet il titolo è quasi sempre il nome del negozio
            String merchant = !title.isEmpty() ? title : "Pagamento Carta";

            // Invia subito alla WebView se l'app è aperta
            BankPlugin.notifyTransaction(merchant, amount);

            // Mostra una notifica cliccabile per aprire l'app direttamente sulla modale
            showPromptNotification(merchant, amount);
        }
    }

    private void showPromptNotification(String merchant, double amount) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Nuove Spese Rilevate", NotificationManager.IMPORTANCE_HIGH
            );
            nm.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("tx_merchant", merchant);
        intent.putExtra("tx_amount", amount);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(String.format("Spesa di %.2f € da %s", amount, merchant))
                .setContentText("Tocca per annotare cosa hai comprato")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();

        nm.notify((int) System.currentTimeMillis(), notification);
    }
}