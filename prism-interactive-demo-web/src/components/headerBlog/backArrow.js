import * as React from 'react';
import PropTypes from 'prop-types';
import Back from '../../images/back.svg';
import './_style.scss';

const BackArrow = ({ backTo }) => (
  <div className="BackArrow">
    <div className="backBtnContainer">
      <img src={Back} alt="backBtn" />
      <a className="backBtn" href={backTo}>
        Back
      </a>
    </div>
  </div>
);

BackArrow.propTypes = {
  backTo: PropTypes.string.isRequired
};

export default BackArrow;
