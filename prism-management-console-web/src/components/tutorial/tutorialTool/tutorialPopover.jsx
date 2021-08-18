import React from 'react';
import { Popover } from 'antd';
import './_style.scss';
import DashboardCard from '../../dashboard/organism/DashboardCard';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';

const content = (
  <div className="popOverStep1">
    <p className="steps">1 of 7</p>
    <div className="popoverText">
      <p className="subtitle">Basic Steps</p>
      <h2>Dashboard Analytics</h2>
      <p>Here you can take a look at the latests stats of your different contacts and groups</p>
    </div>
    <div className="popoverButtons">
      <CustomButton
        buttonProps={{
          className: 'theme-outline'
        }}
        buttonText="Back"
      />
      <CustomButton
        buttonProps={{
          className: 'theme-primary'
        }}
        buttonText="Next"
      />
    </div>
  </div>
);

const TutorialPopover = () => {
  return (
    <Popover placement="right" content={content}>
        {/* This popover should link to the next tutorial step */}
      <div className="step1">
        <DashboardCard />
      </div>
    </Popover>
  );
};

export default TutorialPopover;
