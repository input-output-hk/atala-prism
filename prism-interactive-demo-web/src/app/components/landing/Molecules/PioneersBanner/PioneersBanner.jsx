import React from 'react';
import Icon from '../../../../images/pioneer-icon 1.svg';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const PioneersBanner = () => {
  return (
    <div className="PioneersBannerContent">
      <h1>Powering the trust economy</h1>
      <p>
        Atala PRISM is a decentralized identity solution that enables people to own their personal
        data and interact with organizations seamlessly, privately, and securely.
      </p>
      <div className="textContainer">
        <img src={Icon} />
        <h3>Join our Pioneers Program</h3>
        <p>
          Become part of a select group with early access to a set of courses explaining the core
          principles of Atala PRISM
        </p>
        <CustomButton
        buttonProps={{ className: 'theme-border'}}
        buttonText="Register now" />
      </div>
    </div>
  );
};

export default PioneersBanner;
