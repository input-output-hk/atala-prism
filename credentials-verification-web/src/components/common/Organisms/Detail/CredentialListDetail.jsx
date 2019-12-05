import React, { Fragment, useState } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Collapse, Drawer, Icon } from 'antd';
import { credentialSummaryShape } from '../../../../helpers/propShapes';
import { shortBackendDateFormatter } from '../../../../helpers/formatters';
import Connection from '../../../credentialSummaries/Molecules/Connection/Connection';
import CustomButton from '../../Atoms/CustomButton/CustomButton';
import { drawerWidth } from '../../../../helpers/constants';
import CredentialData from '../../Atoms/CredentialData/CredentialData';

import './_style.scss';
import CredentialDetail from './CredentialDetail';
import holderDefaultAvatar from '../../../../images/holder-default-avatar.svg';

const CredentialListDetail = ({ user: { icon, name: userName }, transactions, date }) => {
  const { t } = useTranslation();

  const [connectionInfo, setConnectionInfo] = useState();

  const genExtra = () => <Icon type="caret-down" />;
  const role = localStorage.getItem('role');

  return (
    <div className="CredentialSummaryDetail">
      <CredentialDetail
        drawerInfo={{
          title: t('credentials.detail.title'),
          onClose: () => setConnectionInfo(null),
          visible: !!connectionInfo
        }}
        credentialData={connectionInfo}
      />
      <div className="CredentialSummaryHeader">
        <img src={icon || holderDefaultAvatar} alt={t('credentialSummary.detail.alt', userName)} />
        <div className="CredentialSummaryUser">
          <h3>{userName}</h3>
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
      {transactions.map(trans => (
        <div className="CredentialSummaryLine">
          <p>Credential</p>
          <Connection {...trans} setConnectionInfo={setConnectionInfo} />
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

CredentialListDetail.defaultProps = {
  transactions: []
};

CredentialListDetail.propTypes = {
  user: PropTypes.shape(credentialSummaryShape.user).isRequired,
  transactions: PropTypes.arrayOf(PropTypes.shape()),
  date: credentialSummaryShape.date.isRequired
};

export default CredentialListDetail;
