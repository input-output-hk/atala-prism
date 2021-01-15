/* eslint-disable react/prop-types */
import React from 'react';
import i18n from 'i18next';
import CellRenderer from '../../components/common/Atoms/CellRenderer/CellRenderer';
import ActionButtons from '../../components/groups/Molecules/ActionButtons/ActionButtons';
import { ReactComponent as GroupIcon } from '../../images/icon-groups.svg';
import { backendDateFormat } from '../formatters';

const translationKeyPrefix = 'groups.table.columns';

const tp = chain => i18n.t(`${translationKeyPrefix}.${chain}`);

const baseColumns = [
  {
    key: 'icon',
    width: 25,
    render: () => <GroupIcon />
  },
  {
    key: 'groupName',
    width: 340,
    render: ({ name }) => <CellRenderer title={tp('groupName')} value={name} />
  },
  {
    key: 'createdAt',
    align: 'left',
    render: ({ createdat }) => (
      <CellRenderer title={tp('createdAt')} value={backendDateFormat(createdat)} />
    )
  },
  {
    key: 'numberOfContacts',
    width: 100,
    align: 'right',
    render: ({ numberofcontacts }) => (
      <CellRenderer title={tp('numberOfContacts')} value={numberofcontacts} light />
    )
  }
];

export const getGroupColumns = ({ onCopy, setGroupToDelete, setGroup }) => {
  const fullInfo = !setGroup;

  const actionColumn = {
    key: 'actions',
    width: 150,
    align: 'right',
    render: group => (
      <ActionButtons
        id={group.id}
        setGroupToDelete={() => setGroupToDelete(group)}
        onCopy={() => onCopy(group)}
        fullInfo={fullInfo}
      />
    )
  };

  return setGroupToDelete ? [...baseColumns, actionColumn] : baseColumns;
};
