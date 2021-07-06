/* eslint-disable react/prop-types */
import React from 'react';
import i18n from 'i18next';
import PopOver from '../../components/common/Organisms/Detail/PopOver';
import CellRenderer from '../../components/common/Atoms/CellRenderer/CellRenderer';
import StatusBadge from '../../components/connections/Atoms/StatusBadge/StatusBadge';
import holderDefaultAvatar from '../../images/holder-default-avatar.svg';
import ActionButtons from '../../components/connections/Atoms/ActionButtons/ActionButtons';
import ActionGroupButtons from '../../components/groupEditing/ActionButtons';
import { backendDateFormat } from '../formatters';

const translationKeyPrefix = 'contacts.table.columns';

const tp = chain => i18n.t(`${translationKeyPrefix}.${chain}`);

const getBasicContactColumns = () => [
  {
    key: 'avatar',
    width: 45,
    render: ({ avatar }) => (
      <img
        style={{ width: '40px', height: '40px' }}
        src={avatar || holderDefaultAvatar}
        alt="Avatar"
      />
    )
  },
  {
    key: 'contactName',
    width: 250,
    render: ({ contactName }) => <CellRenderer title={tp('contactName')} value={contactName} />
  },
  {
    key: 'externalId',
    width: 170,
    render: ({ externalId }) => <CellRenderer title={tp('externalId')} value={externalId} />
  },
  {
    key: 'creationDate',
    render: ({ createdAt }) => (
      <CellRenderer title={tp('creationDate')} value={backendDateFormat(createdAt?.seconds)} />
    )
  }
];

const getExtendedColumns = ({ inviteContact, viewContactDetail }) => [
  {
    key: 'numberOfCredentialsCreated',
    render: ({ numberOfCredentialsCreated }) => (
      <CellRenderer
        title={tp('numberOfCredentialsCreated')}
        componentName="contacts"
        value={numberOfCredentialsCreated}
      />
    )
  },
  {
    key: 'numberOfCredentialsReceived',
    render: ({ numberOfCredentialsReceived }) => (
      <CellRenderer
        title={tp('numberOfCredentialsReceived')}
        componentName="contacts"
        value={numberOfCredentialsReceived}
      />
    )
  },
  {
    key: 'connectionStatus',
    render: ({ connectionStatus }) => (
      <CellRenderer title={tp('contactStatus')} componentName="contacts">
        <StatusBadge status={connectionStatus} />
      </CellRenderer>
    )
  },
  {
    key: 'actions',
    width: 200,
    render: contact => {
      const actionButtons = (
        <ActionButtons
          contact={contact}
          inviteContact={inviteContact}
          viewContactDetail={viewContactDetail}
        />
      );
      return <PopOver content={actionButtons} />;
    }
  }
];

const getSpecialGroupContactsColumns = onDelete => [
  {
    key: 'actions',
    width: 150,
    render: contact => (
      <PopOver content={<ActionGroupButtons contact={contact} onDelete={onDelete} />} />
    )
  }
];

export const getContactColumns = ({ inviteContact, viewContactDetail }) => {
  const basicColumns = getBasicContactColumns();
  if (!viewContactDetail) return basicColumns;
  return basicColumns.concat(getExtendedColumns({ viewContactDetail, inviteContact }));
};

export const getGroupContactColumns = onDelete =>
  getBasicContactColumns().concat(getSpecialGroupContactsColumns(onDelete));
