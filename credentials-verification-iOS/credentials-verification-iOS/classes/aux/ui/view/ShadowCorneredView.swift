//
import Foundation

class ShadowCorneredView: UIView {

    var shadowAdded: Bool = false
    private var shadowLayer: CAShapeLayer!

    override func draw(_ rect: CGRect) {
        super.draw(rect)

        if shadowAdded { return }
        shadowAdded = true

        let shadowLayer = UIView(frame: self.frame)
        shadowLayer.backgroundColor = UIColor.clear
        shadowLayer.layer.shadowPath = UIBezierPath(roundedRect: bounds, cornerRadius: self.cornerRadius).cgPath
        shadowLayer.addDropShadow()
        shadowLayer.clipsToBounds = false

        self.superview?.addSubview(shadowLayer)
        self.superview?.bringSubviewToFront(self)
    }
}
