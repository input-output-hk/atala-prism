import React from 'react';
import { Col, Row } from 'antd';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { shortDateFormatter } from '../../../../helpers/formatters';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const Connection = ({ icon, type, date, id }) => {
  const { t } = useTranslation();
  return (
    <div className="ConnectionCredentials">
      <Col span={18} className="CredentialData">
        <img src={icon} alt={t('connections.detail.iconAlt')} />
        <div className="CredentialDataText">
          <h3>{type}</h3>
          <p>{shortDateFormatter(date)}</p>
        </div>
      </Col>
      <Col span={6} className="CredentialLink">
        <CustomButton buttonText={t('actions.view')} to={`/credential/${id}`} theme="theme-link" />
      </Col>
    </div>
  );
};

export default Connection;
