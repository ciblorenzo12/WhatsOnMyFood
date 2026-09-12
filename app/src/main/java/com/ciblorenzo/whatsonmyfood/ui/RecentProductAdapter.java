package com.ciblorenzo.whatsonmyfood.ui;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import com.ciblorenzo.whatsonmyfood.*;
import com.squareup.picasso.Picasso;
import java.util.*;

public class RecentProductAdapter extends RecyclerView.Adapter<RecentProductAdapter.Holder> {
    private final List<Product> items=new ArrayList<>();
    public void submit(List<Product> value) { items.clear(); if(value!=null)items.addAll(value); notifyDataSetChanged(); }
    public int getItemCount(){return items.size();}
    public Holder onCreateViewHolder(ViewGroup parent,int type){return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.ui_recent_product,parent,false));}
    public void onBindViewHolder(Holder holder,int position){
        Product p=items.get(position); holder.name.setText(p.productName); holder.brand.setText(p.brands);
        holder.score.setText(p.healthScore==null?holder.itemView.getContext().getString(R.string.ui_score_unknown):holder.itemView.getContext().getString(R.string.ui_score_out_of,p.healthScore));
        if(p.imageUrl!=null&&!p.imageUrl.isEmpty())Picasso.get().load(p.imageUrl).placeholder(R.drawable.ic_scan).error(R.drawable.ic_scan).fit().centerInside().into(holder.image); else holder.image.setImageResource(R.drawable.ic_scan);
        holder.itemView.setOnClickListener(v->v.getContext().startActivity(PantryNavigation.productDetailsIntent(v.getContext(),p)));
        holder.itemView.setContentDescription(holder.itemView.getContext().getString(R.string.open_product_details,p.productName));
    }
    static class Holder extends RecyclerView.ViewHolder {
        TextView name,brand,score; ImageView image;
        Holder(View v){super(v);name=v.findViewById(R.id.ui_recent_name);brand=v.findViewById(R.id.ui_recent_brand);score=v.findViewById(R.id.ui_recent_score);image=v.findViewById(R.id.ui_recent_image);}
    }
}
