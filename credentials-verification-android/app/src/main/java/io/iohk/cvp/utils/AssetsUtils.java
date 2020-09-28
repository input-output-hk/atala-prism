package io.iohk.cvp.utils;

import android.content.Context;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import io.iohk.cvp.core.exception.AssetNotFoundException;
import io.iohk.cvp.core.exception.ErrorCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AssetsUtils {

    private final Context context;

    public Optional<String> getTextFromAsset(final int assetResourceId) {
        final String assetName = context.getString(assetResourceId);
        try {
            InputStream stream = context.getAssets().open(assetName);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = stream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            byte[] byteArray = buffer.toByteArray();

            return Optional.of(new String(byteArray, StandardCharsets.UTF_8));
        } catch (IOException e) {
            FirebaseCrashlytics.getInstance().recordException(new AssetNotFoundException(
                    "Couldn't find the asset with the name " + assetName + ", exception:" + e
                            .getMessage(),
                    ErrorCode.ASSET_NOT_FOUND));
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
