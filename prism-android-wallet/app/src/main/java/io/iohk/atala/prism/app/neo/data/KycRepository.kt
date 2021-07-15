package io.iohk.atala.prism.app.neo.data

import android.content.Context
import android.graphics.Bitmap
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
import com.acuant.acuantdocumentprocessing.service.listener.DeleteListener
import com.acuant.acuantdocumentprocessing.service.listener.GetDataListener
import com.acuant.acuantdocumentprocessing.service.listener.UploadImageListener
import com.acuant.acuantfacematchsdk.AcuantFaceMatch
import com.acuant.acuantfacematchsdk.model.FacialMatchData
import com.acuant.acuantimagepreparation.AcuantImagePreparation
import com.acuant.acuantimagepreparation.background.EvaluateImageListener
import com.acuant.acuantimagepreparation.model.AcuantImage
import com.acuant.acuantimagepreparation.model.CroppingData
import com.acuant.acuantpassiveliveness.AcuantPassiveLiveness
import com.acuant.acuantpassiveliveness.model.PassiveLivenessData
import com.acuant.acuantpassiveliveness.model.PassiveLivenessResult
import com.acuant.acuantpassiveliveness.service.PassiveLivenessListener
import io.iohk.atala.prism.app.data.local.preferences.models.AcuantUserInfo
import io.iohk.atala.prism.app.neo.common.AcuantUtils
import io.iohk.atala.prism.app.neo.common.extensions.documentIssueDate
import io.iohk.atala.prism.app.neo.common.extensions.documentNumber
import io.iohk.atala.prism.app.neo.common.extensions.isBackSideRequired
import io.iohk.atala.prism.app.neo.data.local.KycLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class KycRepository(
    private val kycLocalDataSource: KycLocalDataSourceInterface,
    private val remoteDataSource: ConnectorRemoteDataSource,
    private val context: Context,
    sessionLocalDataSource: SessionLocalDataSourceInterface,
    preferencesLocalDataSource: PreferencesLocalDataSourceInterface
) : BaseRepository(sessionLocalDataSource, preferencesLocalDataSource) {

    sealed class KycRepositoryError {
        data class AcuantError(val message: String?) : KycRepositoryError() // any error thrown by Acuant's sdk
        object FaceMatchError : KycRepositoryError() // Error when comparing the user's selfie with the photo obtained from the document
        object UnexpectedDocumentPresentation : KycRepositoryError() // when taking a photo of the document from the wrong side
        object ConnectionError : KycRepositoryError()
        object DocumentPhotoNotFound : KycRepositoryError()
        object UknownError : KycRepositoryError()
    }

    private val repositoryScope = CoroutineScope(Job() + Dispatchers.Main)

    // Due to the complexity of initiating the flow of Acuant, [KycInitializationHelper] was created
    private val kycInitializationHelper = KycInitializationHelper(kycLocalDataSource, remoteDataSource, sessionLocalDataSource, context)

    val kycInitializationStatus = kycInitializationHelper.status

    fun initializeAcuantProcess(): LiveData<KycInitializationHelper.KycInitializationResult?> = kycInitializationHelper.initializeAcuantProcess()

    // HANDLE DOCUMENTS SCAN PROCESS
    private val frontDocumentIdOptions = IdOptions().apply {
        authenticationSensitivity = IdOptions.AuthenticationSensitivity.Normal
        cardSide = CardSide.Front
        isHealthCard = false
        isRetrying = false
    }
    private val backDocumentIdOptions = IdOptions().apply {
        authenticationSensitivity = IdOptions.AuthenticationSensitivity.Normal
        cardSide = CardSide.Back
        isHealthCard = false
        isRetrying = false
    }

    private var documentFaceImage: Bitmap? = null

    private var selfieImage: Bitmap? = null

    private var frontDocument: AcuantImage? = null

    private var backDocument: AcuantImage? = null

    private var idResult: IDResult? = null

    private var barcodeString: String? = null

    private var instanceId: String? = null

    private fun clearData() {
        frontDocument?.destroy()
        frontDocument = null
        backDocument?.destroy()
        backDocument = null
        selfieImage?.recycle()
        selfieImage = null
        documentFaceImage?.recycle()
        documentFaceImage = null
    }

    fun restartProcess() {
        clearData()
        instanceId?.let {
            AcuantDocumentProcessor.deleteInstance(instanceId, DeleteType.ID, null)
        }
    }

    /**
     * Step 1 get bitmap and barcodeString from the document image file (image and barcodeString obtained from [com.acuant.acuantcamera.camera.AcuantCameraActivity])
     * */
    fun handleCapturedDocument(croppedDocumentImage: Bitmap, barcodeString: String?, callBack: AcuantDocumentProcessCallBack) {
        this.barcodeString = barcodeString
        val currentOptions = if (frontDocument == null) frontDocumentIdOptions else backDocumentIdOptions
        evaluateDocument(croppedDocumentImage, currentOptions, callBack)
    }

    /**
     * Step 2 Evaluate the document imagen, basically it is validated that it is an image with a minimum quality and that this image has a document.
     * */
    private fun evaluateDocument(bitmap: Bitmap, options: IdOptions, callBack: AcuantDocumentProcessCallBack) {
        val croppingData = CroppingData(bitmap)
        AcuantImagePreparation.evaluateImage(
            context,
            croppingData,
            object : EvaluateImageListener {
                override fun onError(error: Error) {
                    callBack.onError(KycRepositoryError.AcuantError(error.errorDescription))
                }
                override fun onSuccess(image: AcuantImage) {
                    if (options.cardSide == CardSide.Front) {
                        createDocumentProcessorInstance(image, options, callBack)
                    } else {
                        uploadDocument(image, options, callBack)
                    }
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
                        callBack.onError(KycRepositoryError.AcuantError(error?.errorDescription))
                    } else {
                        this@KycRepository.instanceId = instanceId
                        uploadDocument(image, options, callBack)
                    }
                }
            }
        )
    }

    /**
     * Step 4 Upload the image to the created instance
     * */
    private fun uploadDocument(image: AcuantImage, options: IdOptions, callBack: AcuantDocumentProcessCallBack) {
        // Barcode is needed only for the back of the document (apparently)
        val data = EvaluatedImageData(image.rawBytes, if (options.cardSide == CardSide.Front) null else barcodeString)
        AcuantDocumentProcessor.uploadImage(
            instanceId,
            data,
            options,
            object : UploadImageListener {
                override fun imageUploaded(error: Error?, classification: Classification?) {
                    if (error != null) {
                        removeInstance(KycRepositoryError.AcuantError(error.errorDescription), callBack)
                        return
                    }
                    /*
                    * if classification.presentationChanged == true, this means that the current document is not on the side (front/back) that was expected
                    * then should add an "if" statement to validate this but unfortunately in some cases the Acuant SDK does not guarantee this data.
                    * */
                    if (classification?.presentationChanged == true) {
                        removeInstance(KycRepositoryError.UnexpectedDocumentPresentation, callBack)
                        return
                    }
                    when (options.cardSide) {
                        CardSide.Front -> {
                            frontDocument = image
                            if (classification?.isBackSideRequired() == true) {
                                // if a result is not sent, it means that only the front of the document has been uploaded and that the back is required
                                callBack.onSuccess()
                            } else {
                                obtainResultData(callBack)
                            }
                        }
                        CardSide.Back -> {
                            backDocument = image
                            obtainResultData(callBack)
                        }
                    }
                }
            }
        )
    }

    /**
     * Step 5 Start a request to obtain the identity results, This request can only be made if the photos of the document have
     * already been uploaded (in case both sides of the document are required, both sides will have to be uploaded)
     * */
    private fun obtainResultData(callBack: AcuantDocumentProcessCallBack) {
        AcuantDocumentProcessor.getData(
            instanceId,
            false,
            object : GetDataListener {
                override fun processingResultReceived(result: ProcessingResult?) {
                    (result as? IDResult)?.let { idResult ->
                        idResult.error?.let {
                            removeInstance(KycRepositoryError.AcuantError(it.errorDescription), callBack)
                        } ?: tryToDownloadDocumentPhoto(idResult, callBack)
                    } ?: removeInstance( // if it is not an IDResult it is not a valid result
                        KycRepositoryError.AcuantError(result?.error?.errorDescription),
                        callBack
                    )
                }
            }
        )
    }

    /**
     * Step 6 look if the document has an image of the persons face, if so, download that image
     * this photo will be used to compare it with the selfie taken by the user and check if
     * there is a facial match
     * */
    private fun tryToDownloadDocumentPhoto(result: IDResult, callBack: AcuantDocumentProcessCallBack) {
        if (result.biographic.photo != null && !result.biographic.photo.equals("null", ignoreCase = true)) {
            AcuantUtils.downloadImage(
                result.biographic.photo,
                context,
                responseListener = {
                    // There is a photo in the document and it has been downloaded successfully
                    documentFaceImage = it
                    idResult = result
                    callBack.onSuccess(identityDataFound = true)
                },
                errorListener = {
                    // There is a photo in the document but it could not be downloaded
                    removeInstance(KycRepositoryError.ConnectionError, callBack)
                }
            )
        } else {
            // There is no photo in the document
            removeInstance(KycRepositoryError.DocumentPhotoNotFound, callBack)
        }
    }

    private fun removeInstance(error: KycRepositoryError, callBack: AcuantDocumentProcessCallBack) {
        instanceId?.let {
            AcuantDocumentProcessor.deleteInstance(
                it,
                DeleteType.ID,
                object : DeleteListener {
                    override fun instanceDeleted(success: Boolean) {
                        instanceId = null
                        clearData()
                        callBack.onError(error, processIsRestarted = true)
                    }
                }
            )
        }
    }

    interface AcuantDocumentProcessCallBack {
        fun onSuccess(identityDataFound: Boolean = false, faceMatchFound: Boolean = false)
        fun onError(error: KycRepositoryError, processIsRestarted: Boolean = false)
    }

    /*
    * FACE CAPTURE PROCESS (USER´S SELFIE)
    * */

    /**
     * STEP 1. Get the bitmap of the selfie taken by the user using [com.acuant.acuantfacecapture.FaceCaptureActivity]
     * */
    fun handleSelfieImage(selfieImage: Bitmap, callBack: AcuantDocumentProcessCallBack) {
        if (frontDocument == null || backDocument == null || idResult == null) {
            throw Exception("First it is necessary to have the data of a document (front and back side)")
        }
        this.selfieImage = selfieImage
        passiveLivenessProcess(callBack)
    }

    /**
     * Step 2. Determine liveness from a single selfie image
     * */
    private fun passiveLivenessProcess(callBack: AcuantDocumentProcessCallBack) {
        val plData = PassiveLivenessData(selfieImage!!)
        AcuantPassiveLiveness.processFaceLiveness(
            plData,
            object : PassiveLivenessListener {
                override fun passiveLivenessFinished(result: PassiveLivenessResult) {
                    if (result.livenessAssessment == AcuantPassiveLiveness.LivenessAssessment.Live) {
                        // Is a living person
                        faceMatch(callBack)
                    } else {
                        // Is not a living person
                        callBack.onError(KycRepositoryError.FaceMatchError, processIsRestarted = false)
                    }
                }
            }
        )
    }

    /**
     * Step 3. Check the match between faces
     * */
    private fun faceMatch(callBack: AcuantDocumentProcessCallBack) {
        val data = FacialMatchData()
        data.faceImageOne = documentFaceImage!!
        data.faceImageTwo = selfieImage!!
        AcuantFaceMatch.processFacialMatch(
            data
        ) {
            if (it.isMatch) {
                repositoryScope.launch {
                    // if the faces match, all the data obtained from this process are saved locally
                    storeAcuantUserInfo()
                    clearData()
                    callBack.onSuccess(true, faceMatchFound = true)
                }
            } else {
                callBack.onError(KycRepositoryError.FaceMatchError, processIsRestarted = false)
            }
        }
    }

    /**
     * Step 4. Saves locally all the data obtained from the Acuant SDK
     * */
    private suspend fun storeAcuantUserInfo() {
        kycLocalDataSource.storeAcuantUserInfo(
            AcuantUserInfo(
                idResult!!.biographic.fullName,
                idResult!!.biographic.gender,
                idResult!!.classification.type.countryCode,
                idResult!!.documentNumber() ?: "",
                idResult!!.biographic.birthDate,
                idResult!!.documentIssueDate() ?: "",
                idResult!!.biographic.expirationDate,
                frontDocument!!.image,
                backDocument!!.image,
                documentFaceImage!!,
                selfieImage!!,
                instanceId!!
            )
        )
    }

    /**
     * Load existing identity data
     * */
    suspend fun loadAcuantUserInfo(): AcuantUserInfo? = kycLocalDataSource.acuantUserInfo()

    /**
     * Send the confirmation message of the identity data to the contact/connection obtained for this flow
     * */
    suspend fun confirmKycData() {
        loadAcuantUserInfo()?.let {
            val contact = kycLocalDataSource.kycContact()!!
            remoteDataSource.sendKycData(it, contact)
            it.isConfirmed = true
            kycLocalDataSource.storeAcuantUserInfo(it)
            it.recycleImages()
        }
    }
}
