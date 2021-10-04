import React from 'react';
import { Form, Input } from 'antd';
import { useTranslation } from 'react-i18next';
import { templateBodyAttributeShape } from '../../../../helpers/propShapes';

const FixedTextInput = ({ value }) => {
  const { t } = useTranslation();
  const { name, fieldKey, textAttributeIndex } = value;
  return (
    <Form.Item
      className="fixedTextInputContainer"
      name={[name, 'text']}
      fieldKey={[fieldKey, 'text']}
      label={t('credentialTemplateCreation.step2.content.fixedTextAttribute', {
        index: textAttributeIndex + 1
      })}
      rules={[{ required: true }]}
    >
      <Input placeholder={`Fixed Text ${textAttributeIndex + 1}`} />
    </Form.Item>
  );
};

FixedTextInput.propTypes = {
  value: templateBodyAttributeShape.isRequired
};

export default FixedTextInput;
