import React from 'react';
import { Col } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import defaultIcon from '../../../../images/defaultCredentialImage.svg';

import './_style.scss';

const ConnectionSummary = ({ credential, setConnectionInfo }) => {
  const { t } = useTranslation();
  const { icon, type = 'Credential', graduationDate } = credential;

  return (
    <div className="ConnectionCredentials">
      <Col span={18} className="CredentialData">
        <img src={icon || defaultIcon} alt={t('connections.detail.iconAlt')} />
        <div className="CredentialDataText">
          <h3>{type}</h3>
          <p>{graduationDate}</p>
        </div>
      </Col>
      <Col span={6} className="CredentialLink">
        <CustomButton
          buttonProps={{
            className: 'theme-link',
            onClick: () => setConnectionInfo(credential)
          }}
          buttonText={t('actions.view')}
        />
      </Col>
    </div>
  );
};

ConnectionSummary.defaultProps = {
  icon: ''
};

ConnectionSummary.propTypes = {
  icon: PropTypes.string,
  type: PropTypes.string.isRequired,
  graduationDate: PropTypes.string.isRequired,
  setConnectionInfo: PropTypes.func.isRequired
};

export default ConnectionSummary;
