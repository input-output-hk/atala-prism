import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { Modal, Row, Col } from 'antd';
import ConnectionsTable from '../../connections/Organisms/table/ConnectionsTable';
import SimpleContactFilter from '../../connections/Molecules/Filters/SimpleContactFilter';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';
import { useCurrentGroupStore } from '../../../hooks/useGroupStore';
import SelectAllButton from '../../newCredential/Molecules/RecipientsTable/SelectAllButton';
import { CONTACT_ID_KEY } from '../../../helpers/constants';
import { filterByMultipleKeys } from '../../../helpers/filterHelpers';
import './_style.scss';

const getCheckedAndIndeterminateProps = (rows, selectedRows) => ({
  checked: Boolean(rows.length && selectedRows.length === rows.length),
  indeterminate: Boolean(selectedRows.length && selectedRows.length !== rows.length)
});

const AddContactsModal = observer(({ visible, onCancel, onConfirm }) => {
  const { t } = useTranslation();
  const [contactsNotInGroup, setContactsNotInGroup] = useState([]);
  const [textFilter, setTextFilter] = useState('');
  const [filteredContacts, setFilteredContacts] = useState([]);
  const [selectedContactIds, setSelectedContactIds] = useState([]);
  const { isLoadingContactsNotInGroup, getContactsNotInGroup } = useCurrentGroupStore();

  useEffect(() => {
    const handleGetContacts = async () => {
      const fetchedContacts = await getContactsNotInGroup();
      setContactsNotInGroup(fetchedContacts);
    };

    setTextFilter('');

    if (visible) {
      handleGetContacts();
      setSelectedContactIds([]);
    }
  }, [visible, getContactsNotInGroup]);

  useEffect(() => {
    const applyFilters = contacts =>
      contacts.filter(item => filterByMultipleKeys(textFilter, item, ['name', 'externalId']));

    setFilteredContacts(textFilter ? applyFilters(contactsNotInGroup) : contactsNotInGroup);
  }, [contactsNotInGroup, textFilter]);

  const handleSelectAll = ev => {
    const { checked } = ev.target;
    setSelectedContactIds(checked ? filteredContacts.map(c => c[CONTACT_ID_KEY]) : []);
  };

  const handleContactSelect = (contactRecord, selected) => {
    setSelectedContactIds(prevSelectedContactIds =>
      selected
        ? [...prevSelectedContactIds, contactRecord[CONTACT_ID_KEY]]
        : prevSelectedContactIds.filter(cId => cId !== contactRecord[CONTACT_ID_KEY])
    );
  };

  const handleConfirm = () => onConfirm(selectedContactIds);

  const checkboxProps = {
    ...getCheckedAndIndeterminateProps(filteredContacts, selectedContactIds),
    disabled: isLoadingContactsNotInGroup || !filteredContacts.length,
    onChange: handleSelectAll
  };

  const confirmButton = <div />;

  return (
    <Modal
      title={t('groupEditing.selectContacts')}
      visible={visible}
      onCancel={onCancel}
      width={800}
      className="AddContactsModal"
      footer={confirmButton}
    >
      {/* @dbrosio could you insert the rest of the filters? AZ, sort by and select all*/}
      <div className="modal-filter-container">
        <SelectAllButton
          isLoadingSelection={false}
          selectedEntities={selectedContactIds}
          checkboxProps={checkboxProps}
        />
        <div className="searchAndBtnContainer">
          <SimpleContactFilter
            localStateFilter={{
              value: textFilter,
              setValue: (_key, value) => setTextFilter(value)
            }}
          />
          <CustomButton
            buttonProps={{
              className: 'theme-outline',
              onClick: handleConfirm,
              disabled: selectedContactIds.length === 0
            }}
            buttonText={t('groupEditing.buttons.addContacts')}
          />
        </div>
      </div>
      <div className="ModalContactsContainer">
        <Col span={24}>
          <ConnectionsTable
            // TODO: add pagination for getting group members
            contacts={filteredContacts}
            isLoading={isLoadingContactsNotInGroup}
            selectedContactIds={selectedContactIds}
            onSelect={handleContactSelect}
            size="md"
            hasFiltersApplied={true}
            fetchMoreData={() => {}}
            isFetchingMore={false}
            hasMore={false}
          />
        </Col>
      </div>
    </Modal>
  );
});

AddContactsModal.defaultProps = {
  groupName: ''
};

AddContactsModal.propTypes = {
  onCancel: PropTypes.func.isRequired,
  onConfirm: PropTypes.func.isRequired,
  visible: PropTypes.bool.isRequired
};

export default AddContactsModal;
