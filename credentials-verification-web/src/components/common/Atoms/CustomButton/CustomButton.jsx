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
const CustomButton = ({ icon, buttonText, buttonProps, loading, LoadingComponent }) => (
  <Button {...buttonProps} disabled={buttonProps?.disabled || loading}>
    {icon && icon}
    {loading ? <LoadingComponent /> : buttonText}
  </Button>
);

CustomButton.defaultProps = {
  buttonText: '',
  icon: null,
  loading: false,
  LoadingComponent: null
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
    onClick: PropTypes.func,
    disabled: PropTypes.bool
  }).isRequired,
  buttonText: PropTypes.string,
  icon: PropTypes.element,
  loading: PropTypes.bool,
  LoadingComponent: PropTypes.oneOfType([PropTypes.node, PropTypes.func])
};

export default CustomButton;
