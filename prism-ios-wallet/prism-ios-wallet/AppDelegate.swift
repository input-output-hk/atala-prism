//
import FirebaseCrashlytics
import Firebase
import UIKit
import CoreData
import crypto

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.

        setupFirebase()
        setupLoggingPlatform()

        // Only for the debug version, restore the custom URL
        if !Env.isProduction() {
            Common.URL_API = SharedMemory.global.loggedUser?.apiUrl ?? Common.URL_API
            Logger.d("API URL: \(Common.URL_API)")
        }

        // There is a known bug in Kotlin Native that sometimes causes memory leaks, making the app crash, the suggested workaround is to invoke any Kotlin method from the Main thread
        SHA256Digest.Companion().compute(bytes: [0, 1, 2])
        if CryptoUtils.global.seed != nil {
            
            // There is a known bug in Kotlin Native that causes an error when calling "KeyDerivation().deriveKey(seed: seed, path: path)" from a background thread, making the app crash, the suggested workaround is to invoke this Kotlin method for the first time from the Main thread
            let keyPath = CryptoUtils.global.getNextPublicKeyPath()
            _ = CryptoUtils.global.signData(data: Data(), keyPath: keyPath)
        }

        return true
    }

    // MARK: UISceneSession Lifecycle

    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession,
                     options: UIScene.ConnectionOptions) -> UISceneConfiguration {
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

    func setupFirebase() {
        FirebaseApp.configure()
    }

    // MARK: - Core Data stack

       lazy var persistentContainer: NSPersistentContainer = {
           /*
            The persistent container for the application. This implementation
            creates and returns a container, having loaded the store for the
            application to it. This property is optional since there are legitimate
            error conditions that could cause the creation of the store to fail.
           */
           let container = NSPersistentContainer(name: "AtalaPrismModel")
           container.loadPersistentStores(completionHandler: { (storeDescription, error) in
               if let error = error as NSError? {
                   // Replace this implementation with code to handle the error appropriately.
                   // fatalError() causes the application to generate a crash log and terminate. You should not use this function in a shipping application, although it may be useful during development.

                   /*
                    Typical reasons for an error here include:
                    * The parent directory does not exist, cannot be created, or disallows writing.
                    * The persistent store is not accessible, due to permissions or data protection when the device is locked.
                    * The device is out of space.
                    * The store could not be migrated to the current model version.
                    Check the error message to determine what the actual problem was.
                    */
                   fatalError("Unresolved error \(error), \(error.userInfo)")
               }
           })
           return container
       }()

       // MARK: - Core Data Saving support

       func saveContext () {
           let context = persistentContainer.viewContext
           if context.hasChanges {
               do {
                   try context.save()
               } catch {
                   // Replace this implementation with code to handle the error appropriately.
                   // fatalError() causes the application to generate a crash log and terminate. You should not use this function in a shipping application, although it may be useful during development.
                   let nserror = error as NSError
                   fatalError("Unresolved error \(nserror), \(nserror.userInfo)")
               }
           }
       }
}
