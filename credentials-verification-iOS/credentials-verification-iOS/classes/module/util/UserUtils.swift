//

class UserUtils {

    static func isLogged(_ sharedMemory: SharedMemory) -> Bool {
        return sharedMemory.loggedUser != nil && sharedMemory.loggedUser?.id != nil
    }

    static func logout(_ sharedMemory: SharedMemory) {
        sharedMemory.logoutUser()
    }

    static func openLoginScreen(caller: UIViewController) {
        WelcomeViewController.openThisView(caller)
    }

    static func exitToLogin(caller: UIViewController) {

        caller.callOnMain(millis: 0) {
            UserUtils.openLoginScreen(caller: caller)
        }
    }
}
