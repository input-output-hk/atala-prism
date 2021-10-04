import React from 'react';
import { observer } from 'mobx-react-lite';
import { Button, Form, Upload } from 'antd';
import { PictureOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useTemplateSketch } from '../../../../hooks/useTemplateSketch';
import { templateLayouts } from '../../../../helpers/templateLayouts/templates';
import './_style.scss';

const TemplateIcons = observer(() => {
  const { t } = useTranslation();
  const { templateSketch } = useTemplateSketch();

  const { images } = templateLayouts[templateSketch.layout];

  const normFile = ({ file }) => [file];

  const uploaderProps = {
    accept: '.svg',
    listType: 'picture',
    fileList: []
  };

  return (
    <>
      <div className="customizeHeaderContainer">
        <h3>{t('credentialTemplateCreation.step2.style.customizeHeader')}</h3>
        {images.map(key => (
          <Form.Item
            key={key}
            name={key}
            label={t(`credentialTemplateCreation.step2.style.${key}`)}
            valuePropName="file"
            getValueFromEvent={normFile}
          >
            <Upload name="logo" action="/upload.do" {...uploaderProps}>
              <Button icon={<PictureOutlined />}>
                {t('credentialTemplateCreation.step2.style.chooseImage')}
              </Button>
            </Upload>
          </Form.Item>
        ))}
      </div>
    </>
  );
});

export default TemplateIcons;
