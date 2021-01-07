import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Modal, Row, Col, Checkbox } from 'antd';
import { withApi } from '../../providers/withApi';
import { useContactsWithFilteredListAndNotInGroup } from '../../../hooks/useContacts';
import ConnectionsTable from '../../connections/Organisms/table/ConnectionsTable';
import ConnectionsFilter from '../../connections/Molecules/filter/ConnectionsFilter';
import SimpleLoading from '../../common/Atoms/SimpleLoading/SimpleLoading';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const AddContactsModal = ({ api, groupName, visible, onCancel, onConfirm }) => {
  const { t } = useTranslation();
  const [selectedContacts, setSelectedContacts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searching, setSearching] = useState(false);

  const {
    contacts,
    filteredContacts,
    filterProps,
    handleContactsRequest,
    hasMore
  } = useContactsWithFilteredListAndNotInGroup(api.contactsManager, setLoading, setSearching);

  useEffect(() => {
    if (!contacts.length) handleGetContacts();
  }, [groupName]);

  useEffect(() => {
    if (visible) handleGetContacts();
  }, [visible]);

  const handleGetContacts = () => {
    if (groupName) {
      setLoading(true);
      handleContactsRequest(groupName, true);
    }
  };

  const handleSelectAll = async e => {
    const { checked } = e.target;
    if (checked) {
      setSelectedContacts(contacts.map(contact => contact.contactid));
    } else {
      setSelectedContacts([]);
    }
  };

  const handleConfirm = () => {
    setLoading(true);
    onConfirm(selectedContacts);
    handleGetContacts();
    setSelectedContacts([]);
  };

  return (
    <Modal
      title={t('groupEditing.selectContacts')}
      visible={visible}
      onCancel={onCancel}
      width={800}
      className="AddContactsModal"
      footer={
        // eslint-disable-next-line react/jsx-wrap-multilines
        <Row>
          <Col span={24} className="FooterContainer">
            <CustomButton
              buttonProps={{
                className: 'theme-secondary',
                onClick: handleConfirm
              }}
              buttonText={t('groupEditing.buttons.addContacts')}
            />
          </Col>
        </Row>
      }
    >
      <Row type="flex" align="middle" className="mb-2">
        <Col span={5}>
          <div>
            <Checkbox onChange={handleSelectAll}>{t('groupEditing.selectAll')}</Checkbox>
            {selectedContacts.length ? `(${selectedContacts.length})` : null}
          </div>
        </Col>
        <Col span={17}>
          <ConnectionsFilter {...filterProps} withStatus={false} />
        </Col>
      </Row>
      <Row className="ModalContactsContainer">
        <Col span={24}>
          {loading ? (
            <SimpleLoading />
          ) : (
            <ConnectionsTable
              contacts={filteredContacts}
              selectedContacts={selectedContacts}
              setSelectedContacts={setSelectedContacts}
              searching={searching}
              hasMore={hasMore}
              size="md"
            />
          )}
        </Col>
      </Row>
    </Modal>
  );
};

AddContactsModal.defaultProps = {
  groupName: ''
};

AddContactsModal.propTypes = {
  api: PropTypes.shape({
    contactsManager: PropTypes.shape({
      getContacts: PropTypes.func
    })
  }).isRequired,
  onCancel: PropTypes.func.isRequired,
  onConfirm: PropTypes.func.isRequired,
  visible: PropTypes.bool.isRequired,
  groupName: PropTypes.string
};

export default withApi(AddContactsModal);
