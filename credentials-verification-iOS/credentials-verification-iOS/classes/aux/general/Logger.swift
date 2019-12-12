//
import SwiftyBeaver

class Logger {

    private static var logger = SwiftyBeaver.self

    private init() {}

    static func setup() {

        // add log destinations. at least one is needed!
        let console = ConsoleDestination() // log to Xcode Console
        let file = FileDestination() // log to default swiftybeaver.log file
        // let remote = SBPlatformDestination(appID: Common.LOGS_APP_ID, appSecret: Common.LOGS_APP_SECRET, encryptionKey: Common.LOGS_APP_ENCRYPT_KEY)

        if Common.DEBUG {
            SwiftyBeaver.self.addDestination(console)
            SwiftyBeaver.self.addDestination(file)
            // SwiftyBeaver.self.addDestination(remote)
        }
        v("Log setup finished")
    }

    static func d(_ message: String) {
        logger.debug(message)
    }

    static func w(_ message: String) {
        logger.warning(message)
    }

    static func e(_ message: String) {
        logger.error(message)
    }

    static func i(_ message: String) {
        logger.info(message)
    }

    static func v(_ message: String) {
        logger.verbose(message)
    }
}
