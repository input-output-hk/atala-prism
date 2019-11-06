import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import QRCode from 'qrcode.react';
import { Modal } from 'antd';

const QRModal = ({ tPrefix, visible, onCancel, qrValue }) => {
  const { t } = useTranslation();
  return (
    <Modal
      title={t(`${tPrefix}.modal.title`)}
      visible={visible}
      destroyOnClose
      footer={null}
      onCancel={onCancel}
    >
      <QRCode value={qrValue} />
    </Modal>
  );
};

QRModal.defaultProps = {
  visible: false
};

QRModal.prototype = {
  tPrefix: PropTypes.string.isRequired,
  visible: PropTypes.bool,
  onCancel: PropTypes.func.isRequired,
  qrValue: PropTypes.string.isRequired
};

export default QRModal;
