import React from 'react';
import { Button } from 'antd';
import { PulseLoader } from 'react-spinners';
import PropTypes from 'prop-types';

import './_style.scss';

// The accepted classNames inside the buttonProps are:
//    - theme-primary
//    - theme-secondary
//    - theme-outline
//    - theme-grey
//    - theme-link
const CustomButton = ({ icon, buttonText, buttonProps, loading, loadingProps }) => (
  <Button {...buttonProps} disabled={buttonProps?.disabled || loading}>
    {icon && icon}
    {loading ? <PulseLoader loading {...loadingProps} /> : buttonText}
  </Button>
);

CustomButton.defaultProps = {
  buttonText: '',
  icon: null,
  loading: false,
  loadingProps: {
    size: 6,
    color: '#000000'
  }
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
  loadingProps: PropTypes.shape({
    size: PropTypes.number,
    color: PropTypes.string
  })
};

export default CustomButton;
