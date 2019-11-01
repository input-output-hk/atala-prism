import React, { Fragment } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, message } from 'antd';
import { Link } from 'react-router-dom';
import PropTypes from 'prop-types';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import { shortDateFormatter } from '../../../../helpers/formatters';
import PaginatedTable from '../../../common/Organisms/Tables/PaginatedTable';
import { GROUP_PAGE_SIZE } from '../../../../helpers/constants';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

const GetActionsButtons = ({ id, setGroupToDelete, fullInfo }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      {fullInfo && (
        <Fragment>
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
        </Fragment>
      )}
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

const commonColumns = componentName => [
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
  }
];

const getColumns = (openModal, setGroupToDelete, fullInfo) => {
  const componentName = 'groups';
  const credentialColumns = fullInfo
    ? [
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
        }
      ]
    : [];

  const actionColumn = [
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
          onlyView={fullInfo}
        />
      )
    }
  ];

  return commonColumns(componentName)
    .concat(credentialColumns)
    .concat(actionColumn);
};

const GroupsTable = ({
  setOpen,
  setGroupToDelete,
  groups,
  selectedGroup,
  setGroup,
  current,
  total,
  onPageChange,
  fullInfo
}) => {
  const selectedRows = groups.map(({ groupId }) => groupId).indexOf(selectedGroup);
  const selectedRowKeys = selectedRows === -1 ? [] : [selectedRows];

  const tableProps = {
    columns: getColumns(setOpen, setGroupToDelete, fullInfo),
    data: groups,
    current,
    total,
    defaultPageSize: GROUP_PAGE_SIZE,
    onChange: onPageChange,
    selectionType: fullInfo
      ? null
      : {
          selectedRowKeys,
          type: 'radio',
          onChange: (_index, selected) => setGroup(selected[0].groupId)
        }
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
