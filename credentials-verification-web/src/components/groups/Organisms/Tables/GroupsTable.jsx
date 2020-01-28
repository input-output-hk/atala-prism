import React, { Fragment, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, message } from 'antd';
import { Link } from 'react-router-dom';
import PropTypes from 'prop-types';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import { shortBackendDateFormatter } from '../../../../helpers/formatters';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';

import './_style.scss';

const GetActionsButtons = ({ id, setGroupToDelete, fullInfo }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      {fullInfo && (
        <Fragment>
          <CustomButton
            buttonProps={{
              onClick: setGroupToDelete,
              className: 'theme-link',
              disabled: true
            }}
            buttonText={t('groups.table.buttons.delete')}
          />
          <CustomButton
            buttonProps={{
              onClick: () => message.info(`The id to copy is ${id}`, 1),
              className: 'theme-link',
              disabled: true
            }}
            buttonText={t('groups.table.buttons.copy')}
          />
        </Fragment>
      )}
      <Link disabled to={`group/${id}`}>
        {t('groups.table.buttons.view')}
      </Link>
    </div>
  );
};

GetActionsButtons.propTypes = {
  id: PropTypes.string.isRequired,
  setGroupToDelete: PropTypes.func.isRequired,
  fullInfo: PropTypes.bool.isRequired
};

const AddCredentialsButton = ({ id }) => {
  const { t } = useTranslation();

  return (
    <Button onClick={() => message.info('add with this id', id)}>
      {t('groups.table.buttons.addCredential')}
    </Button>
  );
};

AddCredentialsButton.propTypes = {
  id: PropTypes.string.isRequired
};

const getColumns = ({ setGroupToDelete, setGroup }) => {
  const componentName = 'groups';
  const fullInfo = !setGroup;

  const actionColumn = {
    key: 'actions',
    width: 300,
    render: ({ groupId, groupName }) => (
      <GetActionsButtons
        id={groupId}
        setGroupToDelete={() => setGroupToDelete({ id: groupId, groupName })}
        fullInfo={fullInfo}
      />
    )
  };

  const nameColumn = {
    key: 'groupName',
    width: 300,
    render: ({ name }) => (
      <CellRenderer title="groupName" componentName={componentName} value="" firstValue={name} />
    )
  };

  return [nameColumn, actionColumn];
};

const getSelectedIndexArray = ({ name }, groups) => {
  if (!name) return [];

  const selectedIndex = groups.map(({ name: groupName }) => groupName).indexOf(name);

  return [selectedIndex];
};

const GroupsTable = ({ setGroupToDelete, groups, selectedGroup, setGroup, onPageChange }) => {
  const [loading, setLoading] = useState(false);
  const selectedRowKeys = getSelectedIndexArray(selectedGroup, groups);
  // const selectedRowKeys = selectedRows === -1 ? [] : [selectedRows];

  const getMoreData = () => {
    setLoading(true);
    return onPageChange().finally(() => setLoading(false));
  };

  const tableProps = {
    columns: getColumns({ setGroupToDelete, setGroup }),
    data: groups,
    selectionType: !setGroup
      ? null
      : {
          selectedRowKeys,
          type: 'radio',
          onChange: (_index, [selected]) => setGroup(selected)
        },
    loading,
    hasMore: false,
    getMoreData
  };

  return (
    <div className="GroupTableContainer">
      <InfiniteScrollTable {...tableProps} />
    </div>
  );
};

GroupsTable.defaultProps = {
  groups: [],
  selectedGroup: '',
  setGroup: null
};

GroupsTable.propTypes = {
  setGroupToDelete: PropTypes.func.isRequired,
  groups: PropTypes.arrayOf(PropTypes.object),
  onPageChange: PropTypes.func.isRequired,
  selectedGroup: PropTypes.string,
  setGroup: PropTypes.func,
  hasMore: PropTypes.func.isRequired
};

export default GroupsTable;
