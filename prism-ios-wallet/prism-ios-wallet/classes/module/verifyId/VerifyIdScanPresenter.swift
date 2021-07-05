//
//  VerifyIdScanPresenter.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 01/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation
import AcuantImagePreparation
import AcuantCommon
import AcuantDocumentProcessing
import AcuantFaceMatch
import AcuantHGLiveness
import AcuantIPLiveness
import AcuantPassiveLiveness
import AVFoundation
import AcuantiOSSDKV11

class VerifyIdScanPresenter: BasePresenter {

    var viewImpl: VerifyIdScanViewController? {
        return view as? VerifyIdScanViewController
    }

    private let service: IAcuantTokenService = AcuantTokenService()

    public var documentInstance: String?
    public var livenessString: String?
    public var capturedFacialMatchResult: FacialMatchResult?
    public var capturedFaceImageUrl: String?
    private var isInitialized = false
    private var faceCapturedImage: UIImage?

    public var idOptions = IdOptions()
    public var idData = IdData()

    public var ipLivenessSetupResult: LivenessSetupResult?

    var autoCapture = true
    var detailedAuth = true
    var detailedIsResultDone = false

    private let createInstanceGroup = DispatchGroup()
    private let getDataGroup = DispatchGroup()
    private let faceProcessingGroup = DispatchGroup()
    private let showResultGroup = DispatchGroup()

    var documentInstanceID: String?
    var kycToken: String?
    var contact: Contact?
    var selfieImg: UIImage?
    var isSelfie = false

    let fieldsRequiered = ["full name", "sex", "nationality name", "document number",
                           "birth date", "issue date", "expiration date"]

    override func viewDidAppear() {
        super.viewDidAppear()
        if !self.isInitialized {
            if let token = kycToken {
                setAcuantToken(token: token)
            } else {
                self.getToken()
            }
        }
        if detailedIsResultDone {
            resetData()
        }
    }

    // MARK: Acuant

    func getToken() {

        self.viewImpl?.showLoading(doShow: true)
        let task = self.service.getTask { token in
            DispatchQueue.main.async {
                if let token = token {
                    self.setAcuantToken(token: token)
                } else {
                    self.viewImpl?.showLoading(doShow: false)
                    self.viewImpl?.showErrorMessage(doShow: true, message: "Failed to get Token")
                }
            }
        }
        task?.resume()
    }

    func setAcuantToken(token: String) {
        if AcuantCommon.Credential.setToken(token: token) {
            if !self.isInitialized {
                self.initialize()
            } else {
                self.viewImpl?.showLoading(doShow: false)
            }
        } else {
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "Invalid Token")
        }
    }

    private func initialize() {
        let initalizer: IAcuantInitializer = AcuantInitializer()
        let packages: [IAcuantPackage] = [AcuantImagePreparationPackage()]

        _ = initalizer.initialize(packages: packages) { [weak self] error in

            DispatchQueue.main.async {
                if let self = self {
                    if error == nil {
                        AcuantIPLiveness.getLivenessTestCredential(delegate: self)
                    } else {
                        if let msg = error?.errorDescription {
                            self.viewImpl?.showErrorMessage(doShow: true, message: "\(error!.errorCode) : \(msg)")
                        }
                    }
                    self.viewImpl?.showLoading(doShow: false)
                }
            }
        }
    }

    private func resetData() {
        capturedFaceImageUrl = nil
        capturedFacialMatchResult = nil
        documentInstance = nil
        ipLivenessSetupResult = nil
        livenessString = nil
        faceCapturedImage = nil
        idOptions.cardSide = CardSide.Front
        viewImpl?.toogleTitle(isBack: false)
        detailedIsResultDone = false
        isSelfie = false

        self.createInstance()
    }

    public func confirmImage(image: AcuantImage) {
        self.createInstanceGroup.notify(queue: .main) {
            self.viewImpl?.showLoading(doShow: true, message: "Processing...")
            self.selfieImg = image.image
            let evaluted = EvaluatedImageData(imageBytes: image.data, barcodeString: self.idData.barcodeString)

            AcuantDocumentProcessing.uploadImage(instancdId: self.documentInstance!, data: evaluted,
                                                 options: self.idOptions, delegate: self)
        }
    }

    public func retryCapture() {
        showDocumentCaptureCamera()
    }

    func isBackSideRequired(classification: Classification?) -> Bool {
        if classification == nil {
            return false
        }
        var isBackSideRequired: Bool = false
        let supportedImages: [Dictionary<String, Int>]? = classification?.type?.supportedImages
            as? [Dictionary<String, Int>]
        if supportedImages != nil {
            for image in supportedImages! {
                if image["Light"] == 0 && image["Side"] == 1 {
                    isBackSideRequired = true
                }
            }
        }
        return isBackSideRequired
    }

    // MARK: Buttons

    func scanTapped() {
        _ = AcuantCommon.Credential.getToken()

        if isSelfie {
            self.getIdDataAndStartFace()
        } else {
            if self.idOptions.cardSide != CardSide.Back {
                self.resetData()
            }
            self.showDocumentCaptureCamera()
        }
    }
}

