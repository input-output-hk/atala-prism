import React from 'react';
import { Button, Col, Modal, Row } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';

const DeleteGroupModal = ({
  groupToDelete: { groupName, id },
  open,
  closeModal,
  handleGroupDeletion
}) => {
  const { t } = useTranslation();

  return (
    <Modal
      title={t('groups.modal.title')}
      destroyOnClose
      visible={open}
      footer={null}
      onCancel={closeModal}
    >
      <React.Fragment>
        <Row>{t('groups.modal.text', { groupName })}</Row>
        <Row>
          <Col>
            <Button onClick={closeModal}>{t('actions.cancel')}</Button>
          </Col>
          <Col>
            <Button onClick={() => handleGroupDeletion(id, groupName)}>
              {t('actions.delete')}
            </Button>
          </Col>
        </Row>
      </React.Fragment>
    </Modal>
  );
};

DeleteGroupModal.defaultProps = {
  groupToDelete: { groupName: '', id: '' },
  open: false
};

DeleteGroupModal.propTypes = {
  groupToDelete: PropTypes.shape({ groupName: PropTypes.string, id: PropTypes.string }),
  closeModal: PropTypes.func.isRequired,
  open: PropTypes.bool,
  handleGroupDeletion: PropTypes.func.isRequired
};

export default DeleteGroupModal;
