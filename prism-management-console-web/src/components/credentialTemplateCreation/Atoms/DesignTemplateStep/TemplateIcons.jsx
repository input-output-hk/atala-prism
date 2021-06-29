import React from 'react';
import { Button, Form, message, Upload } from 'antd';
import { PictureOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const normFile = ev => {
  message.warn('Upload event:', ev);
  if (Array.isArray(ev)) return ev;
  return ev && ev.fileList;
};

const TemplateIcons = () => {
  const { t } = useTranslation();

  const uploaderProps = {
    accept: '.svg, .png, .jpg, .jpeg',
    multiple: false
    // fileList: selectedFileList,
    // onRemove: handleRemoveFile,
    // customRequest: handleFileRequest
  };

  return (
    <>
      <h3>{t('credentialTemplateCreation.step2.style.customizeHeader')}</h3>
      <Form.Item
        name="backgroundHeader"
        label={t('credentialTemplateCreation.step2.style.backgroundHeader')}
        valuePropName="fileList"
        getValueFromEvent={normFile}
      >
        <Upload name="logo" action="/upload.do" listType="picture" {...uploaderProps}>
          <Button icon={<PictureOutlined />}>
            {t('credentialTemplateCreation.step2.style.chooseImage')}
          </Button>
        </Upload>
      </Form.Item>
      <Form.Item
        name="iconHeader"
        label={t('credentialTemplateCreation.step2.style.iconHeader')}
        valuePropName="fileList"
        getValueFromEvent={normFile}
      >
        <Upload name="logo" action="/upload.do" listType="picture">
          <Button icon={<PictureOutlined />}>
            {t('credentialTemplateCreation.step2.style.chooseImage')}
          </Button>
        </Upload>
      </Form.Item>
    </>
  );
};

export default TemplateIcons;
