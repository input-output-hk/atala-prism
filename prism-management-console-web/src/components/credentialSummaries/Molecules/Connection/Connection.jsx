import React from 'react';
import { Col } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import iconCredentials from '../../../../images/icon-credentials.svg';

import './_style.scss';

const Connection = ({
  icon,
  type,
  date,
  university,
  student,
  graduationDate,
  setConnectionInfo
}) => {
  const { t } = useTranslation();

  return (
    <div className="ConnectionCredentials">
      <Col span={18} className="CredentialData">
        <img src={icon || iconCredentials} alt={t('connections.detail.iconAlt')} />
        <div className="CredentialDataText">
          <h3>{type}</h3>
          <p>{date}</p>
        </div>
      </Col>
      <Col span={6} className="CredentialLink">
        <CustomButton
          buttonProps={{
            className: 'theme-link',
            onClick: () =>
              setConnectionInfo({
                title: type,
                startDate: date,
                university,
                student,
                graduationDate
              })
          }}
          buttonText={t('actions.view')}
        />
      </Col>
    </div>
  );
};

Connection.defaultProps = {
  icon: ''
};

Connection.propTypes = {
  icon: PropTypes.string,
  type: PropTypes.string.isRequired,
  graduationDate: PropTypes.string.isRequired,
  setConnectionInfo: PropTypes.func.isRequired
};

export default Connection;
