import React from 'react';
import { Button, Col, Modal, Row } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';

const DeleteGroupModal = ({ toDelete: { name, id }, open, closeModal, handleGroupDeletion }) => {
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
        <Row>{t('groups.modal.text', { groupName: name })}</Row>
        <Row>
          <Col>
            <Button onClick={closeModal}>{t('actions.cancel')}</Button>
          </Col>
          <Col>
            <Button onClick={() => handleGroupDeletion(id, name)}>{t('actions.delete')}</Button>
          </Col>
        </Row>
      </React.Fragment>
    </Modal>
  );
};

DeleteGroupModal.defaultProps = {
  toDelete: { name: '', id: '' },
  open: false
};

DeleteGroupModal.propTypes = {
  toDelete: PropTypes.shape({ name: PropTypes.string, id: PropTypes.string }),
  closeModal: PropTypes.func.isRequired,
  open: PropTypes.bool,
  handleGroupDeletion: PropTypes.func.isRequired
};

export default DeleteGroupModal;
