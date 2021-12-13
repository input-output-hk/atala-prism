import React from 'react';
import PropTypes from 'prop-types';
import { message, Radio, Upload } from 'antd';
import { useTranslation } from 'react-i18next';
import prettyBytes from 'pretty-bytes';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import IconOption from '../../Atoms/CategorySelection/IconOption';
import { defaultTemplatesIconGallery } from '../../../../helpers/templateIcons/gallery';
import { blobToBase64 } from '../../../../helpers/genericHelpers';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';
import { MAX_ICON_FILE_SIZE } from '../../../../helpers/constants';
import './_style.scss';

const i18nPrefix = 'credentialTemplateCreation.iconPicker';

const isValidSize = file => file.size < MAX_ICON_FILE_SIZE;

const allowedFormats = '.svg';

const IconSelector = ({ selectedIcon, setSelectedIcon }) => {
  const { t } = useTranslation();

  const onChange = ({ file }) => {
    if (file.status === 'uploading') return;
    if (!isValidSize(file)) {
      message.error(t('errors.saveFile.tooLarge', { maxSize: prettyBytes(MAX_ICON_FILE_SIZE) }));
    } else {
      setSelectedIcon({ file, isCustomIcon: true });
    }
  };

  const previewFile = async file => {
    const thumbUrl = await blobToBase64(file);
    if (isValidSize(file)) {
      setSelectedIcon({ file: { ...file, thumbUrl }, isCustomIcon: true });
    }
    return thumbUrl;
  };

  const uploaderProps = {
    accept: allowedFormats,
    listType: 'picture',
    onChange,
    previewFile
  };

  const onIconChange = ev => setSelectedIcon(ev.target.value);

  const iconPreview = selectedIcon.isCustomIcon ? selectedIcon.file.thumbUrl : selectedIcon.src;

  const renderFileList = (_originNode, file) => {
    if (!isValidSize(file)) return;

    if (!file.thumbUrl)
      return (
        <div className="IconOption IconOptionPlaceholder">
          <SimpleLoading />
        </div>
      );

    return (
      <IconOption
        key={file.uid}
        icon={{ file, isCustomIcon: true }}
        selected={selectedIcon.isCustomIcon && file.uid === selectedIcon.file.uid}
      />
    );
  };

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
        <p className="ModalSubtitle">
          {t(`${i18nPrefix}.allowedFormats`, {
            allowedFormats,
            maxSize: prettyBytes(MAX_ICON_FILE_SIZE)
          })}
        </p>
        <Upload
          name="logo"
          action="/upload.do"
          className="UploaderContainer"
          {...uploaderProps}
          itemRender={renderFileList}
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
