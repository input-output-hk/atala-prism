//

class TutorialPresenter: BasePresenter {

    var viewImpl: TutorialViewController? {
        return view as? TutorialViewController
    }

    func tappedRegisterButton() {
        Tracker.global.trackCreateAccountTapped()
        viewImpl?.changeScreenToRegister()
    }

    func tappedRestoreAccountButton() {
        viewImpl?.changeScreenToRestoreAccount()
    }
}