//AcuantCamera - START =============
extension VerifyIdScanPresenter: CameraCaptureDelegate {

    public func setCapturedImage(image: Image, barcodeString: String?) {
        if image.image != nil {
            self.viewImpl?.showLoading(doShow: true, message: "Processing...")
            AcuantImagePreparation.evaluateImage(image: image.image!) { result, error in

                DispatchQueue.main.async {
                    if result != nil {
                        self.idData.barcodeString = barcodeString
                        self.confirmImage(image: result!)
                    } else {
                        self.viewImpl?.showErrorMessage(doShow: true, message: error?.errorDescription,
                                                        afterErrorAction: {
                            self.retryCapture()
                        })
                    }
                }
            }
        }
    }

    func showDocumentCaptureCamera() {
        //handler in .requestAccess is needed to process user's answer to our request
        AVCaptureDevice.requestAccess(for: .video) { [weak self] success in
            if success { // if request is granted (success is true)
                DispatchQueue.main.async {
                    let options = AcuantCameraOptions(digitsToShow: 2, autoCapture: self!.autoCapture,
                                                      hideNavigationBar: true)
                    let documentCameraController = DocumentCameraController.getCameraController(delegate: self!,
                                                                                                cameraOptions: options)
                    self!.viewImpl?.navigationController?.pushViewController(documentCameraController, animated: false)

                }
            } else { // if request is denied (success is false)
                // Create Alert
                let alert = UIAlertController(title: "Camera",
                                              message: "Camera access is absolutely necessary to use this app",
                                              preferredStyle: .alert)
                // Add "OK" Button to alert, pressing it will bring you to the settings app
                alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { _ in
                    UIApplication.shared.open(URL(string: UIApplication.openSettingsURLString)!)
                }))
                // Show the alert with animation
                self!.viewImpl?.present(alert, animated: true)
            }
        }
    }

    public func showKeylessHGLiveness() {

    }

}
//AcuantCamera - END =============

//DocumentProcessing - START ============

extension VerifyIdScanPresenter: CreateInstanceDelegate {

    func instanceCreated(instanceId: String?, error: AcuantError?) {
        if error == nil {
            documentInstance = instanceId
        } else {
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true,
                                            message: "\(error!.errorCode) : \((error?.errorDescription)!)")
        }
        self.createInstanceGroup.leave()
    }

    private func createInstance() {
        self.createInstanceGroup.enter()
        AcuantDocumentProcessing.createInstance(options: self.idOptions, delegate: self)
    }
}

extension VerifyIdScanPresenter: UploadImageDelegate {

    private func getIdDataAndStartFace() {
        if AcuantCommon.Credential.endpoints()?.frmEndpoint != nil {
            self.showFacialCaptureInterface()
        }
        self.getDataGroup.enter()
        AcuantDocumentProcessing.getData(instanceId: self.documentInstance!, isHealthCard: false, delegate: self)
        self.viewImpl?.showLoading(doShow: true, message: "Processing...")
    }

