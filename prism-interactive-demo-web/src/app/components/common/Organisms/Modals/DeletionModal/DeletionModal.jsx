import React from 'react';
import { Button, Col, Modal, Row } from 'antd';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import PropTypes from 'prop-types';

const DeletionModal = ({ toDelete: { name, id }, open, closeModal, handleDeletion, prefix }) => {
  const { t } = useTranslation();

  return (
    <Modal
      title={t(`${prefix}.modal.title`)}
      destroyOnClose
      visible={open}
      footer={null}
      onCancel={closeModal}
    >
      <React.Fragment>
        <Row>{t(`${prefix}.modal.text`, { name })}</Row>
        <Row>
          <Col>
            <Button onClick={closeModal}>{t('actions.cancel')}</Button>
          </Col>
          <Col>
            <Button onClick={() => handleDeletion(id, name)}>{t('actions.delete')}</Button>
          </Col>
        </Row>
      </React.Fragment>
    </Modal>
  );
};

DeletionModal.defaultProps = {
  toDelete: { name: '', id: '' },
  open: false
};

DeletionModal.propTypes = {
  toDelete: PropTypes.shape({ name: PropTypes.string, id: PropTypes.string }),
  closeModal: PropTypes.func.isRequired,
  open: PropTypes.bool,
  handleDeletion: PropTypes.func.isRequired,
  prefix: PropTypes.string.isRequired
};

export default DeletionModal;
