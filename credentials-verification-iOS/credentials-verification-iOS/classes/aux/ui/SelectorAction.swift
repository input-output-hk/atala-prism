//

public class SelectorAction: NSObject {

    private var _action: () -> Void

    init(action: @escaping () -> Void) {
        _action = action
        super.init()
    }

    @objc func action(_ recognizer: UITapGestureRecognizer? = nil) {
        _action()
    }
}
