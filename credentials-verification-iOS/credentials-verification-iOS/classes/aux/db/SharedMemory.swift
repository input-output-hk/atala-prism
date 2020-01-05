//

import AlamofireObjectMapper
import Foundation
import ObjectMapper

class SharedMemory: NSObject {

    static let global = SharedMemory()

    let preferences = UserDefaults.standard
    let helper: SharedMemoryHelper

    // Main
    private var mLoggedUser: LoggedUser?
    private var mImageBank: ImageBank?

    // Used for change screen
    var changeScreenParams: [Any?]?
    var changeScreenSegueIdentif: String?

    // Used for push notifications
    var notifPushParams: [Any?]?
    var notifPushSegueIdentif: String?

    override init() {

        helper = SharedMemoryHelper()
    }

    public func logoutUser() {

        mLoggedUser = nil
        mImageBank = nil
        if let bundle = Bundle.main.bundleIdentifier {
            preferences.removePersistentDomain(forName: bundle)
        }
    }

    // MARK: Getters and setters

    var loggedUser: LoggedUser? {
        get {
            mLoggedUser = helper.getMappableObject(key: Common.KEY_PREF + "loggeduser", internalObject: mLoggedUser)
            return mLoggedUser
        }
        set {
            mLoggedUser = newValue
            helper.setMappableObject(key: Common.KEY_PREF + "loggeduser", internalObject: mLoggedUser)
        }
    }

    var imageBank: ImageBank? {
        get {
            mImageBank = helper.getMappableObject(key: Common.KEY_PREF + "imagebank", internalObject: mImageBank)
            return mImageBank
        }
        set {
            mImageBank = newValue
            helper.setMappableObject(key: Common.KEY_PREF + "imagebank", internalObject: mImageBank)
        }
    }
}
