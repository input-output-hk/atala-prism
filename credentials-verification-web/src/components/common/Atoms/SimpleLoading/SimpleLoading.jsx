import React from 'react';
import PropTypes from 'prop-types';
import { PulseLoader } from 'react-spinners';

import './_style.scss';

const sizes = {
  xs: 3,
  sm: 6,
  md: 9,
  lg: 12,
  xl: 15
};

const SimpleLoading = ({ color, size }) => {
  return (
    <span className={`SimpleLoadingContainer ${size}`}>
      <PulseLoader loading size={sizes[size]} color={color} />
    </span>
  );
};

SimpleLoading.defaultProps = {
  color: '#000000',
  size: 'sm'
};

SimpleLoading.propTypes = {
  color: PropTypes.string,
  size: PropTypes.oneOf(Object.keys(sizes))
};

export default SimpleLoading;
