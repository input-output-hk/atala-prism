import React from 'react';
import { useTranslation } from 'react-i18next';
import { Button, message } from 'antd';
import { Link } from 'react-router-dom';
import PropTypes from 'prop-types';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import { shortDateFormatter } from '../../../../helpers/formatters';
import PaginatedTable from '../../../common/Organisms/Tables/PaginatedTable';
import { GROUP_PAGE_SIZE } from '../../../../helpers/constants';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

const GetActionsButtons = ({ id, setGroupToDelete }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      <CustomButton
        onClick={setGroupToDelete}
        buttonText={t('groups.table.buttons.delete')}
        theme="theme-link"
      />
      <CustomButton
        onClick={() => message.info(`The id to copy is ${id}`, 1)}
        buttonText={t('groups.table.buttons.copy')}
        theme="theme-link"
      />
      <Link to={`group/${id}`}>{t('groups.table.buttons.view')}</Link>
    </div>
  );
};

GetActionsButtons.propTypes = {
  id: PropTypes.string.isRequired,
  setGroupToDelete: PropTypes.func.isRequired
};

const AddCredentialsButton = ({ id }) => {
  const { t } = useTranslation();
  return (
    <Button onClick={() => console.log('add with this id', id)}>
      {t('groups.table.buttons.addCredential')}
    </Button>
  );
};

AddCredentialsButton.propTypes = {
  id: PropTypes.string.isRequired
};

const getColumns = (openModal, setGroupToDelete) => {
  const componentName = 'groups';

  return [
    {
      key: 'icon',
      render: ({ icon, groupName }) => (
        <img style={{ height: '40px', width: '40px' }} src={icon} alt={`${groupName} icon`} />
      )
    },
    {
      key: 'groupName',
      render: ({ groupName }) => (
        <CellRenderer title="groupName" componentName={componentName} value={groupName} />
      )
    },
    {
      key: 'lastUpdate',
      render: ({ lastUpdate }) => (
        <CellRenderer
          title="lastUpdate"
          componentName={componentName}
          value={shortDateFormatter(lastUpdate)}
        />
      )
    },
    {
      key: 'credential',
      render: ({ credential, groupId }) =>
        credential ? (
          <CellRenderer
            title="credential"
            componentName={componentName}
            value={credential.credentialName}
          />
        ) : (
          <AddCredentialsButton id={groupId} />
        )
    },
    {
      key: 'actions',
      fixed: 'right',
      render: ({ groupId, groupName }) => (
        <GetActionsButtons
          id={groupId}
          setGroupToDelete={() => {
            openModal(true);
            setGroupToDelete({ id: groupId, groupName });
          }}
        />
      )
    }
  ];
};

const GroupsTable = ({ setOpen, setGroupToDelete, groups, current, total, onPageChange }) => {
  const tableProps = {
    columns: getColumns(setOpen, setGroupToDelete),
    data: groups,
    current,
    total,
    defaultPageSize: GROUP_PAGE_SIZE,
    onChange: onPageChange
  };

  return <PaginatedTable {...tableProps} />;
};

GroupsTable.defaultProps = {
  groups: [],
  current: 0,
  total: 0
};

GroupsTable.propTypes = {
  setOpen: PropTypes.func.isRequired,
  setGroupToDelete: PropTypes.func.isRequired,
  groups: PropTypes.arrayOf(PropTypes.object),
  current: PropTypes.number,
  total: PropTypes.number,
  onPageChange: PropTypes.func.isRequired
};

export default GroupsTable;
