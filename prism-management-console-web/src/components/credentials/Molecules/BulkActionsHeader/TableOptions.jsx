import React from 'react';
import { observer } from 'mobx-react-lite';
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
import { useCredentialsIssuedPageStore } from '../../../../hooks/useCredentialsIssuedPageStore';

import './_style.scss';

const TableOptions = observer(() => {
  const { t } = useTranslation();
  const {
    credentials,
    selectAllCheckboxStateProps,
    selectedCredentials,
    isLoadingSelection,
    refreshCredentials,
    selectAllCredentials,
    filterSortingProps: { sortingBy, setSortingBy, sortDirection, toggleSortDirection }
  } = useCredentialsIssuedPageStore();

  const sortingOptions = Object.keys(CREDENTIAL_SORTING_KEYS);

  const sortingOptionsMenu = (
    <Menu onClick={({ key }) => setSortingBy(key)}>
      {sortingOptions.map(column => (
        <Menu.Item key={column}>{t(`credentials.table.columns.${column}`)}</Menu.Item>
      ))}
    </Menu>
  );

  const sortAscending = sortDirection === SORTING_DIRECTIONS.ascending;

  const checkboxProps = {
    ...selectAllCheckboxStateProps,
    disabled: isLoadingSelection || !credentials.length,
    onChange: selectAllCredentials
  };

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
        <SelectAllButton
          isLoadingSelection={selectedCredentials}
          isLoadingSelection={isLoadingSelection}
          checkboxProps={checkboxProps}
        />
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
});

export default TableOptions;
