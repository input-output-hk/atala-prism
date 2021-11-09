import React from 'react';
import i18n from 'i18next';
import CellRenderer from '../../components/common/Atoms/CellRenderer/CellRenderer';
import CustomButton from '../../components/common/Atoms/CustomButton/CustomButton';
import PopOver from '../../components/common/Organisms/Detail/PopOver';
import StatusBadge from '../../components/connections/Atoms/StatusBadge/StatusBadge';
import { CONTACT_STATUS, CREDENTIAL_STATUS, CREDENTIAL_STATUSES } from '../constants';
import { backendDateFormat } from '../formatters';
import freeUniLogo from '../../images/free-uni-logo.png';

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
    render: ({ credentialData: { credentialTypeDetails } }) => (
      <img
        style={{ width: '40px', height: '40px' }}
        src={credentialTypeDetails?.icon || freeUniLogo}
        alt={`${credentialTypeDetails?.key} icon`}
      />
    )
  },
  {
    key: 'credentialType',
    // eslint-disable-next-line react/prop-types
    render: ({ credentialData: { credentialTypeDetails } }) => (
      <CellRenderer title={tp('credentialType')} value={credentialTypeDetails.name} />
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
    render: ({ contactData: { externalId } }) => (
      <CellRenderer title={tp('externalId')} value={externalId} />
    )
  }
];

export const getCredentialsIssuedColumns = (
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
    render: ({ publicationStoredAt }) => (
      <CellRenderer
        title={tp('dateSigned')}
        value={
          publicationStoredAt?.seconds
            ? backendDateFormat(publicationStoredAt?.seconds)
            : i18n.t('credentials.detail.notPublished')
        }
      />
    )
  },
  {
    key: 'contactStatus',
    // eslint-disable-next-line react/prop-types
    render: ({ contactData: { connectionStatus } }) => (
      <CellRenderer title={tp('contactStatus')}>
        <StatusBadge status={connectionStatus} useCase={CONTACT_STATUS} />
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
      const { status, credentialId } = credential;
      const loadingProps = { size: 3, color: '#F83633' };
      const actionButtons = (
        <div>
          {shouldShowRevokeButton(status) && (
            <CustomButton
              buttonProps={{
                className: 'theme-link',
                onClick: () => revokeSingleCredential(credentialId),
                disabled: !hasRequiredStatus(
                  credential,
                  credentialRequiredStatus[REVOKE_CREDENTIALS]
                )
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
                onClick: () => signSingleCredential(credentialId),
                disabled: !hasRequiredStatus(credential, credentialRequiredStatus[SIGN_CREDENTIALS])
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
                onClick: () => sendSingleCredential(credentialId),
                disabled: !hasRequiredStatus(credential, credentialRequiredStatus[SEND_CREDENTIALS])
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

export const getCredentialsReceivedColumns = (viewText, onView) => [
  ...commonColumns,
  {
    key: 'dateReceived',
    // eslint-disable-next-line react/prop-types
    render: ({ storedAt }) => (
      <CellRenderer title={tp('dateReceived')} value={backendDateFormat(storedAt?.seconds)} />
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
