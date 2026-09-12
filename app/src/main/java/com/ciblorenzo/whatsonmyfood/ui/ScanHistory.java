package com.ciblorenzo.whatsonmyfood.ui;
import android.content.Context;
import com.ciblorenzo.whatsonmyfood.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

/** A small device-local recent-scan presentation cache, scoped to the signed-in account. */
public final class ScanHistory {
    private ScanHistory() {}
    private static String key() { return FirebaseAuth.getInstance().getCurrentUser()==null ? "guest" : FirebaseAuth.getInstance().getCurrentUser().getUid(); }
    public static List<Product> read(Context context) {
        try {
            List<Product> list=new Gson().fromJson(context.getSharedPreferences("ui_scan_history",0).getString(key(),"[]"),new TypeToken<List<Product>>(){}.getType());
            return list==null?new ArrayList<>():list;
        } catch (RuntimeException error) { return new ArrayList<>(); }
    }
    public static void record(Context context, Product product) {
        if (product==null || !product.isValid()) return;
        List<Product> list=read(context);
        list.removeIf(item -> item==null || product.barcode.equals(item.barcode));
        list.add(0,product);
        if (list.size()>20) list=new ArrayList<>(list.subList(0,20));
        context.getSharedPreferences("ui_scan_history",0).edit().putString(key(),new Gson().toJson(list)).apply();
    }
}
