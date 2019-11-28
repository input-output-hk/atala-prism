import React, { Fragment, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, message, Radio } from 'antd';
import { Link } from 'react-router-dom';
import PropTypes from 'prop-types';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import { shortBackendDateFormatter } from '../../../../helpers/formatters';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';

const GetActionsButtons = ({ id, setGroupToDelete, fullInfo }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      {fullInfo && (
        <Fragment>
          <CustomButton
            buttonProps={{
              onClick: setGroupToDelete,
              className: 'theme-link'
            }}
            buttonText={t('groups.table.buttons.delete')}
          />
          <CustomButton
            buttonProps={{
              onClick: () => message.info(`The id to copy is ${id}`, 1),
              className: 'theme-link'
            }}
            buttonText={t('groups.table.buttons.copy')}
          />
        </Fragment>
      )}
      <Link to={`group/${id}`}>{t('groups.table.buttons.view')}</Link>
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

const getColumns = ({ setGroupToDelete, fullInfo, selectedGroup, setGroup, openModal }) => {
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
    : [
        {
          key: 'checked',
          render: group => (
            <Radio
              checked={group.groupId === selectedGroup.groupId}
              onClick={() => setGroup(group)}
            />
          )
        }
      ];

  const actionColumn = [
    {
      key: 'actions',
      render: ({ groupId, groupName }) => (
        <GetActionsButtons
          id={groupId}
          setGroupToDelete={() => setGroupToDelete({ id: groupId, groupName })}
          fullInfo={fullInfo}
        />
      )
    }
  ];

  const commonColumns = [
    {
      key: 'icon',
      render: ({ icon, groupName }) => (
        <img style={{ height: '40px', width: '40px' }} src={icon} alt={`${groupName} icon`} />
      )
    },
    {
      key: 'groupName',
      render: ({ groupName }) => (
        <CellRenderer
          title="groupName"
          componentName={componentName}
          value=""
          firstValue={groupName}
        />
      )
    },
    {
      key: 'lastUpdate',
      render: ({ lastUpdate }) => (
        <CellRenderer
          title="lastUpdate"
          componentName={componentName}
          value={shortBackendDateFormatter(lastUpdate)}
        />
      )
    }
  ];

  return commonColumns.concat(credentialColumns).concat(actionColumn);
};

const GroupsTable = ({
  setGroupToDelete,
  groups,
  selectedGroup,
  setGroup,
  onPageChange,
  fullInfo,
  hasMore
}) => {
  const [loading, setLoading] = useState(false);
  const selectedRows = groups.map(({ groupId }) => groupId).indexOf(selectedGroup.groupId);
  const selectedRowKeys = selectedRows === -1 ? [] : [selectedRows];

  const getMoreData = () => {
    setLoading(true);
    onPageChange();
    setLoading(false);
  };

  const tableProps = {
    columns: getColumns({ setGroupToDelete, fullInfo, selectedGroup, setGroup }),
    data: groups,
    selectionType: fullInfo
      ? null
      : {
          selectedRowKeys,
          type: 'radio',
          onChange: (_index, selected) => setGroup(selected[0])
        },
    loading,
    hasMore,
    getMoreData
  };

  return <InfiniteScrollTable {...tableProps} />;
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
  fullInfo: PropTypes.bool.isRequired,
  hasMore: PropTypes.func.isRequired
};

export default GroupsTable;