    func imageUploaded(error: AcuantError?, classification: Classification?) {
        self.viewImpl?.showLoading(doShow: false)

        if error == nil {
            self.idOptions.isRetrying = false
                if self.idOptions.cardSide == CardSide.Front {
                    if self.isBackSideRequired(classification: classification) {
                        // Capture Back Side
                        self.idOptions.cardSide = CardSide.Back
                        self.viewImpl?.toogleTitle(isBack: true)
                    } else {
                        self.isSelfie = true
                        self.viewImpl?.toogleToSelfie()
                    }
                } else {
                    self.isSelfie = true
                    self.viewImpl?.toogleToSelfie()
                }
        } else {
            self.viewImpl?.showErrorMessage(doShow: true,
                                            message: "\(error!.errorCode) : \((error?.errorDescription)!)")
        }
    }
}

func appendDetailed(dataArray: inout Array<String>, result: IDResult) {

    dataArray.append("Authentication Starts")
    dataArray.append("Authentication Overall : \(AcuantUtils.getAuthResultString(authResult: result.result))")
    if result.alerts?.actions != nil {
        for alert in result.alerts!.actions! {
            if alert.result != "1" {
                dataArray.append("\(alert.actionDescription ?? "nil") : \(alert.disposition ?? "nil")")
            }
        }
    }
    dataArray.append("Authentication Ends")
    dataArray.append("")
}

extension VerifyIdScanPresenter: GetDataDelegate {

    func processingResultReceived(processingResult: ProcessingResult) {
        if processingResult.error == nil {
            let idResult = processingResult as? IDResult
            if idResult?.fields == nil {
                self.viewImpl?.showLoading(doShow: false)
                self.viewImpl?.showErrorMessage(doShow: true, message: "Could not extract data")
                getDataGroup.leave()
                return
            } else if idResult?.fields!.documentFields == nil {
                self.viewImpl?.showLoading(doShow: false)
                self.viewImpl?.showErrorMessage(doShow: true, message: "Could not extract data")
                getDataGroup.leave()
                return
            } else if idResult?.fields!.documentFields!.count == 0 {
                self.viewImpl?.showLoading(doShow: false)
                self.viewImpl?.showErrorMessage(doShow: true, message: "Could not extract data")
                getDataGroup.leave()
                return
            }
            let fields: Array<DocumentField>! = idResult?.fields!.documentFields!
            var frontImageUri: String?
            var backImageUri: String?
            var signImageUri: String?
            var faceImageUri: String?

            var dataArray = [String]()

            for field in fields {
                if !fieldsRequiered.contains(field.key?.lowercased().trim() ?? "") {
                    continue
                }
                if field.type == "string" {
                    dataArray.append("\(field.key!) : \(field.value!)")
                } else if field.type == "datetime" {
                    dataArray.append("\(field.key!) : \(Utils.dateFieldToDateString(dateStr: field.value!)!)")
                } else if field.key == "Photo" && field.type == "uri" {
                    faceImageUri = field.value
                    capturedFaceImageUrl = faceImageUri
                } else if field.key == "Signature" && field.type == "uri" {
                    signImageUri = field.value
                }
            }

            for image in (idResult?.images?.images!)! {
                if image.side == 0 {
                    frontImageUri = image.uri
                } else if image.side == 1 {
                    backImageUri = image.uri
                }
            }

            self.showResult(data: dataArray, front: frontImageUri, back: backImageUri, sign: signImageUri,
                            face: faceImageUri)
        } else {
            self.viewImpl?.showLoading(doShow: false)
            if let msg = processingResult.error?.errorDescription {
                self.viewImpl?.showErrorMessage(doShow: true, message: msg)
            }
        }
    }

    func showResult(data: [String]?, front: String?, back: String?, sign: String?, face: String?) {
        self.getDataGroup.leave()
        self.viewImpl?.showLoading(doShow: false)
        self.viewImpl?.changeScreenToResults(values: data, contact: contact, image: self.selfieImg,
                                             documentInstanceId: documentInstance)
        detailedIsResultDone = true

    }
}
//DocumentProcessing - END ============

