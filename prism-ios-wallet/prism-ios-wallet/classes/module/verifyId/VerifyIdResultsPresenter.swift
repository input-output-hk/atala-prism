//
//  VerifyIdResultsPresenter.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 08/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation
import AcuantCommon

class VerifyIdResultsPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate,
                                VerifyIdResultsCellDelegate {

    var viewImpl: VerifyIdResultsViewController? {
        return view as? VerifyIdResultsViewController
    }

    struct InitialCellValue {
        var title: String
        var value: String?
    }

    lazy var initialStaticCells: [InitialCellValue] = []
    var contact: Contact?
    var selfieImg: UIImage?
    var documentInstanceId: String?
    var attributes: [Attribute] = []

    func config(values: [String?]) {

        for item in values {
            if let parts = item?.split(separator: ":", maxSplits: 2, omittingEmptySubsequences: false),
               parts.count > 1 {
                initialStaticCells.append(InitialCellValue(title: String(parts[0]), value: String(parts[1])))
                let attribute = Attribute()
                attribute.category = String(parts[0])
                attribute.value = String(parts[1])
                attributes.append(attribute)
            }
        }
    }

    private func sendMessage(image: Data?) {

        guard let contact = self.contact, let documentInstanceId = documentInstanceId, let image = image else {
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            return
        }
        self.viewImpl?.showLoading(doShow: true)

        // Call the service
        ApiService.call(async: {
            do {
                let responses = try ApiService.global.sendKycResult(contact: contact,
                                                                    documentInstanceId: documentInstanceId,
                                                                    selfieImage: image)
                Logger.d("shareCredential response: \(responses)")

            } catch {
                return error
            }
            return nil
        }, success: {
            let user = self.sharedMemory.loggedUser
            user?.personalAttributes = self.attributes
            self.sharedMemory.loggedUser = user
            self.viewImpl?.changeScreenToSuccess(action: self.actionSuccessContinue)
        }, error: { error in
            print(error)
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    lazy var actionSuccessContinue = SelectorAction(action: { [weak self] in
        self?.viewImpl?.goToMainScreen()
    })

    // MARK: ListingBaseTableUtilsPresenterDelegate

    func cleanData() {

    }

    func fetchData() {

    }

    func hasData() -> Bool {
        return true
    }

    func getElementCount() -> [Int] {
        return [initialStaticCells.count]
    }

    func getSectionCount() -> Int? {
        return 1
    }

    func getSectionHeaderViews() -> [UIView] {
        return [UIView()]
    }

    // MARK: Table

    func hasPullToRefresh() -> Bool {
        true
    }

    func actionPullToRefresh() {

    }

    // MARK: TypeSelectCellDelegate

    func setup(for cell: VerifyIdResultsTableViewCell) {
        if let index = cell.indexPath?.row {
            let item = initialStaticCells[index]
            cell.config(title: item.title, value: item.value)
        }
    }

    // MARK: Buttons

    func continueTapped() {
        guard let image = selfieImg?.jpegData(compressionQuality: 1) else {
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            return
        }
        sendMessage(image: image)
    }

    func retryTapped() {
        viewImpl?.onBackPressed()
    }
}
