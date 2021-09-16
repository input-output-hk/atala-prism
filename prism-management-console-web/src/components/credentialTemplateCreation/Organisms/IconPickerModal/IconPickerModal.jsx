import React from 'react';
import PropTypes from 'prop-types';
import { Modal } from 'antd';
import { useTranslation } from 'react-i18next';
import IconPicker from './IconPicker';
import './_style.scss';

const i18nPrefix = 'credentialTemplateCreation';

const IconPickerModal = ({ visible, close }) => {
  const { t } = useTranslation();

  return (
    <Modal
      className="IconPickerModal"
      visible={visible}
      onCancel={close}
      title={t(`${i18nPrefix}.categoryCreationModal.title`)}
      destroyOnClose
      footer={null}
    >
      <IconPicker close={close} />
    </Modal>
  );
};

IconPickerModal.propTypes = {
  visible: PropTypes.bool.isRequired,
  close: PropTypes.func.isRequired
};

export default IconPickerModal;
