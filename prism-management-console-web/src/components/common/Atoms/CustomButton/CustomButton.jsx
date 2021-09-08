import React from 'react';
import { Button } from 'antd';
import PropTypes from 'prop-types';
import SimpleLoading from '../SimpleLoading/SimpleLoading';
import { childrenType } from '../../../../helpers/propShapes';

import './_style.scss';

// The accepted classNames inside the buttonProps are:
//    - theme-primary
//    - theme-secondary
//    - theme-outline
//    - theme-grey
//    - theme-link
const CustomButton = ({ buttonText, buttonProps, loading, className, ...propagatedProps }) => (
  <Button
    {...propagatedProps}
    {...buttonProps}
    className={`${buttonProps?.className} ${className}`}
    disabled={buttonProps?.disabled || loading}
  >
    {loading ? <SimpleLoading /> : buttonText}
  </Button>
);

CustomButton.defaultProps = {
  buttonProps: {},
  buttonText: '',
  className: '',
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
      'theme-filter'
    ]),
    onClick: PropTypes.func,
    disabled: PropTypes.bool
  }),
  buttonText: childrenType,
  className: PropTypes.string,
  loading: PropTypes.bool
};

export default CustomButton;
