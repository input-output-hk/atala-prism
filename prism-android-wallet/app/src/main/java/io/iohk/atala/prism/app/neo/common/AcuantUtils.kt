package io.iohk.atala.prism.app.neo.common

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import com.acuant.acuantcommon.model.Credential
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley

class AcuantUtils {
    companion object {
        fun downloadImage(url: String, ctx: Context, responseListener: Response.Listener<Bitmap>, errorListener: Response.ErrorListener) {

            val imageRequest = object : ImageRequest(
                url,
                responseListener,
                0,
                0,
                ImageView.ScaleType.CENTER,
                Bitmap.Config.RGB_565,
                errorListener
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    val params = HashMap<String, String>()
                    val token = Credential.get().token
                    params["Authorization"] = "Bearer $token"
                    return params
                }
            }
            Volley.newRequestQueue(ctx).add(imageRequest)
        }
    }
}
