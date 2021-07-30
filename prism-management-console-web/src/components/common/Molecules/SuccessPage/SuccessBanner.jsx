import React from 'react';
import PropTypes from 'prop-types';
import { Button } from 'antd';
import img from '../../../../images/success-img.png';

const SuccessBanner = ({ title, message, buttonText, onContinue }) => (
  <div className="success-wrapper">
    <div className="success-container">
      <img className="img-success" src={img} alt="" />
      <h1>{title}</h1>
      <p>{message}</p>
      {onContinue && (
        <Button className="theme-secondary" onClick={onContinue}>
          {buttonText}
        </Button>
      )}
    </div>
  </div>
);

SuccessBanner.defaultProps = {
  title: '',
  message: '',
  buttonText: '',
  onContinue: undefined
};

SuccessBanner.propTypes = {
  title: PropTypes.string,
  message: PropTypes.string,
  buttonText: PropTypes.string,
  onContinue: PropTypes.func
};

export default SuccessBanner;
