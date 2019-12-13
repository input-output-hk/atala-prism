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
  buttonText: '',
  icon: null
};

CustomButton.propTypes = {
  buttonProps: PropTypes.shape({
    className: PropTypes.oneOf([
      'theme-primary',
      'theme-secondary',
      'theme-outline',
      'theme-grey',
      'theme-link',
      'theme-filter'
    ]),
    onClick: PropTypes.func
  }).isRequired,
  buttonText: PropTypes.string,
  icon: PropTypes.element
};

export default CustomButton;
