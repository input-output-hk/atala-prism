import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../../common/Atoms/CustomButton/CustomButton';
import './style.scss';

const CredentialDetail = ({ img, credentialName, title, date }) => {
  const { t } = useTranslation();
  return (
    <div className="credentialDetailContainer">
      <div className="credentialDataContainer">
        <div className="img">
          <img className="icons" src={img} alt="Free University Tbilisi" />
        </div>
        <div className="credentialData">
          <p>{credentialName}</p>
          <span>{title}</span>
        </div>
      </div>
      <div className="credentialDataContainer">
        <div className="credentialData">
          <p>{t('credentials.detail.dateCreated')}</p>
          <span>{date}</span>
        </div>
        <div>
          <CustomButton
            buttonProps={{
              className: 'theme-link'
            }}
            buttonText="View"
          />
        </div>
      </div>
    </div>
  );
};
export default CredentialDetail;
