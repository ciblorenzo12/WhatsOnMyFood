package com.example.myapplication.retail;

import android.graphics.Color;

import androidx.annotation.DrawableRes;

import com.example.myapplication.R;

class RetailerBrandAssets {
    final int logoResId;
    final String logoUrl;
    final int brandColor;

    private RetailerBrandAssets(@DrawableRes int logoResId, String logoUrl, int brandColor) {
        this.logoResId = logoResId;
        this.logoUrl = logoUrl;
        this.brandColor = brandColor;
    }

    static RetailerBrandAssets resolve(String retailerName) {
        String name = retailerName == null ? "" : retailerName.toLowerCase();
        if (name.contains("walmart")) {
            return local(R.drawable.retail_logo_walmart, "#0071CE");
        }
        if (name.contains("target")) {
            return local(R.drawable.retail_logo_target, "#CC0000");
        }
        if (name.contains("amazon")) {
            return local(R.drawable.retail_logo_amazon, "#FF9900");
        }
        if (name.contains("kroger")) {
            return local(R.drawable.retail_logo_kroger, "#084999");
        }
        if (name.contains("instacart")) {
            return local(R.drawable.retail_logo_instacart, "#43B02A");
        }
        if (name.contains("whole foods")) {
            return remote("wholefoodsmarket.com", "#00674B");
        }
        if (name.contains("trader joe")) {
            return remote("traderjoes.com", "#D71920");
        }
        if (name.contains("sprouts")) {
            return remote("sprouts.com", "#5F8F22");
        }
        if (name.contains("costco")) {
            return remote("costco.com", "#005DAA");
        }
        if (name.contains("safeway")) {
            return remote("safeway.com", "#E1251B");
        }
        if (name.contains("publix")) {
            return remote("publix.com", "#007A3D");
        }
        if (name.contains("thrive")) {
            return remote("thrivemarket.com", "#00A651");
        }
        if (name.contains("aldi")) {
            return remote("aldi.us", "#004990");
        }
        if (name.contains("heb") || name.contains("h-e-b")) {
            return remote("heb.com", "#CC0000");
        }
        if (name.contains("meijer")) {
            return remote("meijer.com", "#0A4DA2");
        }
        if (name.contains("wegmans")) {
            return remote("wegmans.com", "#007A3D");
        }
        if (name.contains("albertsons")) {
            return remote("albertsons.com", "#00529B");
        }
        if (name.contains("food lion")) {
            return remote("foodlion.com", "#005BAC");
        }
        if (name.contains("giant")) {
            return remote("giantfood.com", "#D71920");
        }
        if (name.contains("stop & shop") || name.contains("stop and shop")) {
            return remote("stopandshop.com", "#5F259F");
        }
        if (name.contains("shoprite")) {
            return remote("shoprite.com", "#F58220");
        }
        if (name.contains("ralphs")) {
            return remote("ralphs.com", "#005DAA");
        }
        if (name.contains("vons")) {
            return remote("vons.com", "#00539B");
        }
        if (name.contains("harris teeter")) {
            return remote("harristeeter.com", "#006B54");
        }
        if (name.contains("fresh market")) {
            return remote("thefreshmarket.com", "#6A8F2A");
        }
        return local(R.drawable.ic_retail_brand_website, "#7C4DFF");
    }

    private static RetailerBrandAssets local(@DrawableRes int logoResId, String brandColor) {
        return new RetailerBrandAssets(logoResId, null, Color.parseColor(brandColor));
    }

    private static RetailerBrandAssets remote(String domain, String brandColor) {
        String logoUrl = "https://www.google.com/s2/favicons?sz=128&domain_url=https://" + domain;
        return new RetailerBrandAssets(R.drawable.ic_retail_brand_website, logoUrl, Color.parseColor(brandColor));
    }
}
