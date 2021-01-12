//

import FirebaseCrashlytics

class CrashReporter: NSObject {

    static func logUser(loggedUser: LoggedUser?) {

        if let loggedUserId = loggedUser?.id {
            Crashlytics.crashlytics().setUserID(loggedUserId)

        }
    }
}
