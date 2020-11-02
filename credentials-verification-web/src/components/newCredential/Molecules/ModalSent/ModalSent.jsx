import React from 'react';
import { useTranslation } from 'react-i18next';
import { Modal } from 'antd';
import PropTypes from 'prop-types';
import credentialSent from '../../../../images/credentialSent.svg';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './style.scss';

const ModalSent = () => {
  const { t } = useTranslation();

  return (
    <Modal width={450} centered destroyOnClose visible footer={null}>
      <div className="SentModalContainer">
        <img src={credentialSent} alt={t('credential.credentialsSentModal.title')} />
        <h3>
          {t('credentials.sentCredentialModal.title')} 30{' '}
          {t('credentials.sentCredentialModal.credentials')}
        </h3>
        <p>25 {t('credentials.sentCredentialModal.cantSendText')}</p>
      </div>
      <div className="FooterButtonsSent">
        <CustomButton
          buttonProps={{ className: 'theme-secondary' }}
          buttonText={t('newCredential.modal.saveForLaterCancel')}
        />
        <CustomButton
          buttonProps={{
            className: 'theme-primary'
          }}
          buttonText={t('credentials.sentCredentialModal.confirm')}
        />
      </div>
    </Modal>
  );
};

export default ModalSent;
