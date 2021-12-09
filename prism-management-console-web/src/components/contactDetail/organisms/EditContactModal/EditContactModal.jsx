import React from 'react';
import PropTypes from 'prop-types';
import { Input, Modal, Form } from 'antd';
import { useTranslation } from 'react-i18next';
import DetailBox from '../../molecules/detailBox/DetailBox';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const EditContactModal = ({
  visible,
  externalId,
  name,
  groups,
  contactId,
  selectGroupsToRemove,
  onClose,
  onFinish
}) => {
  const { t } = useTranslation();

  const items = [
    {
      fieldKey: 'name',
      name: 'name',
      label: 'Contact Name',
      rules: [{ required: true, message: 'Contact Name is required.' }]
    },
    {
      fieldKey: 'externalId',
      name: 'externalId',
      label: 'External ID',
      // TODO: add unique external ID validation once get contacts is integrated with new backend
      rules: [{ required: true, message: 'External ID is required.' }]
    }
  ];

  return (
    <Modal visible={visible} footer={null} onCancel={onClose}>
      <Form
        name="editContact"
        autoComplete="off"
        initialValues={{ externalId, name }}
        onFinish={onFinish}
      >
        <h1 className="modalTitle">{t('contacts.edit.title')}</h1>
        <div className="modalContainer">
          <div className="titleContainer">
            {items.map(col => (
              <div key={col.fieldKey} className="col">
                {col.label}
              </div>
            ))}
          </div>
          <div className="input">
            {items.map(item => (
              <Form.Item
                name={item.name}
                fieldKey={item.fieldKey}
                rules={item.rules}
                key={item.fieldKey}
              >
                <Input />
              </Form.Item>
            ))}
          </div>
        </div>
        <p className="modalSubtitle">
          <strong>{t('contacts.edit.groups')}</strong>
        </p>
        <div className="boxContainer">
          <DetailBox groups={groups} onDelete={selectGroupsToRemove} contactId={contactId} />
          <CustomButton
            buttonProps={{
              className: 'theme-secondary',
              htmlType: 'submit'
            }}
            buttonText={t('actions.save')}
          />
        </div>
      </Form>
    </Modal>
  );
};

EditContactModal.propTypes = {
  visible: PropTypes.bool.isRequired,
  externalId: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  groups: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string,
      numberOfContacts: PropTypes.number
    })
  ).isRequired,
  contactId: PropTypes.string.isRequired,
  selectGroupsToRemove: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
  onFinish: PropTypes.func.isRequired
};

export default EditContactModal;
