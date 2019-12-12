//

import Crashlytics

class CrashReporter: NSObject {

    static func logUser(loggedUser: LoggedUser?) {

        if loggedUser == nil {
            return
        }

        Crashlytics.sharedInstance().setUserIdentifier(loggedUser?.id)
        Crashlytics.sharedInstance().setUserEmail(loggedUser?.email)
        Crashlytics.sharedInstance().setUserName(loggedUser?.lastName)
    }
}
