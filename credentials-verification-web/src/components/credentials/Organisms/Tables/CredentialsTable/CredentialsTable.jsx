import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CellRenderer from '../../../../common/Atoms/CellRenderer/CellRenderer';
import { dayMonthYearBackendFormatter } from '../../../../../helpers/formatters';
import InfiniteScrollTable from '../../../../common/Organisms/Tables/InfiniteScrollTable';
import freeUniLogo from '../../../../../images/free-uni-logo.png';
import CustomButton from '../../../../common/Atoms/CustomButton/CustomButton';
import { credentialShape } from '../../../../../helpers/propShapes';
import StatusBadge from '../../../../connections/Atoms/StatusBadge/StatusBadge';
import PopOver from '../../../../common/Organisms/Detail/PopOver';
import {
  CREDENTIAL_STATUSES,
  CREDENTIALS_ISSUED,
  CREDENTIALS_RECIEVED,
  CONTACT_STATUS,
  CREDENTIAL_STATUS
} from '../../../../../helpers/constants';
import './_style.scss';

const commonColumns = [
  {
    key: 'icon',
    render: ({ credentialType }) => (
      <img
        style={{ width: '40px', height: '40px' }}
        src={credentialType?.logo || freeUniLogo}
        alt={`${credentialType?.key} icon`}
      />
    )
  },
  {
    key: 'credentialType',
    render: ({ credentialType }) => (
      <CellRenderer
        title="credentialType"
        value={credentialType?.name}
        componentName="credentials"
      />
    )
  },
  {
    key: 'contactName',
    render: ({ contactData }) => (
      <CellRenderer
        title="contactName"
        value={contactData.contactName}
        componentName="credentials"
      />
    )
  },
  {
    key: 'externalId',
    render: ({ contactData }) => (
      <CellRenderer title="externalId" value={contactData.externalid} componentName="credentials" />
    )
  }
];

const getCredentialsIssuedColumns = (
  viewText,
  signText,
  sendText,
  onView,
  signSingleCredential,
  sendSingleCredential,
  loadingSignSingle,
  loadingSendSingle
) => [
  ...commonColumns,
  {
    key: 'creationDate',
    render: ({ contactData }) => (
      <CellRenderer
        title="creationDate"
        value={dayMonthYearBackendFormatter(contactData.creationDate)}
        componentName="credentials"
      />
    )
  },
  {
    key: 'contactStatus',
    render: ({ contactData: { status } }) => (
      <CellRenderer title="contactStatus" componentName="credentials">
        <StatusBadge status={status} useCase={CONTACT_STATUS} />
      </CellRenderer>
    )
  },
  {
    key: 'credentialStatus',
    render: ({ status }) => (
      <CellRenderer title="credentialStatus" componentName="credentials">
        <StatusBadge status={status} useCase={CREDENTIAL_STATUS} />
      </CellRenderer>
    )
  },
  {
    key: 'actions',
    render: ({ status, credentialid }) => {
      const actionButtons = (
        <div>
          {status === CREDENTIAL_STATUSES.credentialDraft && (
            <CustomButton
              buttonProps={{
                className: 'theme-link',
                onClick: () => signSingleCredential(credentialid)
              }}
              buttonText={signText}
              loading={loadingSignSingle}
              loadingProps={{ size: 3, color: '#F83633' }}
            />
          )}
          {status === CREDENTIAL_STATUSES.credentialSigned && (
            <CustomButton
              buttonProps={{
                className: 'theme-link',
                onClick: () => sendSingleCredential(credentialid)
              }}
              buttonText={sendText}
              loading={loadingSendSingle}
              loadingProps={{ size: 3, color: '#F83633' }}
            />
          )}
          <CustomButton
            buttonProps={{
              className: 'theme-link',
              onClick: onView
            }}
            buttonText={viewText}
          />
        </div>
      );
      return <PopOver content={actionButtons} />;
    }
  }
];

const getCredentialsRecievedColumns = (viewText, onView) => [
  ...commonColumns,
  {
    key: 'actions',
    render: ({ status, credentialid }) => {
      const actionButtons = (
        <div>
          <CustomButton
            buttonProps={{
              className: 'theme-link',
              onClick: onView
            }}
            buttonText={viewText}
          />
        </div>
      );
      return <PopOver content={actionButtons} />;
    }
  }
];

const CredentialsTable = ({
  credentials,
  loading,
  getMoreData,
  hasMore,
  onView,
  signSingleCredential,
  sendSingleCredential,
  selectionType,
  tab
}) => {
  const { t } = useTranslation();
  const [loadingSignSingle, setLoadingSignSingle] = useState();
  const [loadingSendSingle, setLoadingSendSingle] = useState();

  const wrapSignSingleCredential = async credentialid => {
    setLoadingSignSingle(true);
    await signSingleCredential(credentialid);
    setLoadingSignSingle(false);
  };

  const wrapSendSingleCredential = async credentialid => {
    setLoadingSendSingle(true);
    await sendSingleCredential(credentialid);
    setLoadingSendSingle(false);
  };

  const columns = {
    [CREDENTIALS_ISSUED]: getCredentialsIssuedColumns(
      t('actions.view'),
      t('credentials.actions.signOneCredential'),
      t('credentials.actions.sendOneCredential'),
      onView,
      wrapSignSingleCredential,
      wrapSendSingleCredential,
      loadingSignSingle,
      loadingSendSingle
    ),
    [CREDENTIALS_RECIEVED]: getCredentialsRecievedColumns(t('actions.view'), onView)
  };

  return (
    <div className="CredentialsTable">
      <InfiniteScrollTable
        columns={columns[tab]}
        data={credentials}
        loading={loading}
        getMoreData={getMoreData}
        hasMore={hasMore}
        rowKey="credentialid"
        selectionType={selectionType}
      />
    </div>
  );
};

CredentialsTable.defaultProps = {
  credentials: [],
  selectionType: null
};

CredentialsTable.propTypes = {
  credentials: PropTypes.arrayOf(PropTypes.shape(credentialShape)),
  getMoreData: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  hasMore: PropTypes.bool.isRequired,
  onView: PropTypes.func.isRequired,
  signSingleCredential: PropTypes.func.isRequired,
  sendSingleCredential: PropTypes.func.isRequired,
  selectionType: PropTypes.shape({
    selectedRowKeys: PropTypes.arrayOf(PropTypes.string),
    onChange: PropTypes.func
  }),
  tab: PropTypes.oneOf([CREDENTIALS_ISSUED, CREDENTIALS_RECIEVED]).isRequired
};

export default CredentialsTable;
