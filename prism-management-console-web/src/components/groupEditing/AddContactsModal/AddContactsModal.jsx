import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { Modal, Row, Col } from 'antd';
import ConnectionsTable from '../../connections/Organisms/table/ConnectionsTable';
import ConnectionsFilter from '../../connections/Molecules/filter/ConnectionsFilter';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';
import { useCurrentGroupStore } from '../../../hooks/useGroupStore';
import SelectAllButton from '../../newCredential/Molecules/RecipientsTable/SelectAllButton';
import { useSelectAll } from '../../../hooks/useSelectAll';
import { CONTACT_ID_KEY } from '../../../helpers/constants';
import { filterByMultipleKeys } from '../../../helpers/filterHelpers';
import './_style.scss';

const AddContactsModal = observer(({ visible, onCancel, onConfirm }) => {
  const { t } = useTranslation();
  const [contactsNotInGroup, setContactsNotInGroup] = useState([]);
  const [textFilter, setTextFilter] = useState('');
  const [filteredContacts, setFilteredContacts] = useState([]);
  const [selectedContacts, setSelectedContacts] = useState([]);
  const { isLoadingContactsNotInGroup, getContactsNotInGroup } = useCurrentGroupStore();

  useEffect(() => {
    const handleGetContacts = async () => {
      const fetchedContacts = await getContactsNotInGroup();
      setContactsNotInGroup(fetchedContacts);
    };

    if (visible) {
      handleGetContacts();
      setSelectedContacts([]);
    }
  }, [visible, getContactsNotInGroup]);

  useEffect(() => {
    const applyFilters = contacts =>
      contacts.filter(item => filterByMultipleKeys(textFilter, item, ['name', 'externalId']));

    setFilteredContacts(textFilter ? applyFilters(contactsNotInGroup) : contactsNotInGroup);
  }, [contactsNotInGroup, textFilter]);

  const { loadingSelection, checkboxProps } = useSelectAll({
    displayedEntities: filteredContacts,
    entitiesFetcher: () => filteredContacts,
    entityKey: CONTACT_ID_KEY,
    selectedEntities: selectedContacts,
    setSelectedEntities: setSelectedContacts,
    isFetching: isLoadingContactsNotInGroup
  });

  const handleConfirm = () => onConfirm(selectedContacts);

  const confirmButton = (
    <Row>
      <Col span={24} className="FooterContainer">
        <CustomButton
          buttonProps={{
            className: 'theme-secondary',
            onClick: handleConfirm,
            disabled: selectedContacts.length === 0
          }}
          buttonText={t('groupEditing.buttons.addContacts')}
        />
      </Col>
    </Row>
  );

  return (
    <Modal
      title={t('groupEditing.selectContacts')}
      visible={visible}
      onCancel={onCancel}
      width={800}
      className="AddContactsModal"
      footer={confirmButton}
    >
      <Row type="flex" align="middle" className="mb-3">
        <Col span={5}>
          <SelectAllButton
            isLoadingSelection={loadingSelection}
            selectedEntities={selectedContacts}
            checkboxProps={checkboxProps}
          />
        </Col>
        <Col span={17}>
          <ConnectionsFilter
            showFullFilter={false}
            localStateFilter={{
              value: textFilter,
              setValue: (_key, value) => setTextFilter(value)
            }}
          />
        </Col>
      </Row>
      <Row className="ModalContactsContainer">
        <Col span={24}>
          <ConnectionsTable
            // TODO: add pagination for getting group members
            contacts={filteredContacts}
            hasFiltersApplied
            isLoading={isLoadingContactsNotInGroup}
            selectedContacts={selectedContacts}
            setSelectedContacts={setSelectedContacts}
            size="md"
          />
        </Col>
      </Row>
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
