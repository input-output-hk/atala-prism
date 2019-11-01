import React from 'react';
import { useTranslation } from 'react-i18next';
import { Col, Modal, Row } from 'antd';
import saveCredentials from '../../../../images/noCredentials.svg';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const SaveForLaterModal = ({ open, onCancel, onOk }) => {
  const { t } = useTranslation();

  return (
    <Modal onCancel={onCancel} centered destroyOnClose visible={open} footer={null}>
      <div className="ModalContainer">
        <img src={saveCredentials} alt={t('newCredential.modal.saveCredentials')} />
        <h3>{t('newCredential.modal.title')}</h3>
        <p>{t('newCredential.modal.subtitle')}</p>
      </div>
      <div className="FooterButtons">
        <CustomButton
          buttonProps={{ onClick: onCancel, className: 'theme-outline' }}
          buttonText={t('newCredential.modal.saveForLaterCancel')}
        />
        <CustomButton
          buttonProps={{
            onClick: onOk,
            className: 'theme-secondary'
          }}
          buttonText={t('newCredential.modal.saveForLaterConfirm')}
        />
      </div>
    </Modal>
  );
};

export default SaveForLaterModal;
