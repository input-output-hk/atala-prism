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

import './_style.scss';
import CredentialDetail from './CredentialDetail';
import holderDefaultAvatar from '../../../../images/holder-default-avatar.svg';

const CredentialListDetail = ({ user: { icon, name: userName }, transactions, date }) => {
  const { t } = useTranslation();

  const [connectionInfo, setConnectionInfo] = useState();

  const genExtra = () => <Icon type="caret-down" />;
  const role = localStorage.getItem('userRole');

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
      {transactions.map(trans => {
        const transaction = mapTransaction(trans, setConnectionInfo);
        return (
          <div className="CredentialSummaryLine">
            <p>Credential</p>
            <Connection
              icon={transaction.icon}
              type={transaction.type}
              date={transaction.date}
              setConnectionInfo={transaction.setConnectionInfo}
              university={transaction.university}
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

const mapTransaction = (credential, setConnectionInfo) => {
  const {
    degreeawarded,
    additionalspeciality,
    admissiondate,
    issuertype,
    subjectdata,
    graduationdate,
    grantingDecision
  } = credential;
  return {
    icon: null,
    type: getTitle(degreeawarded, additionalspeciality),
    date: dayMonthYearBackendFormatter(admissiondate),
    setConnectionInfo,
    university: getIssuerName(issuertype),
    student: getStudentName(subjectdata),
    graduationDate: dayMonthYearBackendFormatter(graduationdate),
    award: grantingDecision
  };
};

const getStudentName = student => {
  const names = student.namesList.join(' ');
  const surnames = student.surnamesList.join(' ');
  return `${names} ${surnames}`;
};

const getIssuerName = issuer => {
  const { issuerlegalname, academicauthority } = issuer;
  return `${issuerlegalname}, ${academicauthority}`;
};

const getTitle = (degreeawarded, additionalspeciality) =>
  `${degreeawarded}, ${additionalspeciality}`;

CredentialListDetail.defaultProps = {
  transactions: []
};

CredentialListDetail.propTypes = {
  user: PropTypes.shape(credentialSummaryShape.user).isRequired,
  transactions: PropTypes.arrayOf(PropTypes.shape()),
  date: credentialSummaryShape.date.isRequired
};

export default CredentialListDetail;
