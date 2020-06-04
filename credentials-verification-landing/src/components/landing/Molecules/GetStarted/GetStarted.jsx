import React from 'react';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { CONTACT_US_NAME } from '../../../../helpers/constants';

import './_style.scss';

const GetStarted = ({ executeScroll }) => {
  const { t } = useTranslation();

  return (
    <div className="GetStartedContent">
      <div className="TextContainer">
        <div className="GetStartedDescription">
          <h1>{t('landing.getStarted.title')}</h1>
          <h3>{t('landing.getStarted.part1')}</h3>
          <h3>{t('landing.getStarted.part2')}</h3>
          <h3>{t('landing.getStarted.part3')}</h3>
          <CustomButton
            buttonProps={{
              className: 'theme-secondary',
              onClick: () => executeScroll(CONTACT_US_NAME)
            }}
            buttonText="Contact Us"
          />
        </div>
      </div>
    </div>
  );
};

export default GetStarted;
