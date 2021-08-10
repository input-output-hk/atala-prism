import * as React from 'react';
import Back from '../../images/back.svg';
import './_style.scss';

const BackArrow = ({ backTo }) => {
  return (
    <div className="BackArrow">
      <div className="backBtnContainer">
        <img src={Back} alt="backBtn" />
        <a className="backBtn" href={backTo}>
          Back
        </a>
      </div>
    </div>
  );
};

export default BackArrow;
