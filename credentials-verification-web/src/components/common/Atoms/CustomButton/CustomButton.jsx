import React from 'react';
import { Button } from 'antd';
import PropTypes from 'prop-types';

import './_style.scss';

// The accepted classNames inside the buttonProps are:
//    - theme-primary
//    - theme-secondary
//    - theme-outline
//    - theme-grey
//    - theme-link
const CustomButton = ({ icon, buttonText, buttonProps }) => (
  <Button {...buttonProps}>
    {icon && icon}
    {buttonText}
  </Button>
);

CustomButton.defaultProps = {
  icon: null
};

CustomButton.propTypes = {
  buttonProps: PropTypes.shape({
    className: PropTypes.oneOf([
      'theme-primary',
      'theme-secondary',
      'theme-outline',
      'theme-grey',
      'theme-link'
    ]),
    onClick: PropTypes.func
  }).isRequired,
  buttonText: PropTypes.string.isRequired,
  icon: PropTypes.element
};

export default CustomButton;
