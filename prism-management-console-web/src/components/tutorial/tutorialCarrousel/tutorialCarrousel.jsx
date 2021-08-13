import React, { useState } from 'react';
import { Carousel } from 'antd';
import './_style.scss';
import onboarding1 from '../../../images/onboarding1.png';
import onboarding2 from '../../../images/onboarding2.png';
import onboarding3 from '../../../images/onboarding3.png';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';

function onChange(a, b, c) {
  console.log(a, b, c);
}

const TutorialCarrousel = () => {
  return (
    <Carousel className="tutorialOnboarding" afterChange={onChange}>
      <div className="contentStyle">
        <img src={onboarding1} alt="onboarding" />
        <h2>Welcome to Atala Management Console</h2>
        <p>
          Click "continue" to start the tutorial and take a walkthrough of the most common features.
        </p>
      </div>
      <div className="contentStyle">
        <img style={{ marginTop: 20, width: 220 }} src={onboarding2} alt="onboarding" />
        <h2>This is the Progress Tutorial Bar</h2>
        <p>
          You will find it on the bottom right and it will guide you throughout the different
          sections and its features.
        </p>
      </div>
      <div className="contentStyle">
        <img style={{ height: 190 }} src={onboarding3} alt="onboarding" />
        <h2>Always accessible</h2>
        <p>
          You can access it any time from the sidebar.
        </p>
        <CustomButton
        buttonProps={{
          className: 'theme-secondary',
        }}
        buttonText="Start Tutorial"
      />
      </div>
    </Carousel>
  );
};

export default TutorialCarrousel;
