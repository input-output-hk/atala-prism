package io.iohk.atala.prism.app.neo.common;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.card.MaterialCardView;

public class DataBindingAdapters {

    /**
     * [View] Bindings Adapters
     */

    @BindingAdapter("iohk:visible")
    public static void visible(View view, Boolean show) {
        if (show) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    /**
     * [ViewPager2] Bindings Adapters
     */

    @BindingAdapter("iohk:currentItem")
    public static void setItem(ViewPager2 pager, int item) {
        pager.setCurrentItem(item);
    }

    @InverseBindingAdapter(attribute = "iohk:currentItem")
    public static int getItem(ViewPager2 pager) {
        return pager.getCurrentItem();
    }

    @BindingAdapter("iohk:currentItemAttrChanged")
    public static void setListeners(ViewPager2 pager, InverseBindingListener attrChange) {
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                attrChange.onChange();
            }
        });
    }

    /**
     * [ImageView] Bindings Adapters
     */

    @BindingAdapter({"iohk:imageInBytes", "iohk:failImage"})
    public static void image(ImageView imageView, byte[] imageInBytes, Drawable failImage) {
        try {
            if (imageInBytes.length == 0) {
                imageView.setImageDrawable(failImage);
            } else {
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageInBytes, 0, imageInBytes.length);
                imageView.setImageBitmap(bitmap);
            }
        } catch (Exception ex) {
            imageView.setImageDrawable(failImage);
            Log.e("iohk:imageInBytes", ex.getLocalizedMessage());
        }
    }

    @BindingAdapter({"iohk:bitmap", "iohk:failImage"})
    public static void image(ImageView imageView, Bitmap bitmap, Drawable failImage) {
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageDrawable(failImage);
        }
    }

    @BindingAdapter("tint")
    public static void tint(ImageView imageView, Integer tintResource) {
        if(tintResource != null){
            int color;
            try{
                color = ContextCompat.getColor(imageView.getContext(), tintResource);
            }catch (Resources.NotFoundException e){
                color = tintResource;
            }
            ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(color));
        }
    }

    @BindingAdapter("strokeWidth")
    public static void strokeWidth(MaterialCardView cardView, Float width) {
        cardView.setStrokeWidth(width.intValue());
    }

    /**
     * [TextView] Bindings Adapters
     */

    @BindingAdapter("htmlText")
    public static void htmlText(TextView textView, String htmlString){
        Spanned text = Html.fromHtml(htmlString,Html.FROM_HTML_MODE_LEGACY);
        textView.setText(text);
    }
}