import React from 'react';
import PropTypes from 'prop-types';
import { Checkbox } from 'antd';
import { WarningOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import SelectAllButton from './SelectAllButton';
import { useCreateCredentialPageStore } from '../../../../hooks/useCreateCredentialPageStore';
import SimpleGroupsFilter from '../../../groups/Molecules/Filters/SimpleGroupsFilter';
import './_style.scss';

const GroupsTableHeader = observer(({ shouldSelectRecipients, toggleShouldSelectRecipients }) => {
  const { t } = useTranslation();

  const {
    selectedGroupIds,
    groupsSelectAllCheckboxStateProps,
    isLoadingGroupsSelection,
    selectAllGroups,
    groups,
    groupsFilterSortingProps
  } = useCreateCredentialPageStore();

  const selectAllCheckboxProps = {
    checked: groupsSelectAllCheckboxStateProps.checked,
    indeterminate: groupsSelectAllCheckboxStateProps.indeterminate,
    disabled: !shouldSelectRecipients || !groups.length,
    onChange: selectAllGroups
  };

  return (
    <div className="RecipientsSelectionTableHeader">
      <SimpleGroupsFilter filterSortingProps={groupsFilterSortingProps} />
      <SelectAllButton
        isLoadingSelection={isLoadingGroupsSelection}
        selectedEntities={selectedGroupIds}
        checkboxProps={selectAllCheckboxProps}
      />
      <div className="RecipientSelectionCheckbox NoRecipientsCheckbox">
        <Checkbox
          className="CheckboxReverse"
          onChange={toggleShouldSelectRecipients}
          checked={!shouldSelectRecipients}
        >
          <WarningOutlined className="icon" /> {t('newCredential.targetsSelection.checkbox')}
        </Checkbox>
      </div>
    </div>
  );
});

GroupsTableHeader.propTypes = {
  shouldSelectRecipients: PropTypes.bool.isRequired,
  toggleShouldSelectRecipients: PropTypes.func.isRequired
};

export default GroupsTableHeader;
