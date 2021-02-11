import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Modal } from 'antd';
import { useTranslation } from 'react-i18next';
import { credentialShape } from '../../../../helpers/propShapes';
import sendConfirmationIcon from '../../../../images/sendConfirmationIcon.svg';
import revokeConfirmationIcon from '../../../../images/revokeConfirmationIcon.svg';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { REVOKE_CREDENTIALS, SEND_CREDENTIALS } from '../../../../helpers/constants';
import './style.scss';

const CredentialActionConfirmationModal = ({
  type,
  selected,
  targetCredentials,
  ...modalProps
}) => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const { onOk, onCancel } = modalProps;

  const onConfirm = async () => {
    setLoading(true);
    return onOk().finally(() => setLoading(false));
  };

  const nonActionablesAmount = selected.length - targetCredentials.length;

  const icon = {
    [REVOKE_CREDENTIALS]: revokeConfirmationIcon,
    [SEND_CREDENTIALS]: sendConfirmationIcon
  };

  return (
    <Modal
      className="Modal"
      visible={type}
      width={450}
      centered
      destroyOnClose
      footer={null}
      {...modalProps}
    >
      <div className="CredentialActionModalContainer">
        <img src={icon[type]} alt={t(`credentials.modal.${type}.title`)} />
        <h3 className="ModalTitle">{t(`credentials.modal.${type}.title`)}</h3>
        <p className="ModalSubtitle">
          <strong>
            {t(`credentials.modal.${type}.actionables`, { actionables: targetCredentials.length })}
          </strong>
          <br />
          {type === REVOKE_CREDENTIALS && t(`credentials.modal.${type}.actionablesHelperText`)}
        </p>
        {!!nonActionablesAmount && (
          <p className="ModalInfo">
            <strong>
              {t(`credentials.modal.${type}.nonActionables`, {
                nonActionables: nonActionablesAmount
              })}
            </strong>
            {t(`credentials.modal.${type}.nonActionablesHelperText`)}
          </p>
        )}
      </div>
      <div className="FooterActionButtons">
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

CredentialActionConfirmationModal.defaultProps = {
  targetCredentials: []
};

CredentialActionConfirmationModal.propTypes = {
  type: PropTypes.oneOf([REVOKE_CREDENTIALS, SEND_CREDENTIALS]).isRequired,
  selected: PropTypes.arrayOf(credentialShape).isRequired,
  targetCredentials: PropTypes.arrayOf(credentialShape),
  onOk: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired
};

export default CredentialActionConfirmationModal;
