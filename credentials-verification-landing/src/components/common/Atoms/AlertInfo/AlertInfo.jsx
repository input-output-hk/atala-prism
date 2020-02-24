import React from 'react';
import PropTypes from 'prop-types';
import './_style.scss';

const AlertInfo = ({ titleAlert, textAlert }) => (
  <div className="AlertInfo">
    <p>
      <strong>{titleAlert}</strong>
    </p>
    <p>{textAlert}</p>
  </div>
);

AlertInfo.propTypes = {
  titleAlert: PropTypes.string.isRequired,
  textAlert: PropTypes.string.isRequired,
};

export default AlertInfo;
