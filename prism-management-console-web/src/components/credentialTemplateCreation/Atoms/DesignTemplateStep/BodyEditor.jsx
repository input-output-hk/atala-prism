import React from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Col, Form, Input, Row, Space } from 'antd';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { defaultAttribute } from '../../../../hooks/useTemplateSettings';

const BodyEditor = props => {
  const { t } = useTranslation();

  return (
    <Form.List name="credentialBody">
      {(attributes, { add, remove }) => (
        <Col>
          <Row>
            <h3>{t('credentialTemplateCreation.step2.content.body')}</h3>
            <CustomButton
              buttonText="Add Attribute"
              buttonProps={{
                className: 'theme-link',
                icon: <PlusOutlined />,
                onClick: () => add(defaultAttribute)
              }}
            />{' '}
            <CustomButton
              buttonText="Add fixed text"
              buttonProps={{
                className: 'theme-link',
                icon: <PlusOutlined />
              }}
            />
          </Row>
          <Col>
            <div>
              {attributes.map(({ key, name, fieldKey, ...restField }) => (
                <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                  <Row>
                    <Form.Item
                      {...restField}
                      name={[name, 'attributeLabel']}
                      fieldKey={[fieldKey, 'attributeLabel']}
                      label={`Attribute ${fieldKey} Label`}
                    >
                      <Input placeholder={`Attribute ${fieldKey} Label`} />
                    </Form.Item>
                    <Form.Item
                      {...restField}
                      name={[name, 'attributeType']}
                      fieldKey={[fieldKey, 'attributeType']}
                      label="Label Type"
                    >
                      <Input placeholder="Label Type" />
                    </Form.Item>
                    <Button icon={<DeleteOutlined />} onClick={() => remove(name)} />
                  </Row>
                </Space>
              ))}
            </div>
          </Col>
        </Col>
      )}
    </Form.List>
  );
};

BodyEditor.propTypes = {};

export default BodyEditor;
