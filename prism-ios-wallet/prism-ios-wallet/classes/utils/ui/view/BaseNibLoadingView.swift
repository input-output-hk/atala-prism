//

import UIKit

// Usage: Subclass your UIView from NibLoadView to automatically load a xib with the same name as your class
protocol NibDefinable {
    var nibName: String { get }
}

@IBDesignable
class BaseNibLoadingView: UIView, NibDefinable {

    @IBOutlet weak var view: UIView!

    var nibName: String {
        return String(describing: type(of: self))
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        nibSetup()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        nibSetup()
    }

    init() {
        super.init(frame: .zero)
        nibSetup()
    }

    private func nibSetup() {
        backgroundColor = .clear

        view = loadViewFromNib()
        view.frame = bounds
        view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.translatesAutoresizingMaskIntoConstraints = true

        addSubview(view)

        commonInit()
    }

    private func loadViewFromNib() -> UIView {
        let bundle = Bundle(for: type(of: self))
        let nib = UINib(nibName: nibName, bundle: bundle)
        if let nibView = nib.instantiate(withOwner: self, options: nil).first as? UIView {
            return nibView
        }
        return UIView()
    }

    func commonInit() {}
}
