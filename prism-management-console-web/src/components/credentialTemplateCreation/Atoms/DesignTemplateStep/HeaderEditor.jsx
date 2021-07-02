import React from 'react';
import { useTranslation } from 'react-i18next';
import { Col, Form, Input, Row } from 'antd';

const HeaderEditor = props => {
  const { t } = useTranslation();
  return (
    <div>
      <h3>{t('credentialTemplateCreation.step2.content.header')}</h3>
      <div>
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
      </div>
    </div>
  );
};

HeaderEditor.propTypes = {};

export default HeaderEditor;
