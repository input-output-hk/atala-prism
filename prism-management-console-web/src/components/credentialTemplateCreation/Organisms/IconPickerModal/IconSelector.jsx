import React from 'react';
import PropTypes from 'prop-types';
import { Radio, Upload } from 'antd';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import IconOption from '../../Atoms/CategorySelection/IconOption';
import { defaultTemplatesIconGallery } from '../../../../helpers/templateIcons/gallery';
import { blobToBase64 } from '../../../../helpers/genericHelpers';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';
import './_style.scss';

const i18nPrefix = 'credentialTemplateCreation.iconPicker';

const IconSelector = ({ selectedIcon, setSelectedIcon }) => {
  const { t } = useTranslation();

  const onChange = ({ file }) => {
    if (file.status === 'uploading') return;
    setSelectedIcon({ file, isCustomIcon: true });
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
      <Radio.Group className="IconSelectorContainer" onChange={onIconChange}>
        {iconPreview ? (
          <img className="IconPreview" src={iconPreview} alt="upload custom icon" />
        ) : (
          <div className="IconPreview IconPreviewPlaceholder">
            <SimpleLoading />
          </div>
        )}
        <h3 className="ModalTitle">{t(`${i18nPrefix}.title`)}</h3>
        <p className="ModalSubtitle">{t(`${i18nPrefix}.allowedFormats`, { allowedFormats })}</p>
        <Upload
          name="logo"
          action="/upload.do"
          className="UploaderContainer"
          {...uploaderProps}
          itemRender={(_originNode, file) =>
            file.thumbUrl ? (
              <IconOption
                key={file.uid}
                icon={{ file, isCustomIcon: true }}
                selected={selectedIcon.isCustomIcon && file.uid === selectedIcon.file.uid}
              />
            ) : (
              <div className="IconOption IconOptionPlaceholder">
                <SimpleLoading />
              </div>
            )
          }
        >
          <div className="UploaderContent verticalFlex">
            <CustomButton className="theme-outline" buttonText={t(`${i18nPrefix}.uploadButton`)} />
            <div className="GalleryLabel">
              <p>{t(`${i18nPrefix}.gallery`)}</p>
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

IconSelector.propTypes = {
  selectedIcon: PropTypes.shape({
    isCustomIcon: PropTypes.bool,
    file: PropTypes.shape({
      uid: PropTypes.string,
      thumbUrl: PropTypes.string
    }),
    index: PropTypes.number,
    src: PropTypes.string
  }).isRequired,
  setSelectedIcon: PropTypes.func.isRequired
};

export default IconSelector;
