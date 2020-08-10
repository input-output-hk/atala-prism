import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Collapse, Icon } from 'antd';
import { credentialSummaryShape } from '../../../../helpers/propShapes';
import {
  dayMonthYearBackendFormatter,
  shortBackendDateFormatter
} from '../../../../helpers/formatters';
import Connection from '../../../credentialSummaries/Molecules/Connection/Connection';
import CustomButton from '../../Atoms/CustomButton/CustomButton';
import CredentialDetail from './CredentialDetail';
import holderDefaultAvatar from '../../../../images/holder-default-avatar.svg';
import { withRedirector } from '../../../providers/withRedirector';
import { useSession } from '../../../providers/SessionContext';

import './_style.scss';

const CredentialListDetail = ({
  redirector: { redirectToNewCredential },
  user: { icon, name: userName },
  transactions,
  date
}) => {
  const { t } = useTranslation();

  const { session } = useSession();
  const role = session.userRole;

  const [connectionInfo, setConnectionInfo] = useState();

  const genExtra = () => <Icon type="caret-down" />;

  const hasCredentials = !!transactions.length;

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
          {date && <p>{shortBackendDateFormatter(date)}</p>}
        </div>
      </div>
      {role && hasCredentials && (
        <Collapse accordion className="TransactionsDetail">
          <Collapse.Panel
            header={t('credentialSummary.detail.transactions')}
            key="1"
            showArrow={false}
            extra={genExtra()}
          />
        </Collapse>
      )}
      {!hasCredentials && <label>{t('credentialSummary.detail.noCredentials')}</label>}
      {hasCredentials &&
        transactions.map(trans => {
          const transaction = mapTransaction(trans, setConnectionInfo);
          return (
            <div className="CredentialSummaryLine">
              <p>{t('credentialDetail.type')}</p>
              <Connection
                icon={transaction.icon}
                type={transaction.type}
                date={transaction.date}
                setConnectionInfo={transaction.setConnectionInfo}
                university={transaction.university}
                result={transaction.award}
                student={transaction.student}
                graduationDate={transaction.graduationDate}
              />
            </div>
          );
        })}
      {role && (
        <div className="ControlButtons">
          <CustomButton
            buttonProps={{
              className: 'theme-outline',
              onClick: () => {},
              disabled: true
            }}
            buttonText={t('credentialSummary.detail.proofRequest')}
            icon={<Icon type="plus" />}
          />
        </div>
      )}
    </div>
  );
};

const mapTransaction = (credential, setConnectionInfo) => {
  const { issuer, issuanceDate, credentialSubject } = credential;
  return {
    icon: null,
    type: credentialSubject.degreeAwarded,
    date: issuanceDate,
    setConnectionInfo,
    university: issuer.name,
    student: credentialSubject.name,
    graduationDate: issuanceDate,
    result: credentialSubject.degreeResult,
    lg: 24
  };
};

const getStudentName = student => {
  const names = student.namesList.join(' ');
  const surnames = student.surnamesList.join(' ');
  return `${names} ${surnames}`;
};

const getIssuerName = issuer => {
  const { issuerlegalname } = issuer;
  return issuerlegalname;
};

const getTitle = (degreeawarded, additionalspeciality) =>
  additionalspeciality ? `${degreeawarded}, ${additionalspeciality}` : degreeawarded;

CredentialListDetail.defaultProps = {
  transactions: []
};

CredentialListDetail.propTypes = {
  user: PropTypes.shape(credentialSummaryShape.user).isRequired,
  transactions: PropTypes.arrayOf(PropTypes.shape()),
  date: credentialSummaryShape.date.isRequired
};

export default withRedirector(CredentialListDetail);
