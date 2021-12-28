import React from 'react';
import PropTypes from 'prop-types';
import { Checkbox } from 'antd';
import { WarningOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import SelectAllButton from './SelectAllButton';
import SimpleContactFilter from '../../../connections/Molecules/Filters/SimpleContactFilter';
import { useCreateCredentialPageStore } from '../../../../hooks/useCreateCredentialPageStore';
import './_style.scss';

const ContactsTableHeader = observer(({ shouldSelectRecipients, toggleShouldSelectRecipients }) => {
  const { t } = useTranslation();

  const {
    selectedContactIds,
    contactsSelectAllCheckboxStateProps,
    isLoadingContactsSelection,
    selectAllContacts,
    contacts,
    contactsFilterSortingProps
  } = useCreateCredentialPageStore();

  const selectAllCheckboxProps = {
    checked: contactsSelectAllCheckboxStateProps.checked,
    indeterminate: contactsSelectAllCheckboxStateProps.indeterminate,
    disabled: !shouldSelectRecipients || !contacts.length,
    onChange: selectAllContacts
  };

  return (
    <div className="RecipientsSelectionTableHeader">
      <SimpleContactFilter filterSortingProps={contactsFilterSortingProps} />
      <SelectAllButton
        isLoadingSelection={isLoadingContactsSelection}
        selectedEntities={selectedContactIds}
        checkboxProps={selectAllCheckboxProps}
      />
      <div className="RecipientSelectionCheckbox NoRecipientsCheckbox">
        <Checkbox
          className="CheckboxReverse"
          onChange={toggleShouldSelectRecipients}
          checked={!shouldSelectRecipients}
        >
          <WarningOutlined className="icon" />
          {t('newCredential.targetsSelection.checkbox')}
        </Checkbox>
      </div>
    </div>
  );
});

ContactsTableHeader.propTypes = {
  shouldSelectRecipients: PropTypes.bool.isRequired,
  toggleShouldSelectRecipients: PropTypes.func.isRequired
};

export default ContactsTableHeader;
