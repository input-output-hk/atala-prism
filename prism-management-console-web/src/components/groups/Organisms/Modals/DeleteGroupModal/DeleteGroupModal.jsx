import React from 'react';
import { Col, Modal, Row } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../../../../common/Atoms/CustomButton/CustomButton';
import Delete from '../../../../../images/delete.svg';

import './_style.scss';

const DeleteGroupModal = ({ group, open, closeModal, handleGroupDeletion }) => {
  const { t } = useTranslation();

  const { name } = group;

  return (
    <Modal
      destroyOnClose
      visible={open}
      footer={null}
      className="DeleteGroupModal"
      onCancel={closeModal}
    >
      <Row>
        <img src={Delete} alt="remove group" />
      </Row>
      <Row className="title">{t('groups.modal.title')}</Row>
      <Row>{t('groups.modal.text', { name })}</Row>
      <Row>
        <Col span={24} className="FooterContainer">
          <CustomButton
            buttonProps={{
              className: 'theme-secondary',
              onClick: closeModal
            }}
            buttonText={t('groups.table.buttons.cancel')}
          />
          <CustomButton
            buttonProps={{
              className: 'theme-primary',
              onClick: handleGroupDeletion
            }}
            buttonText={t('groups.table.buttons.delete')}
          />
        </Col>
      </Row>
    </Modal>
  );
};

DeleteGroupModal.defaultProps = {
  group: { name: '', id: '' },
  open: false
};

DeleteGroupModal.propTypes = {
  group: PropTypes.shape({ name: PropTypes.string, id: PropTypes.string }),
  closeModal: PropTypes.func.isRequired,
  open: PropTypes.bool,
  handleGroupDeletion: PropTypes.func.isRequired
};

export default DeleteGroupModal;
