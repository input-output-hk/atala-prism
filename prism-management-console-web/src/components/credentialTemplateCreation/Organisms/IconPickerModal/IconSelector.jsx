import React from 'react';
import { Radio, Upload } from 'antd';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import IconOption from '../../Atoms/CategorySelection/IconOption';
import { defaultTemplatesIconGallery } from '../../../../helpers/templateIcons/gallery';
import { blobToBase64 } from '../../../../helpers/genericHelpers';
import './_style.scss';

const i18nPrefix = 'credentialTemplateCreation';

const IconSelector = ({ selectedIcon, setSelectedIcon }) => {
  const { t } = useTranslation();

  const onChange = ({ file, fileList }) => {
    if (file.status === 'uploading') return;
    const newFile = fileList.find(f => f.uid === file.uid);
    setSelectedIcon({ file: newFile, isCustomIcon: true });
  };

  const previewFile = async file => {
    const thumbUrl = await blobToBase64(file);
    setSelectedIcon({ file: { ...file, thumbUrl }, isCustomIcon: true });
    return thumbUrl;
  };

  const allowedFormats = '.jpg, .jpeg, .png, .svg';

  const uploaderProps = {
    accept: allowedFormats,
    listType: 'picture',
    onChange,
    previewFile
  };

  const onIconChange = ev => setSelectedIcon(ev.target.value);

  const iconPreview = selectedIcon.isCustomIcon ? selectedIcon.file.thumbUrl : selectedIcon.src;

  return (
    <div className="TemplateIconSelector verticalFlex">
      <Radio.Group onChange={onIconChange}>
        <img className="IconPreview" src={iconPreview} alt="upload custom icon" />
        <h3>{t(`${i18nPrefix}.categoryCreationModal.uploadIcon`)}</h3>
        <p>{t(`${i18nPrefix}.categoryCreationModal.allowedFormats`, { allowedFormats })}</p>

        <Upload
          name="logo"
          action="/upload.do"
          {...uploaderProps}
          itemRender={(_originNode, file) => (
            <IconOption
              key={file.uid}
              icon={{ file, isCustomIcon: true }}
              selected={selectedIcon.isCustomIcon && file.uid === selectedIcon.file.uid}
            />
          )}
        >
          <div className="verticalFlex">
            <CustomButton
              className="theme-outline"
              buttonText={t(`${i18nPrefix}.categoryCreationModal.uploadButton`)}
            />
            <div className="galleryLabel">
              <p>{t(`${i18nPrefix}.categoryCreationModal.gallery`)}</p>
            </div>
          </div>
        </Upload>
        <div className="DefaultGalleryContainer">
          {defaultTemplatesIconGallery.map((src, index) => (
            <IconOption
              key={src}
              icon={{ src, index, isCustomIcon: false }}
              selected={!selectedIcon.isCustomIcon && index === selectedIcon.index}
            />
          ))}
        </div>
      </Radio.Group>
    </div>
  );
};

export default IconSelector;
