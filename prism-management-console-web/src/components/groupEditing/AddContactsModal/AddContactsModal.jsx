import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { Modal, Row, Col } from 'antd';
import { withApi } from '../../providers/withApi';
import ConnectionsTable from '../../connections/Organisms/table/ConnectionsTable';
import ConnectionsFilter from '../../connections/Molecules/filter/ConnectionsFilter';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';
import { useCurrentGroupState } from '../../../hooks/useCurrentGroupState';
import './_style.scss';

const AddContactsModal = observer(({ visible, onCancel, onConfirm }) => {
  const { t } = useTranslation();
  const [contactsNotInGroup, setContactsNotInGroup] = useState();
  const [isNewModal, setIsNewModal] = useState(true);
  const [selectedContacts, setSelectedContacts] = useState([]);
  const { getContactsNotInGroup } = useCurrentGroupState();

  useEffect(() => {
    const handleGetContacts = async () => {
      const fetchedContacts = await getContactsNotInGroup();
      setContactsNotInGroup(fetchedContacts);
    };

    if (isNewModal && visible) {
      setIsNewModal(false);
      handleGetContacts();
    }
  }, [visible, isNewModal, getContactsNotInGroup]);

  const handleConfirm = () => {
    setIsNewModal(true);
    return onConfirm(selectedContacts).finally(() => setIsNewModal(true));
  };

  const confirmButton = (
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
      <Row type="flex" align="middle" className="mb-2">
        <Col span={5}>
          {/* <SelectAllButton
            loadingSelection={loadingSelection}
            selectedEntities={selectedGroupContacts}
            checkboxProps={checkboxProps}
          /> */}
          {/* <div>
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
          </div> */}
        </Col>
        <Col span={17}>
          <ConnectionsFilter showFullFilter={false} />
        </Col>
      </Row>
      <Row className="ModalContactsContainer">
        <Col span={24}>
          <ConnectionsTable
            overrideContacts
            contacts={contactsNotInGroup}
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

export default withApi(AddContactsModal);
