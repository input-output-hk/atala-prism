import React, { useState } from 'react';
import { Form, Radio, Upload } from 'antd';
import { useTranslation } from 'react-i18next';
import uploadCategoryIcon from '../../../../images/upload-category-icon.svg';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import IconOption from '../../Atoms/CategorySelection/IconOption';
import { defaultCategoryIcons } from '../../../../helpers/templateCategories/categories';
import { isInteger } from '../../../../helpers/genericHelpers';

const defaultFileList = defaultCategoryIcons.map((thumbUrl, index) => ({ thumbUrl, uid: index }));
const i18nPrefix = 'credentialTemplateCreation';

const CategoryIconSelector = () => {
  const { t } = useTranslation();
  const [selectedIcon, setSelectedIcon] = useState(defaultFileList[0].uid);
  const [categoryCustomIcons, setSelectedCustomIcons] = useState([]);

  const normFile = ({ file, fileList }) => {
    const newFileList = [file].concat(fileList.filter(f => f.uid !== file.uid));

    return newFileList;
  };

  const onChange = ({ fileList }) => {
    setSelectedCustomIcons(fileList);
  };

  const categoryIconRules = [
    {
      validator: (_rule, value) =>
        isInteger(value)
          ? Promise.resolve()
          : Promise.reject(
              t(`${i18nPrefix}.categoryCreationModal.errors.fieldIsRequired`, {
                field: t(`${i18nPrefix}.categoryCreationModal.categoryIcon`)
              })
            )
    }
  ];

  const onIconChange = ev => {
    setSelectedIcon(ev.target.value);
  };

  const allowedFormats = '.jpg, .jpeg, .png, .svg';

  const uploaderProps = {
    accept: allowedFormats,
    listType: 'picture',
    fileList: categoryCustomIcons,
    onChange
  };

  return (
    <Form.Item name="categoryIcon" rules={categoryIconRules}>
      <Radio.Group onChange={onIconChange}>
        <Form.Item name="categoryCustomIcons" valuePropName="file" getValueFromEvent={normFile}>
          <Upload
            name="logo"
            action="/upload.do"
            {...uploaderProps}
            itemRender={(_originNode, file) => (
              <IconOption icon={file} selected={file.uid === selectedIcon} />
            )}
          >
            <div className="verticalFlex">
              <img src={uploadCategoryIcon} className="iconSample" alt="upload custom icon" />
              <h3>{t(`${i18nPrefix}.categoryCreationModal.uploadIcon`)}</h3>
              <p>{t(`${i18nPrefix}.categoryCreationModal.allowedFormats`, { allowedFormats })}</p>
              <CustomButton
                className="theme-outline"
                buttonText={t(`${i18nPrefix}.categoryCreationModal.uploadButton`)}
              />
              <div className="galleryLabel">
                <p>{t(`${i18nPrefix}.categoryCreationModal.gallery`)}</p>
              </div>
            </div>
          </Upload>
        </Form.Item>
        <div className="imgGalleryContainer">
          {defaultFileList.map(file => (
            <IconOption icon={file} selected={file.uid === selectedIcon} />
          ))}
        </div>
      </Radio.Group>
    </Form.Item>
  );
};

export default CategoryIconSelector;
