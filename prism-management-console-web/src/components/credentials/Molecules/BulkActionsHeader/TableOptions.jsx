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
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { CREDENTIAL_SORTING_KEYS, SORTING_DIRECTIONS } from '../../../../helpers/constants';
import SelectAllButton from '../../../newCredential/Molecules/RecipientsTable/SelectAllButton';
import { useCredentialIssuedUiState } from '../../../../hooks/useCredentialIssuedStore';
import { checkboxPropShape } from '../../../../helpers/propShapes';

import './_style.scss';

const TableOptions = ({ bulkActionsProps }) => {
  const { t } = useTranslation();
  const { selectedCredentials, selectAllProps, refreshCredentials } = bulkActionsProps;
  const {
    sortingBy,
    setSortingBy,
    sortDirection,
    toggleSortDirection
  } = useCredentialIssuedUiState();

  const sortingOptions = Object.keys(CREDENTIAL_SORTING_KEYS);

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
          onClick={toggleSortDirection}
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

TableOptions.propTypes = {
  bulkActionsProps: PropTypes.shape({
    selectedCredentials: PropTypes.arrayOf(PropTypes.string).isRequired,
    refreshCredentials: PropTypes.func.isRequired,
    selectAllProps: PropTypes.shape({
      loadingSelection: PropTypes.bool.isRequired,
      checkboxProps: checkboxPropShape.isRequired
    })
  }).isRequired
};

export default TableOptions;
