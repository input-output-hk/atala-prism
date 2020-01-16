//
import Crashlytics
import Fabric
import Firebase
import UIKit

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.

        setupFirebase()
        setupCrashlytics()
        setupLoggingPlatform()

        // Only for the debug version, restore the custom URL
        if Common.DEBUG {
            Common.URL_API = SharedMemory.global.loggedUser?.apiUrl ?? Common.URL_API
            Logger.d("API URL: \(Common.URL_API)")
        }

        return true
    }

    // MARK: UISceneSession Lifecycle

    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        // Called when a new scene session is being created.
        // Use this method to select a configuration to create the new scene with.
        return UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
    }

    func application(_ application: UIApplication, didDiscardSceneSessions sceneSessions: Set<UISceneSession>) {
        // Called when the user discards a scene session.
        // If any sessions were discarded while the application was not running, this will be called shortly after application:didFinishLaunchingWithOptions.
        // Use this method to release any resources that were specific to the discarded scenes, as they will not return.
    }

    // MARK: Logs

    func setupLoggingPlatform() {

        Logger.setup()
    }

    // MARK: Firebase et al.

    func setupCrashlytics() {
        Fabric.with([Crashlytics.self])
    }

    func setupFirebase() {
        FirebaseApp.configure()
    }
}
