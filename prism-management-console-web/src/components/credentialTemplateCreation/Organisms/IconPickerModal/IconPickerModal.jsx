import React from 'react';
import PropTypes from 'prop-types';
import { Modal } from 'antd';
import IconPicker from './IconPicker';
import './_style.scss';

const IconPickerModal = ({ visible, close }) => (
  <Modal
    className="IconPickerModal"
    visible={visible}
    onCancel={close}
    destroyOnClose
    footer={null}
  >
    <IconPicker close={close} />
  </Modal>
);

IconPickerModal.propTypes = {
  visible: PropTypes.bool.isRequired,
  close: PropTypes.func.isRequired
};

export default IconPickerModal;
