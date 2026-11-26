package com.spendismart.app;

import android.content.Intent;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(BankPlugin.class);
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("tx_merchant")) {
            String merchant = intent.getStringExtra("tx_merchant");
            double amount = intent.getDoubleExtra("tx_amount", 0.0);
            BankPlugin.notifyTransaction(merchant, amount);
            intent.removeExtra("tx_merchant");
        }
    }
}