import React from 'react';
import { Button, Form, Upload } from 'antd';
import { PictureOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const TemplateIcons = () => {
  const { t } = useTranslation();

  const normFile = ({ file }) => [file];

  const uploaderProps = {
    accept: '.svg',
    listType: 'picture',
    fileList: []
  };

  return (
    <>
      <h3>{t('credentialTemplateCreation.step2.style.customizeHeader')}</h3>
      <Form.Item
        name="backgroundHeader"
        label={t('credentialTemplateCreation.step2.style.backgroundHeader')}
        valuePropName="file"
        getValueFromEvent={normFile}
      >
        <Upload name="logo" action="/upload.do" {...uploaderProps}>
          <Button icon={<PictureOutlined />}>
            {t('credentialTemplateCreation.step2.style.chooseImage')}
          </Button>
        </Upload>
      </Form.Item>
      <Form.Item
        name="iconHeader"
        label={t('credentialTemplateCreation.step2.style.iconHeader')}
        valuePropName="file"
        getValueFromEvent={normFile}
      >
        <Upload name="logo" action="/upload.do" {...uploaderProps}>
          <Button icon={<PictureOutlined />}>
            {t('credentialTemplateCreation.step2.style.chooseImage')}
          </Button>
        </Upload>
      </Form.Item>
    </>
  );
};

export default TemplateIcons;
