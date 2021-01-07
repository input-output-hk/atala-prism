import React from 'react';
import PropTypes from 'prop-types';
import { Modal, Row, Col } from 'antd';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';

const ConfirmDeletionModal = ({ visible, onConfirm, onCancel, length }) => {
  const { t } = useTranslation();

  return (
    <Modal
      title={t('groupEditing.removeContacts')}
      visible={visible}
      onCancel={onCancel}
      footer={
        // eslint-disable-next-line react/jsx-wrap-multilines
        <Row>
          <Col span={24} className="FooterContainer">
            <CustomButton
              buttonProps={{
                className: 'theme-link',
                onClick: onCancel
              }}
              buttonText={t('groupEditing.buttons.cancel')}
            />
            <CustomButton
              buttonProps={{
                className: 'theme-secondary',
                onClick: onConfirm
              }}
              buttonText={t('groupEditing.buttons.remove')}
            />
          </Col>
        </Row>
      }
    >
      <h3>
        {length === 1 ? t('groupEditing.wantToRemoveOne') : t('groupEditing.wantToRemoveMany')}
      </h3>
    </Modal>
  );
};

ConfirmDeletionModal.defaultProps = {};

ConfirmDeletionModal.propTypes = {
  length: PropTypes.number.isRequired,
  onCancel: PropTypes.func.isRequired,
  onConfirm: PropTypes.func.isRequired,
  visible: PropTypes.bool.isRequired
};

export default ConfirmDeletionModal;
