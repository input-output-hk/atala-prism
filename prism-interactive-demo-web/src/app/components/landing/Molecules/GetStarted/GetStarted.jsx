import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import { Link } from 'gatsby';
import CustomButton from '../../../../../components/customButton/CustomButton';
import { CONTACT_US_NAME } from '../../../../../helpers/constants';

import './_style.scss';

const GetStarted = () => {
  const { t } = useTranslation();

  return (
    <div className="GetStartedContent">
      <div className="TextContainer">
        <div className="GetStartedDescription">
          <h1>{t('landing.getStarted.title')}</h1>
          <h3>{t('landing.getStarted.part1')}</h3>
          <h3>{t('landing.getStarted.part2')}</h3>
          <h3>{t('landing.getStarted.part3')}</h3>
          <Link to={`#${CONTACT_US_NAME}`}>
            <CustomButton
              buttonProps={{
                className: 'theme-secondary'
              }}
              buttonText="Contact Us"
            />
          </Link>
        </div>
      </div>
    </div>
  );
};

export default GetStarted;
