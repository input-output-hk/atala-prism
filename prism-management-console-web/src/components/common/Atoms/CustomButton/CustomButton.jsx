import React from 'react';
import { Button } from 'antd';
import PropTypes from 'prop-types';
import SimpleLoading from '../SimpleLoading/SimpleLoading';

import './_style.scss';

// The accepted classNames inside the buttonProps are:
//    - theme-primary
//    - theme-secondary
//    - theme-outline
//    - theme-grey
//    - theme-link
//    - theme-filter
//    - theme-text
// alternatively it can be overriden with overrideClassName.
const CustomButton = ({
  buttonText,
  buttonProps,
  loading,
  overrideClassName,
  ...propagatedProps
}) => (
  <Button
    {...propagatedProps}
    {...buttonProps}
    className={overrideClassName || buttonProps?.className}
    disabled={buttonProps?.disabled || loading}
  >
    {loading ? <SimpleLoading /> : buttonText}
  </Button>
);

CustomButton.defaultProps = {
  buttonProps: {},
  buttonText: '',
  overrideClassName: '',
  loading: false
};

CustomButton.propTypes = {
  buttonProps: PropTypes.shape({
    className: PropTypes.oneOf([
      'theme-primary',
      'theme-secondary',
      'theme-outline',
      'theme-grey',
      'theme-link',
      'theme-filter',
      'theme-text'
    ]),
    onClick: PropTypes.func,
    disabled: PropTypes.bool
  }),
  buttonText: PropTypes.node,
  overrideClassName: PropTypes.string,
  loading: PropTypes.bool
};

export default CustomButton;
