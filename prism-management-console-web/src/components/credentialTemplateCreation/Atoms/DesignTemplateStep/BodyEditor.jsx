import React from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Form, Input, Space } from 'antd';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { getDefaultAttribute } from '../../../../hooks/useTemplateSettings';

const BodyEditor = props => {
  const { t } = useTranslation();

  return (
    <Form.List name="credentialBody">
      {(attributes, { add, remove }) => (
        <div>
          <div className="rowHeader">
            <h3>{t('credentialTemplateCreation.step2.content.body')}</h3>
            <div>
              <CustomButton
                buttonText="Add Attribute"
                buttonProps={{
                  className: 'theme-link',
                  icon: <PlusOutlined />,
                  onClick: () => add(getDefaultAttribute(attributes.length + 1))
                }}
              />{' '}
              <CustomButton
                buttonText="Add fixed text"
                buttonProps={{
                  className: 'theme-link',
                  icon: <PlusOutlined />
                }}
              />
            </div>
          </div>
          <div className="bodyItemContainer">
            <div className="">
              {attributes.map(({ key, name, fieldKey, ...restField }) => (
                <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                  <div className="firstGroupInputContainer">
                    <Form.Item
                      className="inputContainer"
                      {...restField}
                      name={[name, 'attributeLabel']}
                      fieldKey={[fieldKey, 'attributeLabel']}
                      label={t('credentialTemplateCreation.step2.content.attributeLabel', {
                        index: fieldKey + 1
                      })}
                    >
                      <Input placeholder={`Attribute ${fieldKey} Label`} />
                    </Form.Item>
                    <Form.Item
                      className="inputContainer"
                      {...restField}
                      name={[name, 'attributeType']}
                      fieldKey={[fieldKey, 'attributeType']}
                      label={t('credentialTemplateCreation.step2.content.attributeType')}
                    >
                      <Input placeholder="Label Type" />
                    </Form.Item>
                    <Button icon={<DeleteOutlined />} onClick={() => remove(name)} />
                  </div>
                </Space>
              ))}
            </div>
          </div>
        </div>
      )}
    </Form.List>
  );
};

BodyEditor.propTypes = {};

export default BodyEditor;
