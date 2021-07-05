import React from 'react';
import { useTranslation } from 'react-i18next';
import { Form, Input } from 'antd';

const HeaderEditor = () => {
  const { t } = useTranslation();
  return (
    <div className="headerContainer">
      <h3>{t('credentialTemplateCreation.step2.content.header')}</h3>
      <div className="inputContainer">
        <Form.Item
          className="input"
          name="credentialTitle"
          label={t('credentialTemplateCreation.step2.content.credentialTitle')}
        >
          <Input />
        </Form.Item>
        <Form.Item
          className="input"
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