//IPLiveness - START ============

extension VerifyIdScanPresenter: LivenessTestCredentialDelegate {

    func livenessTestCredentialReceived(result: Bool) {
        self.isInitialized = true

        DispatchQueue.main.async {
            self.viewImpl?.showLoading(doShow: false)
            if result {
            }
        }
    }

    func livenessTestCredentialReceiveFailed(error: AcuantError) {
        self.viewImpl?.showLoading(doShow: false)
        self.viewImpl?.showErrorMessage(doShow: true, message: "\(error.errorCode) : \(error.errorDescription ?? "")")
    }
}

//IPLiveness - END ============

//Passive Liveness + FaceCapture - START ============

extension VerifyIdScanPresenter {
    private func processPassiveLiveness(image: UIImage) {
        self.faceProcessingGroup.enter()
        AcuantPassiveLiveness.postLiveness(request: AcuantLivenessRequest(image: image)) { [weak self] (result, err) in
            if result != nil && (result?.result == AcuantLivenessAssessment.Live
                                    || result?.result == AcuantLivenessAssessment.NotLive) {
                self?.livenessString = "Liveness : \(result!.result.rawValue)"
            } else {
                self?.livenessString = "Liveness : \(result?.result.rawValue ?? "Unknown") \(err?.errorCode?.rawValue ?? "") \(err?.description ?? "")"
            }
            self?.faceProcessingGroup.leave()
        }
    }

    public func showPassiveLiveness() {
        DispatchQueue.main.async {
            let controller = AcuantFaceCaptureController()
            controller.callback = { [weak self]
                (image) in

                if image != nil {
                    self?.faceCapturedImage = image
                    self?.processPassiveLiveness(image: image!)
                    self?.processFacialMatch(image: image!)
                }

                self?.faceProcessingGroup.notify(queue: .main) {
                    self?.showResultGroup.leave()
                }
            }
            self.viewImpl?.navigationController?.pushViewController(controller, animated: true)
        }
    }

    func showFacialCaptureInterface() {
        self.showResultGroup.enter()
        self.showPassiveLiveness()
    }
}

//Passive Liveness + FaceCapture - END ============

//FaceMatch - START ============
extension VerifyIdScanPresenter: FacialMatchDelegate {

    func facialMatchFinished(result: FacialMatchResult?) {
        self.faceProcessingGroup.leave()

        if result?.error == nil {
            capturedFacialMatchResult = result
        }
    }
    func processFacialMatch(image: UIImage?) {
        self.faceProcessingGroup.enter()
        self.viewImpl?.showLoading(doShow: true, message: "Processing...")
        self.getDataGroup.notify(queue: .main) {
            if self.capturedFaceImageUrl != nil && image != nil {
                let loginData = String(format: "%@:%@", AcuantCommon.Credential.username()!,
                                       AcuantCommon.Credential.password()!).data(using: String.Encoding.utf8)!
                let base64LoginData = loginData.base64EncodedString()

                // create the request
                let url = URL(string: self.capturedFaceImageUrl!)!
                var request = URLRequest(url: url)
                request.httpMethod = "GET"
                request.setValue("Basic \(base64LoginData)", forHTTPHeaderField: "Authorization")

                URLSession.shared.dataTask(with: request) { (data, response, error) in
                    let httpURLResponse = response as? HTTPURLResponse
                    if httpURLResponse?.statusCode == 200 {
                        let downloadedImage = UIImage(data: data!)

                        if downloadedImage != nil {
                            let facialMatchData = FacialMatchData(faceImageOne: downloadedImage!, faceImageTwo: image!)
                            AcuantFaceMatch.processFacialMatch(facialData: facialMatchData, delegate: self)
                        } else {
                            self.faceProcessingGroup.leave()
                        }
                    } else {
                        self.faceProcessingGroup.leave()
                        return
                    }
                }.resume()
            } else {
                self.faceProcessingGroup.leave()
                DispatchQueue.main.async {
                    self.viewImpl?.showLoading(doShow: false)
                }
            }
        }
    }
}
//FaceMatch - END ============
