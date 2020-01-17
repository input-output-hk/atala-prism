import React from 'react';
import { Button } from 'antd';
import PropTypes from 'prop-types';
import { LEFT, RIGHT } from '../../../../helpers/constants';

import './_style.scss';

// The accepted classNames inside the buttonProps are:
//    - theme-primary
//    - theme-secondary
//    - theme-outline
//    - theme-grey
//    - theme-link
const CustomButton = ({ icon: { icon, side }, buttonText, buttonProps }) => (
  <Button {...buttonProps}>
    {side === LEFT && icon}
    {buttonText}
    {side === RIGHT && icon}
  </Button>
);

CustomButton.defaultProps = {
  buttonText: '',
  icon: { icon: null }
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
  icon: PropTypes.shape({ icon: PropTypes.element, side: PropTypes.string })
};

export default CustomButton;
