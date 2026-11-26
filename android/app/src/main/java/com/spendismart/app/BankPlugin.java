package com.spendismart.app;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "BankPlugin")
public class BankPlugin extends Plugin {
    private static BankPlugin instance;

    @Override
    public void load() {
        instance = this;
    }

    public static void notifyTransaction(String merchant, double amount) {
        if (instance != null) {
            JSObject ret = new JSObject();
            ret.put("merchant", merchant);
            ret.put("amount", amount);
            instance.notifyListeners("onBankTransaction", ret, true);
        }
    }
}