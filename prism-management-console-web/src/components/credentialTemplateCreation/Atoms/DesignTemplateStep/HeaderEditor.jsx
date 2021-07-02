import React from 'react';
import { useTranslation } from 'react-i18next';
import { Col, Form, Input, Row } from 'antd';

const HeaderEditor = props => {
  const { t } = useTranslation();
  return (
    <div className="headerContainer">
      <h3>{t('credentialTemplateCreation.step2.content.header')}</h3>
      <div className="inputContainer">
        <div
          className="input"
          name="credentialTitle"
          label={t('credentialTemplateCreation.step2.content.credentialTitle')}
        >
          <label>Credential Title</label>
          <Input />
        </div>{' '}
        <div
          className="input"
          name="credentialSubtitle"
          label={t('credentialTemplateCreation.step2.content.credentialSubtitle')}
        >
          <label>Credential Subtitle</label>
          <Input />
        </div>
      </div>
    </div>
  );
};

HeaderEditor.propTypes = {};

export default HeaderEditor;
