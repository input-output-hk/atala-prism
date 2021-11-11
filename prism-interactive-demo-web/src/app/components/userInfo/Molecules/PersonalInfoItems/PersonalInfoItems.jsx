import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import './_style.scss';

const items = [
  {
    label: 'userInfo.PersonalInfoItems.address',
    placeholder: 'userInfo.PersonalInfoItems.addressPlaceholder',
    className: 'large'
  },
  {
    label: 'userInfo.PersonalInfoItems.zipCode',
    placeholder: 'userInfo.PersonalInfoItems.zipCodePlaceholder'
  },
  {
    label: 'userInfo.PersonalInfoItems.city',
    placeholder: 'userInfo.PersonalInfoItems.cityPlaceholder'
  },
  {
    label: 'userInfo.PersonalInfoItems.state',
    placeholder: 'userInfo.PersonalInfoItems.statePlaceholder'
  },
  {
    label: 'userInfo.PersonalInfoItems.country',
    placeholder: 'userInfo.PersonalInfoItems.countryPlaceholder'
  }
];

const PersonalInfoItems = () => {
  const { t } = useTranslation();
  return (
    <div className="PersonalInfoItems">
      {items.map(({ label, placeholder, className }) => (
        <div className={`InfoItem ${className}`}>
          <span>{t(label)}</span>
          <p>{t(placeholder)}</p>
        </div>
      ))}
    </div>
  );
};

export default PersonalInfoItems;
