import React from 'react';
import { Form, Input, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import { templateBodyAttributeShape } from '../../../../helpers/propShapes';

const { Option } = Select;

const dynamicAttributeTypeOptions = ['text', 'date', 'number'];

const DynamicAttributeInput = ({ value }) => {
  const { t } = useTranslation();
  const { key, name, fieldKey, dynamicAttributeIndex, ...restField } = value;
  return (
    <>
      <Form.Item
        className="dynamicAttributeInputContainer"
        {...restField}
        name={[name, 'attributeLabel']}
        fieldKey={[fieldKey, 'attributeLabel']}
        label={t('credentialTemplateCreation.step2.content.dynamicAttributeLabel', {
          index: dynamicAttributeIndex + 1
        })}
        rules={[{ required: true }]}
      >
        <Input placeholder={`Attribute ${dynamicAttributeIndex + 1}`} />
      </Form.Item>
      <Form.Item
        className="dynamicAttributeInputContainer"
        {...restField}
        name={[name, 'attributeType']}
        fieldKey={[fieldKey, 'attributeType']}
        label={t('credentialTemplateCreation.step2.content.dynamicAttributeType')}
      >
        <Select>
          {dynamicAttributeTypeOptions.map(option => (
            <Option value={option}>
              {t(`credentialTemplateCreation.step2.content.dynamicAttributeTypeOptions.${option}`)}
            </Option>
          ))}
        </Select>
      </Form.Item>
    </>
  );
};

DynamicAttributeInput.propTypes = {
  value: templateBodyAttributeShape.isRequired
};

export default DynamicAttributeInput;
