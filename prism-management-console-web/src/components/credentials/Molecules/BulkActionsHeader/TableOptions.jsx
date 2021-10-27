import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import Checkbox from 'antd/lib/checkbox/Checkbox';
import { PulseLoader } from 'react-spinners';
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

const TableOptions = ({ bulkActionsProps, loadingSelection, selectedLength, sortingProps }) => {
  const { t } = useTranslation();
  const { selectAllProps, refreshCredentials } = bulkActionsProps;
  const { sortingBy, setSortingBy, sortDirection, setSortDirection } = sortingProps;

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

  const toggleSorting = () =>
    setSortDirection(sortAscending ? SORTING_DIRECTIONS.descending : SORTING_DIRECTIONS.ascending);

  const selectedLabel = selectedLength ? `  (${selectedLength})  ` : null;

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
        <Checkbox className="TableOptionButton" {...selectAllProps}>
          {loadingSelection ? (
            <PulseLoader size={3} color="#FFAEB3" />
          ) : (
            <span>
              {t('credentials.actions.selectAll')}
              {selectedLabel}
            </span>
          )}
        </Checkbox>
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
  loadingSelection: false,
  selectedLength: 0
};

TableOptions.propTypes = {
  bulkActionsProps: PropTypes.shape({
    refreshCredentials: PropTypes.func.isRequired,
    selectAllProps: PropTypes.shape()
  }).isRequired,
  loadingSelection: PropTypes.bool,
  selectedLength: PropTypes.number,
  sortingProps: PropTypes.shape({
    sortingBy: PropTypes.string,
    setSortingBy: PropTypes.func,
    sortDirection: PropTypes.string,
    setSortDirection: PropTypes.func
  }).isRequired
};

export default TableOptions;
