import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Modal, Row, Col, Checkbox } from 'antd';
import { PulseLoader } from 'react-spinners';
import { withApi } from '../../providers/withApi';
import { useContactsWithFilteredListAndNotInGroup } from '../../../hooks/useContacts';
import ConnectionsTable from '../../connections/Organisms/table/ConnectionsTable';
import ConnectionsFilter from '../../connections/Molecules/filter/ConnectionsFilter';
import SimpleLoading from '../../common/Atoms/SimpleLoading/SimpleLoading';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';
import {
  getCheckedAndIndeterminateProps,
  handleSelectAll
} from '../../../helpers/selectionHelpers';
import { CONTACT_ID_KEY } from '../../../helpers/constants';

import './_style.scss';

const AddContactsModal = ({ api, groupName, visible, onCancel, onConfirm }) => {
  const { t } = useTranslation();
  const [isNewModal, setIsNewModal] = useState(true);
  const [selectedContacts, setSelectedContacts] = useState([]);
  const [loadingSelection, setLoadingSelection] = useState(false);

  const {
    contacts,
    filterProps,
    handleContactsRequest,
    hasMore,
    isLoading,
    isSearching
  } = useContactsWithFilteredListAndNotInGroup(api.contactsManager);

  useEffect(() => {
    if (!visible) setIsNewModal(true);
    if (visible && isNewModal) {
      setIsNewModal(false);
      handleContactsRequest({ groupNameParam: groupName, isRefresh: true });
    }
  }, [visible, isNewModal, groupName, handleContactsRequest]);

  const handleSelectAllContacts = ev =>
    handleSelectAll({
      ev,
      setSelected: setSelectedContacts,
      entities: contacts,
      hasMore,
      idKey: CONTACT_ID_KEY,
      fetchAll: () => api.contactsManager.getAllContacts(),
      setLoading: setLoadingSelection
    });

  const selectAllProps = {
    ...getCheckedAndIndeterminateProps(contacts, selectedContacts),
    disabled: loadingSelection,
    onChange: handleSelectAllContacts
  };

  const handleConfirm = () => {
    onConfirm(selectedContacts);
    handleContactsRequest({ groupNameParam: groupName, isRefresh: true });
    setSelectedContacts([]);
  };

  const selectedLabel = selectedContacts.length ? `  (${selectedContacts.length})  ` : null;

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
            <Checkbox className="groupsCheckbox" {...selectAllProps}>
              {loadingSelection ? (
                <PulseLoader size={3} color="#FFAEB3" />
              ) : (
                <span>
                  {t('groupEditing.selectAll')}
                  {selectedLabel}
                </span>
              )}
            </Checkbox>
          </div>
        </Col>
        <Col span={17}>
          <ConnectionsFilter {...filterProps} fullFilters={false} />
        </Col>
      </Row>
      <Row className="ModalContactsContainer">
        <Col span={24}>
          {isLoading ? (
            <SimpleLoading />
          ) : (
            <ConnectionsTable
              contacts={contacts}
              selectedContacts={selectedContacts}
              setSelectedContacts={setSelectedContacts}
              searching={isSearching}
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
      getContacts: PropTypes.func,
      getAllContacts: PropTypes.func
    })
  }).isRequired,
  onCancel: PropTypes.func.isRequired,
  onConfirm: PropTypes.func.isRequired,
  visible: PropTypes.bool.isRequired,
  groupName: PropTypes.string
};

export default withApi(AddContactsModal);
