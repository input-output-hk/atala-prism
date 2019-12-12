//
import Foundation

class BasePresenter: NSObject {

    let sharedMemory: SharedMemory = SharedMemory.global
    let apiService = ApiService.global

    // abstract var
    var isPublicScreen: Bool { return false }

    weak var view: BaseView?

    override init() {
        super.init()
    }

    public func viewDidLoad() {
        Logger.d("Presenter's viewDidLoad: \(String(describing: self))")
    }

    public func viewDidAppear() {
        Logger.d("Presenter's viewDidAppear: \(String(describing: self))")
    }

    public func viewDidDisappear() {
        Logger.d("Presenter's viewDidDisappear: \(String(describing: self))")
    }

    public func viewWillAppear() {
        Logger.d("Presenter's viewWillAppear: \(String(describing: self))")
    }

    public func viewWillDisappear() {
        Logger.d("Presenter's viewWillDisappear: \(String(describing: self))")
    }

    public func didEnterForeground() {
        Logger.d("Presenter's didEnterForeground: \(String(describing: self))")
    }

    public func willEnterForeground() {
        Logger.d("Presenter's willEnterForeground: \(String(describing: self))")
    }

    public func didEnterBackground() {
        Logger.d("Presenter's didEnterBackground: \(String(describing: self))")
    }

    public func willEnterBackground() {
        Logger.d("Presenter's willEnterBackground: \(String(describing: self))")
    }

    func doLogout() {

        Logger.d("Logging out user")
        UserUtils.logout(sharedMemory)
        view?.exitToLogin()
    }
}
