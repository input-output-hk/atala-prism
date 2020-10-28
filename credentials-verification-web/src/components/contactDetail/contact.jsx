import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import DetailBox from './molecules/detailBox/detailBox';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import IdIcon from '../../images/IdIcon.svg';
import './_style.scss';
import CredentialDetail from './molecules/detailBox/CredentialDetails/credentialDetail';

const Contact = () => {
  const { t } = useTranslation();
  const credNumber = 3;
  return (
    <div className="contactDetail">
      <div className="ContentHeader headerSection">
        <h1>{t('contacts.detail.detailSection.title')}</h1>
      </div>
      <div className="detailSection">
        <div>
          <h3>{t('contacts.detail.detailSection.subtitle')}</h3>
          <div className="header">
            <div className="img">
              <img className="IconUniversity" src={IdIcon} alt="Icon University" />
            </div>
            <div className="title">
              <p>Contact Name</p>
              <span>Kate Smith</span>
            </div>
            <div className="title">
              <p>External ID</p>
              <span>000000</span>
            </div>
          </div>
          <p className="subtitle">{t('contacts.detail.detailSection.credentialSubtitle')}</p>
          <DetailBox />
        </div>
        <div className="CredentialInfo">
          <div className="CredentialTitleContainer">
            <span>
              {t('contacts.detail.credIssued')} ({credNumber})
            </span>
            <span>
              {t('contacts.detail.credReceived')} ({credNumber})
            </span>
          </div>
          <p>{t('contacts.detail.detailSection.groupsSubtitle')}</p>
          <div className="credentialDetailsContainer">
            <CredentialDetail
              img={IdIcon}
              credentialName="Government ID"
              title="Government ID"
              date="10/11/2020"
            />
          </div>
        </div>
      </div>
      <div className="buttonSection">
        <CustomButton buttonText="Back" buttonProps={{ className: 'theme-grey' }} />
      </div>
    </div>
  );
};
export default Contact;
