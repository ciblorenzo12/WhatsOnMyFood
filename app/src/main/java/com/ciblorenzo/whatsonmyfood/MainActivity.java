package com.ciblorenzo.whatsonmyfood;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ciblorenzo.whatsonmyfood.ui.*;
import com.ciblorenzo.whatsonmyfood.retail.*;
import java.util.List;
import java.util.Random;

public class MainActivity extends BaseActivity {
    private RecentProductAdapter recentAdapter;
    private RetailerAlternativeAdapter alternatives;
    private ContentStateView swapsState;
    private ProductWithDetails latestProduct;
    private String loadedBarcode;
    private int requestGeneration;
    private RetailerRepository retailers;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        findViewById(R.id.action_scan).setOnClickListener(v -> openScanner(false));
        findViewById(R.id.action_ingredients).setOnClickListener(v -> openScanner(true));
        findViewById(R.id.action_pantry).setOnClickListener(v -> startActivity(new Intent(this,PantryActivity.class)));
        findViewById(R.id.action_database).setOnClickListener(v -> startActivity(new Intent(this,AdditiveDatabaseActivity.class)));
        findViewById(R.id.action_history).setOnClickListener(v -> {
            View heading=findViewById(R.id.history_heading);
            ((NestedScrollView)findViewById(R.id.home_scroll_view)).smoothScrollTo(0,heading.getTop());
            heading.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED);
        });
        recentAdapter=new RecentProductAdapter();
        RecyclerView history=findViewById(R.id.history_list);
        history.setLayoutManager(new LinearLayoutManager(this)); history.setAdapter(recentAdapter);
        alternatives=new RetailerAlternativeAdapter(this);
        RecyclerView swaps=findViewById(R.id.recommendations_recycler_view);
        swaps.setLayoutManager(new LinearLayoutManager(this,RecyclerView.HORIZONTAL,false)); swaps.setAdapter(alternatives);
        swapsState=findViewById(R.id.swaps_state);
        retailers=new RetailerRepository(getApplication());
        findViewById(R.id.view_all_swaps).setOnClickListener(v -> {
            if(latestProduct!=null)startActivity(MarketplaceNavigation.createIntent(this,latestProduct)); else openScanner(false);
        });
        setupDailyTip();
    }
    private void openScanner(boolean ingredients) {
        startActivity(new Intent(this,ScanBarcodeActivity.class).putExtra("ui_start_ingredients",ingredients));
    }
    private void setupDailyTip() {
        String[] tips=getResources().getStringArray(R.array.daily_health_tips);
        String[] sources=getResources().getStringArray(R.array.daily_health_tip_sources);
        int count=Math.min(tips.length,sources.length);
        if(count==0)return;
        int index=new Random().nextInt(count);
        ((TextView)findViewById(R.id.daily_tip_text)).setText(tips[index]);
        ((TextView)findViewById(R.id.daily_tip_source)).setText(getString(R.string.source_prefix,sources[index]));
    }
    @Override protected void onResume() {
        super.onResume();
        if(recentAdapter==null)return;
        List<Product> history=ScanHistory.read(this);
        recentAdapter.submit(history);
        ContentStateView historyState=findViewById(R.id.history_state);
        if(history.isEmpty())historyState.show(R.string.ui_history_empty,false); else historyState.setVisibility(View.GONE);
        if(history.isEmpty()){
            swapsState.show(R.string.ui_swaps_empty,false); swapsState.action(R.string.ui_scan_product,v->openScanner(false)); return;
        }
        Product product=history.get(0);
        if(product.barcode.equals(loadedBarcode))return;
        latestProduct=new ProductWithDetails(); latestProduct.product=product;
        loadAlternatives();
    }
    private void loadAlternatives() {
        if(latestProduct==null)return;
        loadedBarcode=latestProduct.product.barcode;
        int generation=++requestGeneration;
        swapsState.show(R.string.ui_loading,true);
        retailers.getAlternatives(latestProduct,new ProductRepository.RepositoryCallback<List<RetailerAlternative>>() {
            public void onComplete(List<RetailerAlternative> result) { runOnUiThread(()->{
                if(isFinishing()||isDestroyed()||generation!=requestGeneration)return;
                alternatives.submitList(result);
                if(result==null||result.isEmpty())swapsState.show(R.string.ui_swaps_empty,false);else swapsState.setVisibility(View.GONE);
            }); }
            public void onError(Exception error) { runOnUiThread(()->{
                if(isFinishing()||isDestroyed()||generation!=requestGeneration)return;
                loadedBarcode=null; swapsState.show(R.string.ui_error,false); swapsState.action(R.string.ui_retry,v->loadAlternatives());
            }); }
        });
    }
}