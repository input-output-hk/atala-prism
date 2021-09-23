import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import './_style.scss';

const PersonalInfoItems = () => {
  const { t } = useTranslation();
  return (
    <div className="PersonalInfoItems">
      <div className="InfoItem large">
        <span>{t('userInfo.PersonalInfoItems.address')}</span>
        <p>{t('userInfo.PersonalInfoItems.addressPlaceholder')}</p>
      </div>
      <div className="InfoItem">
        <span>{t('userInfo.PersonalInfoItems.zipCode')}</span>
        <p>{t('userInfo.PersonalInfoItems.zipCodePlaceholder')}</p>
      </div>
      <div className="InfoItem">
        <span>{t('userInfo.PersonalInfoItems.city')}</span>
        <p>{t('userInfo.PersonalInfoItems.cityPlaceholder')}</p>
      </div>
      <div className="InfoItem">
        <span>{t('userInfo.PersonalInfoItems.state')}</span>
        <p>{t('userInfo.PersonalInfoItems.statePlaceholder')}</p>
      </div>
      <div className="InfoItem">
        <span>{t('userInfo.PersonalInfoItems.country')}</span>
        <p>{t('userInfo.PersonalInfoItems.countryPlaceholder')}</p>
      </div>
    </div>
  );
};

export default PersonalInfoItems;
