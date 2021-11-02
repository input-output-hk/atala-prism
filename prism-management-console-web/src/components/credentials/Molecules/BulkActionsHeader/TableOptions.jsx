import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import {
  DownOutlined,
  RedoOutlined,
  SortAscendingOutlined,
  SortDescendingOutlined
} from '@ant-design/icons';
import { Button, Dropdown, Menu } from 'antd';

import './_style.scss';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { CREDENTIAL_SORTING_KEYS, SORTING_DIRECTIONS } from '../../../../helpers/constants';
import SelectAllButton from '../../../newCredential/Molecules/RecipientsTable/SelectAllButton';
import { useCredentialIssuedUiState } from '../../../../hooks/useCredentialIssuedStore';

const TableOptions = ({ bulkActionsProps }) => {
  const { t } = useTranslation();
  const { selectedCredentials, selectAllProps, refreshCredentials } = bulkActionsProps;
  const { sortingBy, setSortingBy, sortDirection, toggleSorting } = useCredentialIssuedUiState();

  // FIXME: remove frontend sorting when backend is ready:
  // CREDENTIAL_SORTING_KEYS contains the sorting options currently supported by the backend
  // For the remaining options, the sorting is done by the frontend
  const sortingOptions = Object.keys(CREDENTIAL_SORTING_KEYS).concat(
    'contactName',
    'externalId',
    'dateSigned'
  );

  const sortingOptionsMenu = (
    <Menu onClick={({ key }) => setSortingBy(key)}>
      {sortingOptions.map(column => (
        <Menu.Item key={column}>{t(`credentials.table.columns.${column}`)}</Menu.Item>
      ))}
    </Menu>
  );

  const sortAscending = sortDirection === SORTING_DIRECTIONS.ascending;

  return (
    <div className="TableOptions">
      <div className="LeftOptions">
        <Button
          className="TableOptionButton no-border"
          onClick={toggleSorting}
          icon={
            sortAscending ? (
              <SortAscendingOutlined style={{ fontSize: '16px' }} />
            ) : (
              <SortDescendingOutlined style={{ fontSize: '16px' }} />
            )
          }
        />
        <SelectAllButton selectedEntities={selectedCredentials} {...selectAllProps} />
        <Dropdown overlay={sortingOptionsMenu} trigger={['click']}>
          <CustomButton
            overrideClassName="theme-link TableOptionButton"
            buttonProps={{ icon: <DownOutlined /> }}
            buttonText={t(sortingBy ? `credentials.table.columns.${sortingBy}` : 'actions.sortBy')}
          />
        </Dropdown>
      </div>
      <Button
        className="RefreshButton no-border"
        icon={<RedoOutlined />}
        onClick={refreshCredentials}
      >
        {t('credentials.actions.refreshTable')}
      </Button>
    </div>
  );
};

TableOptions.defaultProps = {
  loadingSelection: false
};

TableOptions.propTypes = {
  bulkActionsProps: PropTypes.shape({
    refreshCredentials: PropTypes.func.isRequired,
    selectAllProps: PropTypes.shape()
  }).isRequired,
  loadingSelection: PropTypes.bool,
  sortingProps: PropTypes.shape({
    sortingBy: PropTypes.string,
    setSortingBy: PropTypes.func,
    sortDirection: PropTypes.string,
    setSortDirection: PropTypes.func
  }).isRequired
};

export default TableOptions;
