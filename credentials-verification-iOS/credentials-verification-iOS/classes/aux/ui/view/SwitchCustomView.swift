//
import UIKit

protocol SwitchCustomDelegate: class {
    func stateChanged(for view: SwitchCustomView, newState: Bool)
}

@IBDesignable class SwitchCustomView: BaseNibLoadingView {

    weak var delegate: SwitchCustomDelegate?

    @IBOutlet weak var imageView: UIImageView!

    @IBInspectable var imageOn: UIImage?
    @IBInspectable var imageOff: UIImage? {
        didSet {
            self.imageView.image = imageOff
        }
    }

    private var currentState: Bool = false

    func getState() -> Bool {
        return currentState
    }

    func changeState(newState: Bool) {
        self.currentState = newState
        self.imageView.image = newState ? imageOn : imageOff
    }

    override func commonInit() {
        super.commonInit()

        changeState(newState: currentState)
        imageView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(SwitchCustomView.imageTapped(_:))))
    }

    @objc func imageTapped(_ sender: UITapGestureRecognizer) {
        changeState(newState: !self.currentState)
        delegate?.stateChanged(for: self, newState: self.currentState)
    }
}
