import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import { Modal } from 'antd';

import modalUserIcon from '../../../../images/modal-user.png';

import './_style.scss';

const IdentityVerifierModal = ({ showModal, onOk, onCancel, user }) => {
  const { t } = useTranslation();
  const { firstName } = user;

  return (
    <Modal visible={showModal} onOk={onOk} onCancel={onCancel} okText="Continue">
      <div className="IdentityVerifierModal">
        <img src={modalUserIcon} alt="Modal User Icon" />
        <h2>
          {t('userInfo.identityVerifierModal.question')}
          <strong>{firstName}</strong>?
        </h2>
        <p>Continue to pick up where you left off!</p>
        <span> If not, cancel and complete the form to start over.</span>
      </div>
    </Modal>
  );
};

IdentityVerifierModal.propTypes = {
  showModal: PropTypes.bool.isRequired,
  onOk: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
  user: PropTypes.objectOf.isRequired
};

export default IdentityVerifierModal;
