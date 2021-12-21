import React from 'react';
import { observer } from 'mobx-react-lite';
import { Button, message, Upload } from 'antd';
import prettyBytes from 'pretty-bytes';
import { PictureOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useTemplateCreationStore } from '../../../../hooks/useTemplatesPageStore';
import { templateLayouts } from '../../../../helpers/templateLayouts/templates';
import { blobToBase64 } from '../../../../helpers/genericHelpers';
import { MAX_ICON_FILE_SIZE } from '../../../../helpers/constants';
import './_style.scss';

const isValidSize = file => file.size < MAX_ICON_FILE_SIZE;

const allowedFormats = '.jpg, .jpeg, .png, .svg';

const TemplateContentIcons = observer(() => {
  const { t } = useTranslation();
  const { templateSketch, setSketchState } = useTemplateCreationStore();

  const { images } = templateLayouts[templateSketch.layout];

  const uploaderProps = {
    accept: allowedFormats,
    listType: 'picture',
    fileList: []
  };

  const setImagePreview = async (file, key) => {
    const src = await blobToBase64(file);
    if (!isValidSize(file)) {
      message.error(t('errors.saveFile.tooLarge', { maxSize: prettyBytes(MAX_ICON_FILE_SIZE) }));
    } else {
      setSketchState({ [key]: src });
      return src;
    }
  };

  return (
    <>
      <div className="customizeHeaderContainer">
        <h3>{t('credentialTemplateCreation.step2.style.customizeHeader')}</h3>
        {images.map(key => (
          <Upload
            key={key}
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
