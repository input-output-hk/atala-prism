import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import PropTypes from 'prop-types';
import QRCode from 'qrcode.react';
import { Modal } from 'antd';
import CustomButton from '../../../../../../components/customButton/CustomButton';

import './_style.scss';

const QRModal = ({ tPrefix, visible, onCancel, qrValue }) => {
  const { t } = useTranslation();
  return (
    <Modal
      visible={visible}
      centered
      className="QRModal"
      footer={null}
      destroyOnClose
      onCancel={onCancel}
    >
      <h2>{t(`${tPrefix}.modal.title`)} </h2>
      <QRCode value={qrValue} />
      <div className="FooterButtons">
        <CustomButton
          buttonProps={{ onClick: onCancel, className: 'theme-secondary' }}
          buttonText="Cancel"
        />
      </div>
    </Modal>
  );
};

QRModal.defaultProps = {
  visible: false
};

QRModal.propTypes = {
  tPrefix: PropTypes.string.isRequired,
  visible: PropTypes.bool,
  onCancel: PropTypes.func.isRequired,
  qrValue: PropTypes.string.isRequired
};

export default QRModal;
