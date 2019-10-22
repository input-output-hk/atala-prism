import React from 'react';
import { Button } from 'antd';
import PropTypes from 'prop-types';

import './_style.scss';

// The accepted themes are:
//    - theme-primary
//    - theme-secondary
//    - theme-outline
//    - theme-grey
const CustomButton = ({ theme, icon, buttonText, onClick }) => (
  <Button onClick={onClick} className={theme}>
    {icon && icon}
    {buttonText}
  </Button>
);

CustomButton.defaultProps = {
  icon: null
};

CustomButton.propTypes = {
  theme: PropTypes.string.isRequired,
  buttonText: PropTypes.string.isRequired,
  icon: PropTypes.element,
  onClick: PropTypes.func.isRequired
};

export default CustomButton;
