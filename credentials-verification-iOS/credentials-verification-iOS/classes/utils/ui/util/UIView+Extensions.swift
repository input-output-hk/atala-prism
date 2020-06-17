//

import UIKit

extension UIView {

    func refreshView() {

        self.layoutIfNeeded()
    }

    func reloadView() {

        self.setNeedsLayout()
        self.layoutIfNeeded()
    }

    func addOnClickListener(action: SelectorAction?, numberOfTaps: Int = 1) {

        let gesture = UITapGestureRecognizer(target: action, action: #selector(action?.action(_:)))
        gesture.cancelsTouchesInView = false
        if numberOfTaps != 1 {
            gesture.numberOfTapsRequired = numberOfTaps
        }
        self.addGestureRecognizer(gesture)
    }

    public func addRoundCorners(radius: CGFloat = 5,
                                borderWidth: CGFloat = 0,
                                borderColor: CGColor = UIColor.white.cgColor,
                                onlyTops: Bool = false,
                                onlyBottoms: Bool = false) {

        layer.cornerRadius = radius
        layer.masksToBounds = true
        layer.borderWidth = borderWidth
        layer.borderColor = borderColor
        if onlyTops {
            layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        } else if onlyBottoms {
            layer.maskedCorners = [.layerMinXMaxYCorner, .layerMaxXMaxYCorner]
        }
    }

    func addDropShadow(radius: CGFloat = 4.0,
                       opacity: Float = 0.4,
                       offset: CGSize = CGSize(width: 0.0, height: 1.0),
                       color: UIColor = UIColor.darkGray) {

        layer.shadowColor = color.cgColor
        layer.shadowOpacity = opacity
        layer.shadowOffset = offset
        layer.shadowRadius = radius
        layer.masksToBounds = false
    }

    static let SHADOW_LAYER_TAG = 300

    func addShadowLayer(radius: CGFloat = 4.0,
                        opacity: Float = 0.4,
                        offset: CGSize = CGSize(width: 0.0, height: 1.0),
                        color: UIColor = UIColor.darkGray) {

        if superview?.subviews.contains(where: { $0.tag == UIView.SHADOW_LAYER_TAG }) ?? false {
            return
        }
        let viewShadow = UIView(frame: frame)
        viewShadow.backgroundColor = UIColor.white
        viewShadow.addDropShadow(radius: radius, opacity: opacity, offset: offset, color: color)
        viewShadow.layer.cornerRadius = self.cornerRadius
        viewShadow.tag = UIView.SHADOW_LAYER_TAG
        superview?.insertSubview(viewShadow, belowSubview: self)

        viewShadow.translatesAutoresizingMaskIntoConstraints = false
        superview?.addConstraint(NSLayoutConstraint(item: viewShadow, attribute: .width, relatedBy: .equal,
                                                    toItem: self, attribute: .width, multiplier: 1.0, constant: 0))
        superview?.addConstraint(NSLayoutConstraint(item: viewShadow, attribute: .height, relatedBy: .equal,
                                                    toItem: self, attribute: .height, multiplier: 1.0, constant: 0))
        superview?.addConstraint(NSLayoutConstraint(item: viewShadow, attribute: .leading, relatedBy: .equal,
                                                    toItem: self, attribute: .leading, multiplier: 1.0, constant: 0))
        superview?.addConstraint(NSLayoutConstraint(item: viewShadow, attribute: .top, relatedBy: .equal,
                                                    toItem: self, attribute: .top, multiplier: 1.0, constant: 0))
        layoutIfNeeded()
    }

    func setVerticalGradientBackground(colorOne: UIColor, colorTwo: UIColor) {

        let gradientLayer = CAGradientLayer()
        gradientLayer.colors = [colorOne.cgColor, colorTwo.cgColor]
        gradientLayer.startPoint = CGPoint(x: 0.5, y: 1.0)
        gradientLayer.endPoint = CGPoint(x: 0.5, y: 0.0)
        gradientLayer.locations = [0, 1]
        gradientLayer.frame = bounds

        layer.insertSublayer(gradientLayer, at: 0)
    }
}

class Dot: UIView {
    override func layoutSubviews() {
        layer.cornerRadius = bounds.size.width / 2
    }
}
