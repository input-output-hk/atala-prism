import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Collapse, Icon } from 'antd';
import { credentialSummaryShape } from '../../../../helpers/propShapes';
import { shortDateFormatter } from '../../../../helpers/formatters';
import Connection from '../../Molecules/Connection/Connection';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import '../../../common/Organisms/Detail/_style.scss';

const CredentialSummaryDetail = ({ user: { icon, name: userName, transactions }, date }) => {
  const { t } = useTranslation();

  const genExtra = () => <Icon type="caret-down" />;

  return (
    <div className="CredentialSummaryDetail">
      <div className="CredentialSummaryHeader">
        <img src={icon} alt={t('credentialSummary.detail.alt', userName)} />
        <div className="CredentialSummaryUser">
          <h3>{userName}</h3>
          <p>{shortDateFormatter(date)}</p>
        </div>
      </div>
      <Collapse accordion className="TransactionsDetail">
        <Collapse.Panel
          header={t('credentialSummary.detail.transactions')}
          key="1"
          showArrow={false}
          extra={genExtra()}
        />
      </Collapse>
      {transactions &&
        transactions.map(trans => (
          <div className="CredentialSummaryLine">
            <p>Credential</p>
            <Connection {...trans} />
          </div>
        ))}
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
    </div>
  );
};

CredentialSummaryDetail.propTypes = {
  user: PropTypes.shape(credentialSummaryShape.user).isRequired,
  date: credentialSummaryShape.date.isRequired
};

export default CredentialSummaryDetail;
