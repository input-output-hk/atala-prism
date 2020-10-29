import React from 'react';
import { useTranslation } from 'react-i18next';
import './_style.scss';
import { Divider } from 'antd';
import imgCard from '../../../images/credentialIcon.svg';
import IconItems from '../Molecules/DashboardCardsItems/IconItems';
import iconDraft from '../../../images/iconDraft.svg';
import iconSigned from '../../../images/iconSigned.svg';
import iconSent from '../../../images/iconArrowRight.svg';
import iconReceived from '../../../images/iconArrowLeft.svg';

const DashboardCardCredential = () => {
  const { t } = useTranslation();
  return (
    <div className="DashboardContactCardContainer">
      <div className="HeaderCardContainer">
        <div>
          <img src={imgCard} alt="img" srcSet="" />
        </div>
        <span className="titleCardContainer">{t('contacts.table.columns.credentials')}</span>
      </div>
      <div className="divider">
        <Divider />
      </div>
      <IconItems icon={iconDraft} title="Draft" value="5" />
      <IconItems icon={iconSigned} title="Signed" value="5" />
      <IconItems icon={iconSent} title="Sent" value="110" />
      <IconItems icon={iconReceived} title="Received" value="87" />
    </div>
  );
};

export default DashboardCardCredential;
