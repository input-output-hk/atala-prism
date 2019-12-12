//

class TutorialPresenter: BasePresenter {

    var viewImpl: TutorialViewController? {
        return view as? TutorialViewController
    }

    func tappedRegisterButton() {
        viewImpl?.changeScreenToRegister()
    }

    func tappedLoginButton() {
        viewImpl?.changeScreenToLogin()
    }
}
