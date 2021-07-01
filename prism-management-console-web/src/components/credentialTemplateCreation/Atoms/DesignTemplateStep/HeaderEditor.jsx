import React from 'react';
import { useTranslation } from 'react-i18next';
import { Col, Form, Input, Row } from 'antd';

const HeaderEditor = props => {
  const { t } = useTranslation();
  return (
    <Col>
      <h3>{t('credentialTemplateCreation.step2.content.header')}</h3>
      <Row>
        <Form.Item
          name="credentialTitle"
          label={t('credentialTemplateCreation.step2.content.credentialTitle')}
        >
          <Input />
        </Form.Item>{' '}
        <Form.Item
          name="credentialSubtitle"
          label={t('credentialTemplateCreation.step2.content.credentialSubtitle')}
        >
          <Input />
        </Form.Item>
      </Row>
    </Col>
  );
};

HeaderEditor.propTypes = {};

export default HeaderEditor;
