import React from 'react';
import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';
import { RedoOutlined } from '@ant-design/icons';
import { Button } from 'antd';
import {
  CREDENTIAL_SORTING_KEYS,
  CREDENTIAL_SORTING_KEYS_TRANSLATION
} from '../../../../helpers/constants';
import SelectAllButton from '../../../newCredential/Molecules/RecipientsTable/SelectAllButton';
import { useCredentialsIssuedPageStore } from '../../../../hooks/useCredentialsIssuedPageStore';
import SortControls from '../../../common/Molecules/Sorting/SortControls';

import './_style.scss';

const TableOptions = observer(() => {
  const { t } = useTranslation();
  const {
    credentials,
    selectAllCheckboxStateProps,
    selectedCredentials,
    isLoadingSelection,
    isRefreshing,
    refreshCredentials,
    selectAllCredentials,
    filterSortingProps
  } = useCredentialsIssuedPageStore();

  const sortingOptions = Object.values(CREDENTIAL_SORTING_KEYS).map(column => ({
    key: column,
    label: t(`credentials.table.columns.${CREDENTIAL_SORTING_KEYS_TRANSLATION[column]}`)
  }));

  const checkboxProps = {
    ...selectAllCheckboxStateProps,
    disabled: isLoadingSelection || !credentials.length,
    onChange: selectAllCredentials
  };

  return (
    <div className="TableOptions">
      <div className="LeftOptions">
        <SortControls options={sortingOptions} {...filterSortingProps} />
        <SelectAllButton
          isLoadingSelection={isLoadingSelection}
          selectedEntities={selectedCredentials}
          checkboxProps={checkboxProps}
        />
      </div>
      <Button
        className="RefreshButton no-border"
        icon={<RedoOutlined />}
        onClick={refreshCredentials}
        disabled={isRefreshing}
      >
        {t('credentials.actions.refreshTable')}
      </Button>
    </div>
  );
});

export default TableOptions;
