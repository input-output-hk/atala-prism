package io.iohk.atala.prism.app.neo.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import io.iohk.atala.prism.app.ui.QrCodeScannerActivity
import io.iohk.atala.prism.app.ui.UnlockActivity
import io.iohk.atala.prism.app.utils.IntentDataConstants
import java.io.File

class IntentUtils {
    companion object {
        fun intentQRCodeScanner(context: Context): Intent {
            val intent = Intent(context, QrCodeScannerActivity::class.java)
            intent.putExtra(IntentDataConstants.QR_SCANNER_MODE_KEY, IntentDataConstants.QR_SCANNER_MODE_QR_CODE)
            return intent
        }

        fun intentCamera(context: Context, photoFile: File): Intent {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.resolveActivity(context.packageManager)?.also {
                val photoURI: Uri = FileProvider.getUriForFile(context, "io.iohk.cvp.fileprovider", photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            }
            return takePictureIntent
        }

        fun intentGallery(): Intent {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            return Intent.createChooser(intent, null)
        }

        fun intentUnlockScreen(context: Context): Intent {
            val intent = Intent(context, UnlockActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            return intent
        }

        fun intentShareURL(url: String, chooserTitle: String): Intent {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_SUBJECT, chooserTitle)
            i.putExtra(Intent.EXTRA_TEXT, url)
            return Intent.createChooser(i, chooserTitle)
        }
    }
}
