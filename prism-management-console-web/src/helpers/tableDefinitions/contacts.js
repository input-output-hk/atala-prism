/* eslint-disable react/prop-types */
import React from 'react';
import i18n from 'i18next';
import PopOver from '../../components/common/Organisms/Detail/PopOver';
import CellRenderer from '../../components/common/Atoms/CellRenderer/CellRenderer';
import StatusBadge from '../../components/connections/Atoms/StatusBadge/StatusBadge';
import holderDefaultAvatar from '../../images/holder-default-avatar.svg';
import ActionButtons from '../../components/connections/Atoms/ActionButtons/ActionButtons';
import ActionGroupButtons from '../../components/groupEditing/ActionButtons';
import { contactBackendDateFormat } from '../formatters';

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
    key: 'externalid',
    width: 170,
    render: ({ externalid }) => <CellRenderer title={tp('externalid')} value={externalid} />
  },
  {
    key: 'creationDate',
    render: ({ createdat }) => (
      <CellRenderer title={tp('creationDate')} value={contactBackendDateFormat(createdat)} />
    )
  }
];

const getExtendedColumns = ({ inviteContact, viewContactDetail }) => [
  {
    key: 'connectionstatus',
    render: ({ status }) => (
      <CellRenderer title={tp('contactStatus')} componentName="contacts">
        <StatusBadge status={status} />
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
