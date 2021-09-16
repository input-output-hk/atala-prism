import React from 'react';
import { Form, Input, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import { templateBodyAttributeShape } from '../../../../helpers/propShapes';
import { dynamicAttributeTypeOptions } from '../../../../helpers/templateHelpers';

const { Option } = Select;

const DynamicAttributeInput = ({ value }) => {
  const { t } = useTranslation();
  const { name, fieldKey, dynamicAttributeIndex } = value;
  return (
    <>
      <Form.Item
        className="dynamicAttributeInputContainer"
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
        name={[name, 'attributeType']}
        fieldKey={[fieldKey, 'attributeType']}
        label={t('credentialTemplateCreation.step2.content.dynamicAttributeType')}
        rules={[{ required: true }]}
      >
        <Select>
          {dynamicAttributeTypeOptions.map(option => (
            <Option key={option.type} value={option.value}>
              {t(
                `credentialTemplateCreation.step2.content.dynamicAttributeTypeOptions.${
                  option.label
                }`
              )}
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
