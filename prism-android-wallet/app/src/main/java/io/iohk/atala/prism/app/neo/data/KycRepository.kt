package io.iohk.atala.prism.app.neo.data

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import com.acuant.acuantcommon.model.Error
import com.acuant.acuantcommon.type.CardSide
import com.acuant.acuantdocumentprocessing.AcuantDocumentProcessor
import com.acuant.acuantdocumentprocessing.model.Classification
import com.acuant.acuantdocumentprocessing.model.DeleteType
import com.acuant.acuantdocumentprocessing.model.EvaluatedImageData
import com.acuant.acuantdocumentprocessing.model.IDResult
import com.acuant.acuantdocumentprocessing.model.IdOptions
import com.acuant.acuantdocumentprocessing.model.ProcessingResult
import com.acuant.acuantdocumentprocessing.service.listener.CreateInstanceListener
import com.acuant.acuantdocumentprocessing.service.listener.GetDataListener
import com.acuant.acuantdocumentprocessing.service.listener.UploadImageListener
import com.acuant.acuantimagepreparation.AcuantImagePreparation
import com.acuant.acuantimagepreparation.background.EvaluateImageListener
import com.acuant.acuantimagepreparation.model.AcuantImage
import com.acuant.acuantimagepreparation.model.CroppingData
import io.iohk.atala.prism.app.neo.common.FileUtils
import io.iohk.atala.prism.app.neo.data.local.KycLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class KycRepository(
    private val kycLocalDataSource: KycLocalDataSourceInterface,
    private val remoteDataSource: ConnectorRemoteDataSource,
    private val context: Context,
    sessionLocalDataSource: SessionLocalDataSourceInterface,
    preferencesLocalDataSource: PreferencesLocalDataSourceInterface
) : BaseRepository(sessionLocalDataSource, preferencesLocalDataSource) {

    // Due to the complexity of initiating the flow of Acuant, [KycInitializationHelper] was created
    private val kycInitializationHelper = KycInitializationHelper(kycLocalDataSource, remoteDataSource, sessionLocalDataSource, context)

    val kycInitializationStatus = kycInitializationHelper.status

    fun initializeAcuantProcess(): LiveData<KycInitializationHelper.KycInitializationResult?> = kycInitializationHelper.initializeAcuantProcess()

    // HANDLE DOCUMENTS SCAN PROCESS
    private val frontDocumentIdOptions = IdOptions().apply {
        authenticationSensitivity = IdOptions.AuthenticationSensitivity.Normal
        cardSide = CardSide.Front
    }
    private val backDocumentIdOptions = IdOptions().apply {
        authenticationSensitivity = IdOptions.AuthenticationSensitivity.Normal
        cardSide = CardSide.Back
    }

    private var frontDocumentResult: IDResult? = null

    private var backDocumentIDResult: IDResult? = null

    suspend fun startVerificationProcessWithDocumentFrontSide(croppedDocumentImageUrl: String, callBack: AcuantDocumentProcessCallBack) {
        frontDocumentResult = null
        handleDocument(croppedDocumentImageUrl, frontDocumentIdOptions, callBack)
    }

    suspend fun continueVerificationProcessWithDocumentBackSide(croppedDocumentImageUrl: String, callBack: AcuantDocumentProcessCallBack) {
        if (frontDocumentResult == null) {
            throw Exception("First the front of the document should be processed")
        }
        handleDocument(croppedDocumentImageUrl, backDocumentIdOptions, callBack)
    }

    /**
     * Step 1 get bitmap from the document image file (image obtained from [AcuantCameraActivity])
     * */
    private suspend fun handleDocument(croppedDocumentImageUrl: String, options: IdOptions, callBack: AcuantDocumentProcessCallBack) = withContext(Dispatchers.Main) {
        val imageFile = File(croppedDocumentImageUrl)
        FileUtils.decodeBitmapFromUri(context, imageFile.toUri())?.let {
            evaluateDocumentImage(it, options, callBack)
        }
    }

    /**
     * Step 2 Evaluate the document image
     * */
    private fun evaluateDocumentImage(bitmap: Bitmap, options: IdOptions, callBack: AcuantDocumentProcessCallBack) {
        val croppingData = CroppingData(bitmap)
        AcuantImagePreparation.evaluateImage(
            context,
            croppingData,
            object : EvaluateImageListener {
                override fun onError(error: Error) {
                    callBack.onError(error)
                }

                override fun onSuccess(image: AcuantImage) {
                    createDocumentProcessorInstance(image, options, callBack)
                }
            }
        )
    }

    /**
     * Step 3 Create Instance for document processing
     * */
    private fun createDocumentProcessorInstance(image: AcuantImage, options: IdOptions, callBack: AcuantDocumentProcessCallBack) {
        AcuantDocumentProcessor.createInstance(
            options,
            object : CreateInstanceListener {
                override fun instanceCreated(instanceId: String?, error: Error?) {
                    if (error != null || instanceId == null) {
                        callBack.onError(error)
                    } else {
                        processDocument(instanceId, image, options, callBack)
                    }
                }
            }
        )
    }

    /**
     * Step 4 processing the document image
     * */
    private fun processDocument(instanceId: String, image: AcuantImage, options: IdOptions, callBack: AcuantDocumentProcessCallBack) {
        AcuantDocumentProcessor.uploadImage(
            instanceId,
            EvaluatedImageData(image.rawBytes),
            options,
            object : UploadImageListener {
                override fun imageUploaded(error: Error?, classification: Classification?) {
                    if (error == null) {
                        AcuantDocumentProcessor.getData(
                            instanceId,
                            false,
                            object : GetDataListener {
                                override fun processingResultReceived(result: ProcessingResult?) {
                                    (result as? IDResult)?.let {
                                        handleProcessingResult(it, callBack)
                                    } ?: kotlin.run {
                                        // if it is not an IDResult it is not a valid result
                                        callBack.onError(result?.error)
                                    }
                                    // Remove AcuantDocumentProcessor instance
                                    AcuantDocumentProcessor.deleteInstance(instanceId, DeleteType.ID, null)
                                }
                            }
                        )
                    } else {
                        callBack.onError(error)
                    }
                }
            }
        )
    }

    private fun handleProcessingResult(result: IDResult, callBack: AcuantDocumentProcessCallBack) {
        result.error?.let {
            callBack.onError(it)
        } ?: kotlin.run {
            if (result.classification.presentationChanged) {
                // this means that the current document is not on the side (front/back) that was expected
                callBack.onError(null)
            } else if (result.classification.type.isGeneric) {
                // this means that the current document is not a valid identity document
                callBack.onError(null)
            } else if (frontDocumentResult?.isTheSameDocument(result) == false) {
                // this means that we already have the data from the front side and it does not match the data of the current document
                callBack.onError(null)
            } else {
                if (frontDocumentResult == null) {
                    frontDocumentResult = result
                    callBack.onSuccess(result, CardSide.Front)
                } else {
                    backDocumentIDResult = result
                    callBack.onSuccess(result, CardSide.Back)
                }
            }
        }
    }

    /**
     * A simple extension to compare [IDResult] objects
     * */
    private fun IDResult.isTheSameDocument(otherResult: IDResult): Boolean {
        return otherResult.classification.type.Class == classification.type.Class &&
            otherResult.biographic.age == biographic.age &&
            otherResult.biographic.birthDate == biographic.birthDate &&
            otherResult.biographic.expirationDate == biographic.expirationDate
    }

    interface AcuantDocumentProcessCallBack {
        fun onSuccess(result: IDResult, side: CardSide)
        fun onError(error: Error?)
    }
}
