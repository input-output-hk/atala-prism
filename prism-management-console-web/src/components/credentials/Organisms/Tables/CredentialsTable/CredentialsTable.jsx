import React, { useState } from 'react';
import i18n from 'i18next';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CellRenderer from '../../../../common/Atoms/CellRenderer/CellRenderer';
import { backendDateFormat } from '../../../../../helpers/formatters';
import InfiniteScrollTable from '../../../../common/Organisms/Tables/InfiniteScrollTable';
import freeUniLogo from '../../../../../images/free-uni-logo.png';
import CustomButton from '../../../../common/Atoms/CustomButton/CustomButton';
import { credentialShape } from '../../../../../helpers/propShapes';
import StatusBadge from '../../../../connections/Atoms/StatusBadge/StatusBadge';
import PopOver from '../../../../common/Organisms/Detail/PopOver';
import {
  CREDENTIAL_STATUSES,
  CREDENTIALS_ISSUED,
  CREDENTIALS_RECEIVED,
  CONTACT_STATUS,
  CREDENTIAL_STATUS,
  CONNECTION_STATUSES
} from '../../../../../helpers/constants';
import './_style.scss';

const translationKeyPrefix = 'credentials.table.columns';

const tp = chain => i18n.t(`${translationKeyPrefix}.${chain}`);

const shouldShowRevokeButton = status =>
  status === CREDENTIAL_STATUSES.credentialSigned || status === CREDENTIAL_STATUSES.credentialSent;

const shouldShowSignButton = status => status === CREDENTIAL_STATUSES.credentialDraft;

const shouldShowSendButton = status => status === CREDENTIAL_STATUSES.credentialSigned;

const commonColumns = [
  {
    key: 'icon',
    // eslint-disable-next-line react/prop-types
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
    // eslint-disable-next-line react/prop-types
    render: ({ credentialType }) => (
      <CellRenderer title={tp('credentialType')} value={i18n.t(credentialType?.name)} />
    )
  },
  {
    key: 'contactName',
    // eslint-disable-next-line react/prop-types
    render: ({ contactData: { contactName } }) => (
      <CellRenderer title={tp('contactName')} value={contactName} />
    )
  },
  {
    key: 'externalId',
    // eslint-disable-next-line react/prop-types
    render: ({ contactData: { externalid } }) => (
      <CellRenderer title={tp('externalId')} value={externalid} />
    )
  }
];

const getCredentialsIssuedColumns = (
  onView,
  revokeSingleCredential,
  signSingleCredential,
  sendSingleCredential,
  loadingRevokeSingle,
  loadingSignSingle,
  loadingSendSingle
) => [
  ...commonColumns,
  {
    key: 'dateSigned',
    // eslint-disable-next-line react/prop-types
    render: ({ publicationstoredat }) => (
      <CellRenderer
        title={tp('dateSigned')}
        value={
          publicationstoredat?.seconds
            ? backendDateFormat(publicationstoredat?.seconds)
            : i18n.t('credentials.detail.notPublished')
        }
      />
    )
  },
  {
    key: 'contactStatus',
    // eslint-disable-next-line react/prop-types
    render: ({ contactData: { status } }) => (
      <CellRenderer title={tp('contactStatus')}>
        <StatusBadge status={status} useCase={CONTACT_STATUS} />
      </CellRenderer>
    )
  },
  {
    key: 'credentialStatus',
    // eslint-disable-next-line react/prop-types
    render: ({ status }) => (
      <CellRenderer title={tp('credentialStatus')}>
        <StatusBadge status={status} useCase={CREDENTIAL_STATUS} />
      </CellRenderer>
    )
  },
  {
    key: 'actions',
    render: credential => {
      const { status, credentialid, contactData } = credential;
      const loadingProps = { size: 3, color: '#F83633' };
      const actionButtons = (
        <div>
          {shouldShowRevokeButton(status) && (
            <CustomButton
              buttonProps={{
                className: 'theme-link',
                onClick: () => revokeSingleCredential(credentialid)
              }}
              buttonText={i18n.t('credentials.actions.revokeOneCredential')}
              loading={loadingRevokeSingle}
              loadingProps={loadingProps}
            />
          )}
          {shouldShowSignButton(status) && (
            <CustomButton
              buttonProps={{
                className: 'theme-link',
                onClick: () => signSingleCredential(credentialid)
              }}
              buttonText={i18n.t('credentials.actions.signOneCredential')}
              loading={loadingSignSingle}
              loadingProps={{ size: 3, color: '#F83633' }}
            />
          )}
          {shouldShowSendButton(status) && (
            <CustomButton
              buttonProps={{
                className: 'theme-link',
                onClick: () => sendSingleCredential(credentialid),
                disabled: contactData.status !== CONNECTION_STATUSES.statusConnectionAccepted
              }}
              buttonText={i18n.t('credentials.actions.sendOneCredential')}
              loading={loadingSendSingle}
              loadingProps={{ size: 3, color: '#F83633' }}
            />
          )}
          <CustomButton
            buttonProps={{
              className: 'theme-link',
              onClick: () => onView(credential)
            }}
            buttonText={i18n.t('actions.view')}
          />
        </div>
      );
      return <PopOver content={actionButtons} />;
    }
  }
];

const getCredentialsReceivedColumns = (viewText, onView) => [
  ...commonColumns,
  {
    key: 'dateReceived',
    // eslint-disable-next-line react/prop-types
    render: ({ storedat }) => (
      <CellRenderer title={tp('dateReceived')} value={backendDateFormat(storedat?.seconds)} />
    )
  },
  {
    key: 'actions',
    render: credential => {
      const actionButtons = (
        <div>
          <CustomButton
            buttonProps={{
              className: 'theme-link',
              onClick: () => onView(credential)
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
  revokeSingleCredential,
  signSingleCredential,
  sendSingleCredential,
  selectionType,
  tab
}) => {
  const { t } = useTranslation();
  const [loadingRevokeSingle, setLoadingRevokeSingle] = useState();
  const [loadingSignSingle, setLoadingSignSingle] = useState();
  const [loadingSendSingle, setLoadingSendSingle] = useState();

  const wrapRevokeSingleCredential = async credentialid => {
    setLoadingRevokeSingle(true);
    await revokeSingleCredential(credentialid);
    setLoadingRevokeSingle(false);
  };

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
      onView,
      wrapRevokeSingleCredential,
      wrapSignSingleCredential,
      wrapSendSingleCredential,
      loadingRevokeSingle,
      loadingSignSingle,
      loadingSendSingle
    ),
    [CREDENTIALS_RECEIVED]: getCredentialsReceivedColumns(t('actions.view'), onView)
  };

  const tableClassName = {
    [CREDENTIALS_ISSUED]: 'credentialsIssued',
    [CREDENTIALS_RECEIVED]: 'credentialsReceived'
  };

  return (
    <div className={`CredentialsTable ${tableClassName[tab]}`}>
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
  revokeSingleCredential: PropTypes.func.isRequired,
  signSingleCredential: PropTypes.func.isRequired,
  sendSingleCredential: PropTypes.func.isRequired,
  selectionType: PropTypes.shape({
    selectedRowKeys: PropTypes.arrayOf(PropTypes.string),
    onChange: PropTypes.func
  }),
  tab: PropTypes.oneOf([CREDENTIALS_ISSUED, CREDENTIALS_RECEIVED]).isRequired
};

export default CredentialsTable;
