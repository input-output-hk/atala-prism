package io.iohk.atala.prism.app.neo.common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;
import androidx.viewpager2.widget.ViewPager2;

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
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageInBytes, 0, imageInBytes.length);
            imageView.setImageBitmap(bitmap);
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
}