import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Modal } from 'antd';
import { useTranslation } from 'react-i18next';
import { credentialShape } from '../../../../helpers/propShapes';
import credentialSent from '../../../../images/credentialSent.svg';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import './style.scss';

const SendCredentialConfirmationModal = ({ selected, targetCredentials, ...modalProps }) => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const { onOk, onCancel } = modalProps;

  const onConfirm = async () => {
    setLoading(true);
    return onOk().finally(() => setLoading(false));
  };

  const nonSendablesAmount = selected.length - targetCredentials.length;

  return (
    <Modal className="Modal" width={450} centered destroyOnClose footer={null} {...modalProps}>
      <div className="SentModalContainer">
        <img src={credentialSent} alt={t('credential.modal.title')} />
        <h3 className="ModalTitle">{t('credentials.modal.title')}</h3>
        <p className="ModalSubtitle">
          <strong>
            {t('credentials.modal.sendables', { sendables: targetCredentials.length })}
          </strong>
        </p>
        {!!nonSendablesAmount && (
          <p className="ModalInfo">
            <strong>
              {t('credentials.modal.nonSendables', {
                nonSendables: nonSendablesAmount
              })}
            </strong>
            {t('credentials.modal.nonSendablesText')}
          </p>
        )}
      </div>
      <div className="FooterButtonsSent">
        <CustomButton
          buttonProps={{
            className: 'theme-secondary',
            onClick: onCancel
          }}
          buttonText={t('credentials.actions.cancel')}
        />
        <CustomButton
          buttonProps={{
            className: 'theme-primary',
            onClick: onConfirm,
            disabled: !targetCredentials.length
          }}
          loading={loading}
          buttonText={t('credentials.actions.confirm')}
        />
      </div>
    </Modal>
  );
};

SendCredentialConfirmationModal.defaultProps = {
  targetCredentials: []
};

SendCredentialConfirmationModal.propTypes = {
  selected: PropTypes.arrayOf(credentialShape).isRequired,
  targetCredentials: PropTypes.arrayOf(credentialShape),
  visible: PropTypes.bool.isRequired,
  onOk: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired
};

export default SendCredentialConfirmationModal;
