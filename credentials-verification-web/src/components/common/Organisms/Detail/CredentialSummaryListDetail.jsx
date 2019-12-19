import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Collapse, Icon } from 'antd';
import { credentialSummaryShape } from '../../../../helpers/propShapes';
import { shortBackendDateFormatter } from '../../../../helpers/formatters';
import Connection from '../../../credentialSummaries/Molecules/Connection/ConnectionSummary';
import CustomButton from '../../Atoms/CustomButton/CustomButton';
import defaultIcon from '../../../../images/holder-default-avatar.svg';

import './_style.scss';
import CredentialSummaryDetail from './CredentialSummaryDetail';

const CredentialSummaryListDetail = ({ user, credentials, date }) => {
  const { icon, fullname } = user;
  const { t } = useTranslation();

  const [connectionInfo, setConnectionInfo] = useState();

  const genExtra = () => <Icon type="caret-down" />;
  const role = localStorage.getItem('role');

  return (
    <div className="CredentialSummaryDetail">
      <CredentialSummaryDetail
        drawerInfo={{
          title: t('credentials.detail.title'),
          onClose: () => setConnectionInfo(null),
          visible: !!connectionInfo
        }}
        credentialData={connectionInfo}
      />
      <div className="CredentialSummaryHeader">
        <img src={icon || defaultIcon} alt={t('credentialSummary.detail.alt', fullname)} />
        <div className="CredentialSummaryUser">
          <h3>{fullname}</h3>
          <p>{shortBackendDateFormatter(date)}</p>
        </div>
      </div>
      {role && (
        <Collapse accordion className="TransactionsDetail">
          <Collapse.Panel
            header={t('credentialSummary.detail.transactions')}
            key="1"
            showArrow={false}
            extra={genExtra()}
          />
        </Collapse>
      )}
      {credentials.map(({ enrollmentdate, title, graduationdate }) => (
        <div className="CredentialSummaryLine">
          <p>{t('credentialSummary.detail.credentialsType.bachelorsDegree')}</p>
          <Connection
            credential={{
              startDate: shortBackendDateFormatter(enrollmentdate),
              type: title,
              title,
              student: user,
              graduationDate: shortBackendDateFormatter(graduationdate),
              lg: 24
            }}
            setConnectionInfo={setConnectionInfo}
          />
        </div>
      ))}
      {role && (
        <div className="ControlButtons">
          <CustomButton
            buttonProps={{
              className: 'theme-outline',
              onClick: () => console.log('placeholder function')
            }}
            buttonText={t('credentialSummary.detail.proofRequest')}
            icon={<Icon type="plus" />}
          />
          <CustomButton
            buttonProps={{
              className: 'theme-secondary',
              onClick: () => console.log('placeholder function')
            }}
            buttonText={t('credentialSummary.detail.newCredential')}
            icon={<Icon type="plus" />}
          />
        </div>
      )}
    </div>
  );
};

CredentialSummaryListDetail.defaultProps = {
  credentials: []
};

CredentialSummaryListDetail.propTypes = {
  user: PropTypes.shape(credentialSummaryShape.user).isRequired,
  credentials: PropTypes.arrayOf(PropTypes.shape()),
  date: credentialSummaryShape.date.isRequired
};

export default CredentialSummaryListDetail;
