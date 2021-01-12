//

protocol RegisterSeedViewDelegate: class {}

class RegisterSeedView: BaseNibLoadingView {

    weak var delegate: RegisterSeedViewDelegate?
    var index: Int = 0

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var viewBg: UIView!

    func config(delegate: RegisterSeedViewDelegate, index: Int, text: String?) {

        self.delegate = delegate
        self.index = index
        config(text: text)
    }

    func config(text: String?) {

        self.labelTitle.text = text
        self.layoutIfNeeded()
    }
}
