// Author: https://github.com/dillidon
// Source: https://github.com/dillidon/alerts-and-pickers
// License: https://github.com/dillidon/alerts-and-pickers/blob/new/LICENSE
// Note: Some of the contents of the original source may have been modified.
import UIKit

// MARK: Properties

extension UITextField {

    public typealias TextFieldConfig = (UITextField) -> Swift.Void

    public func config(textField configurate: TextFieldConfig?) {
        configurate?(self)
    }

    func left(image: UIImage?, color: UIColor = .black) {
        if let image = image {
            leftViewMode = UITextField.ViewMode.always
            let imageView = UIImageView(frame: CGRect(x: 0, y: 0, width: 20, height: 20))
            imageView.contentMode = .scaleAspectFit
            imageView.image = image
            imageView.image = imageView.image?.withRenderingMode(.alwaysTemplate)
            imageView.tintColor = color
            leftView = imageView
        } else {
            leftViewMode = UITextField.ViewMode.never
            leftView = nil
        }
    }

    func right(image: UIImage?, color: UIColor = .black) {
        if let image = image {
            rightViewMode = UITextField.ViewMode.always
            let imageView = UIImageView(frame: CGRect(x: 0, y: 0, width: 20, height: 20))
            imageView.contentMode = .scaleAspectFit
            imageView.image = image
            imageView.image = imageView.image?.withRenderingMode(.alwaysTemplate)
            imageView.tintColor = color
            rightView = imageView
        } else {
            rightViewMode = UITextField.ViewMode.never
            rightView = nil
        }
    }
    
    func addRightViewWith(image: UIImage) {
        let imageView = UIImageView.init(image: image)
        
        imageView.frame = CGRect(x: CGFloat(self.frame.size.width - 25), y: CGFloat(5), width: CGFloat(25), height: CGFloat(25))
        imageView.tag = self.tag
        imageView.image = imageView.image?.withRenderingMode(.alwaysTemplate)
        imageView.tintColor = UIColor.appGreyBlue

        rightView = imageView
        rightViewMode = .always
        
        rightView?.isUserInteractionEnabled = true
    }
    
    func addRightViewWith(text: String) {
        let label = UILabel.init(frame: CGRect(x: CGFloat(self.frame.size.width - 25), y: CGFloat(5), width: CGFloat(80), height: CGFloat(25)))
        
        label.tag = self.tag
        label.text = text
        label.textColor = UIColor.appGreyBlue
        
        rightView = label
        rightViewMode = .always
    }
}

// MARK: Methods

public extension UITextField {

    /// Set placeholder text color.
    ///
    /// - Parameter color: placeholder text color.
    func setPlaceHolderTextColor(_ color: UIColor) {
        self.attributedPlaceholder = NSAttributedString(string: self.placeholder != nil ? self.placeholder! : "",
                                                        attributes: [NSAttributedString.Key.foregroundColor: color])
    }

    /// Set placeholder text and its color
    func placeholder(text value: String, color: UIColor = .red) {
        self.attributedPlaceholder = NSAttributedString(string: value,
                                                        attributes: [NSAttributedString.Key.foregroundColor: color])
    }
}
