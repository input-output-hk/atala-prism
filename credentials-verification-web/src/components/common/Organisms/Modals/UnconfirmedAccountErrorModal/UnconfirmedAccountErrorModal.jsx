import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Modal } from 'antd';
import CustomButton from '../../../Atoms/CustomButton/CustomButton';
import waitIllustration from '../../../../../images/wait-illustration.svg';

import './_style.scss';

const UnconfirmedAccountErrorModal = ({ visible, hide }) => {
  const { t } = useTranslation();
  return (
    <Modal
      visible={visible}
      centered
      className="UnconfirmedAccountErrorModal"
      footer={null}
      destroyOnClose
      onCancel={hide}
    >
      <h2>{t('errors.unconfirmedAccountModal.title')} </h2>
      <p className="ModalInfo">{t('errors.unconfirmedAccountModal.info')}</p>
      <img src={waitIllustration} alt={t('errors.unconfirmedAccountModal.alt')} />
      <div className="FooterButtons">
        <CustomButton
          buttonProps={{ onClick: hide, className: 'theme-secondary' }}
          buttonText={t('errors.unconfirmedAccountModal.action')}
        />
      </div>
    </Modal>
  );
};

UnconfirmedAccountErrorModal.propTypes = {
  visible: PropTypes.bool.isRequired,
  hide: PropTypes.func.isRequired
};

export default UnconfirmedAccountErrorModal;
