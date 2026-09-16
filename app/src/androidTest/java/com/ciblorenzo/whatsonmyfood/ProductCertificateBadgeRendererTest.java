package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ProductCertificateBadgeRendererTest {
    @Test public void bothScreensRenderOfficialArtworkAndPlainClaimsInBothThemes() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
            for (int mode : new int[]{Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
                Configuration config = new Configuration(base.getResources().getConfiguration());
                config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | mode;
                Context context = new ContextThemeWrapper(base.createConfigurationContext(config),
                        R.style.Theme_MyApplication);
                for (int layout : new int[]{R.layout.fragment_product_details, R.layout.activity_product_details}) {
                    View root = LayoutInflater.from(context).inflate(layout, null);
                    HorizontalScrollView scroll = root.findViewById(R.id.certificate_badges_scroll_view);
                    LinearLayout badges = root.findViewById(R.id.certificate_badges_container);
                    ProductCertificateBadgeRenderer.bind(context, null, null, scroll, badges,
                            "USDA Organic, Non-GMO Project Verified, Orthodox Union Kosher, GFCO, Vegan Action, Vegetarian");
                    assertEquals(6, badges.getChildCount());
                    for (int i = 0; i < 5; i++) {
                        assertTrue(badges.getChildAt(i) instanceof ImageView);
                        ImageView badge = (ImageView) badges.getChildAt(i);
                        assertNotNull(badge.getDrawable());
                        assertEquals(ImageView.ScaleType.FIT_CENTER, badge.getScaleType());
                        assertNotNull(badge.getContentDescription());
                    }
                    assertDietaryBadge(badges.getChildAt(5), "Vegetarian");

                    savePreview(context, badges, layout + "-" + mode);
                    ProductCertificateBadgeRenderer.bind(context, null, null, scroll, badges,
                            "Vegan, No gluten, Vegetarian, Kosher");
                    assertEquals(4, badges.getChildCount());
                    assertDietaryBadge(badges.getChildAt(0), "Vegan"); assertDietaryBadge(badges.getChildAt(1), "Gluten-Free"); assertDietaryBadge(badges.getChildAt(2), "Vegetarian"); assertTrue(badges.getChildAt(3) instanceof TextView); savePreview(context, badges, "dietary-" + layout + "-" + mode);
                    ProductCertificateBadgeRenderer.bind(context, null, null, scroll, badges, null);
                    assertEquals(0, badges.getChildCount());
                    assertEquals(View.GONE, scroll.getVisibility());
                }
            }
        });
    }

    private static void assertDietaryBadge(View view, String title) {
        assertTrue(view instanceof LinearLayout);
        LinearLayout badge = (LinearLayout) view;
        TextView label = (TextView) badge.getChildAt(0);
        assertEquals(title, label.getText().toString());
        assertNotNull(label.getCompoundDrawables()[1]);
        assertEquals("Not verified", ((TextView) badge.getChildAt(1)).getText().toString());
        assertTrue(badge.getContentDescription().toString().contains("Not verified"));
    }

    private static void savePreview(Context context, LinearLayout badges, String name) {
        int height = Math.round(92 * context.getResources().getDisplayMetrics().density);
        badges.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        badges.layout(0, 0, badges.getMeasuredWidth(), height);
        for (int i = 0; i < badges.getChildCount(); i++) {
            View child = badges.getChildAt(i);
            assertTrue(child.getMeasuredWidth() > 0);
            assertTrue(child.getBottom() <= height);
        }
        Bitmap bitmap = Bitmap.createBitmap(badges.getWidth(), height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(context.getColor(R.color.card_background));
        badges.draw(canvas);
        File directory = new File(context.getExternalFilesDir(null), "m9-09");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (FileOutputStream output = new FileOutputStream(new File(directory, name + ".png"))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        } catch (IOException error) {
            throw new AssertionError(error);
        } finally {
            bitmap.recycle();
        }
    }
}
