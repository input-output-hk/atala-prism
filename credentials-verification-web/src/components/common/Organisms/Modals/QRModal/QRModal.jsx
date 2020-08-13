import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import QRCode from 'qrcode.react';
import { Modal } from 'antd';
import './_style.scss';
import CustomButton from '../../../Atoms/CustomButton/CustomButton';

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
      <QRCode value={qrValue} />
      <h2>{t(`${tPrefix}.modal.title`)} </h2>
      <p>
        {t(`${tPrefix}.modal.subtitle.0`)} <br /> {t(`${tPrefix}.modal.subtitle.1`)}
      </p>
      <div className="FooterButtons">
        <CustomButton
          buttonProps={{ onClick: onCancel, className: 'theme-secondary' }}
          buttonText={t('actions.done')}
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
