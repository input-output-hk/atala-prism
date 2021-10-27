import React from 'react';
import { observer } from 'mobx-react-lite';
import { Button, Upload } from 'antd';
import { PictureOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useTemplateSketch } from '../../../../hooks/useTemplateSketch';
import { templateLayouts } from '../../../../helpers/templateLayouts/templates';
import { blobToBase64 } from '../../../../helpers/genericHelpers';
import './_style.scss';

const TemplateContentIcons = observer(() => {
  const { t } = useTranslation();
  const { templateSketch, setSketchState } = useTemplateSketch();

  const { images } = templateLayouts[templateSketch.layout];

  const uploaderProps = {
    accept: '.svg',
    listType: 'picture',
    fileList: []
  };

  const setImagePreview = async (file, key) => {
    const src = await blobToBase64(file);
    setSketchState({ [key]: src });
    return src;
  };

  return (
    <>
      <div className="customizeHeaderContainer">
        <h3>{t('credentialTemplateCreation.step2.style.customizeHeader')}</h3>
        {images.map(key => (
          <Upload
            name="logo"
            action="/upload.do"
            beforeUpload={file => setImagePreview(file, key)}
            {...uploaderProps}
          >
            <Button icon={<PictureOutlined />}>
              {t('credentialTemplateCreation.step2.style.chooseImage')}
            </Button>
          </Upload>
        ))}
      </div>
    </>
  );
});

export default TemplateContentIcons;
