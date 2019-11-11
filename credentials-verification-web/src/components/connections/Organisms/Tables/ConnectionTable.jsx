import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import { shortDateFormatter } from '../../../../helpers/formatters';
import PaginatedTable from '../../../common/Organisms/Tables/PaginatedTable';
import { CONNECTION_PAGE_SIZE, AVATAR_WIDTH } from '../../../../helpers/constants';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { connectionShape } from '../../../../helpers/propShapes';

const GetActionsButtons = ({ connection, setCurrentConnection, openDrawer }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      <CustomButton
        onClick={setCurrentConnection}
        buttonText={t('connections.table.buttons.delete')}
        theme="theme-link"
      />
      <CustomButton
        onClick={() => {
          setCurrentConnection(connection);
          openDrawer();
        }}
        theme="theme-link"
        buttonText={t('connections.table.buttons.view')}
      />
    </div>
  );
};

GetActionsButtons.propTypes = {
  connection: PropTypes.shape(connectionShape).isRequired,
  setCurrentConnection: PropTypes.func.isRequired,
  openDrawer: PropTypes.func.isRequired
};

const getColumns = (setCurrentConnection, openDrawer) => {
  const componentName = 'connections';

  const actionsWidth = 250;
  return [
    {
      key: 'icon',
      width: AVATAR_WIDTH,
      render: ({ user: { icon, name } }) => (
        <img style={{ height: '40px', width: '40px' }} src={icon} alt={`${name} icon`} />
      )
    },
    {
      key: 'name',
      render: ({ user: { name } }) => (
        <CellRenderer title="name" componentName={componentName} value={name} />
      )
    },
    {
      key: 'date',
      render: ({ date }) => (
        <CellRenderer title="date" componentName={componentName} value={shortDateFormatter(date)} />
      )
    },
    {
      key: 'totalCredentials',
      render: ({ user: { transactions } }) => (
        <CellRenderer
          title="totalCredentials"
          componentName={componentName}
          value={transactions.length}
        />
      )
    },
    {
      key: 'actions',
      width: actionsWidth,
      render: connection => (
        <GetActionsButtons
          connection={connection}
          setCurrentConnection={setCurrentConnection}
          openDrawer={openDrawer}
        />
      )
    }
  ];
};

const ConnectionTable = ({
  setCurrentConnection,
  connections,
  current,
  total,
  onPageChange,
  openDrawer
}) => {
  const tableProps = {
    columns: getColumns(setCurrentConnection, openDrawer),
    data: connections,
    current,
    total,
    defaultPageSize: CONNECTION_PAGE_SIZE,
    onChange: onPageChange
  };

  return <PaginatedTable {...tableProps} />;
};

ConnectionTable.defaultProps = {
  connections: [],
  current: 0,
  total: 0
};

ConnectionTable.propTypes = {
  setOpen: PropTypes.func.isRequired,
  setCurrentConnection: PropTypes.func.isRequired,
  connections: PropTypes.arrayOf(connectionShape),
  current: PropTypes.number,
  total: PropTypes.number,
  onPageChange: PropTypes.func.isRequired
};

export default ConnectionTable;
