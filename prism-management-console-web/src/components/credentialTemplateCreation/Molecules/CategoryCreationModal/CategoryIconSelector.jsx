import React, { useState } from 'react';
import { Form, Radio, Upload } from 'antd';
import { useTranslation } from 'react-i18next';
import { inRange, isInteger, isString } from 'lodash';
import uploadCategoryIcon from '../../../../images/upload-category-icon.svg';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import IconOption from '../../Atoms/CategorySelection/IconOption';
import { defaultCategoryIcons } from '../../../../helpers/templateCategories/categories';
import { antdV4FormShape } from '../../../../helpers/propShapes';

const i18nPrefix = 'credentialTemplateCreation';
const defaultIcon = { index: 0, src: defaultCategoryIcons[0], isCustomIcon: false };

const CategoryIconSelector = ({ categoryForm }) => {
  const { t } = useTranslation();
  const { categoryCustomIcons } = categoryForm.getFieldsValue();
  const [selectedIcon, setSelectedIcon] = useState(defaultIcon);

  const normFile = ({ fileList }) => fileList;

  const onChange = ({ file, fileList }) => {
    if (file.status === 'uploading') return;
    const newFile = fileList.find(f => f.uid === file.uid);
    const newFileList = [newFile].concat(fileList.filter(f => f.uid !== file.uid));

    setSelectedIcon({ file: newFile, isCustomIcon: true });

    // using setFields forces to only update the correct attributes
    categoryForm.setFields([
      {
        name: ['categoryIcon'],
        value: { file: newFile, isCustomIcon: true }
      }
    ]);
    categoryForm.setFields([
      {
        name: ['categoryCustomIcons'],
        value: newFileList
      }
    ]);

    categoryForm.validateFields();
  };

  const validateCustomIcon = value =>
    isString(value.file.thumbUrl)
      ? Promise.resolve()
      : Promise.reject(
          t(`${i18nPrefix}.categoryCreationModal.errors.fieldIsRequired`, {
            field: t(`${i18nPrefix}.categoryCreationModal.categoryIcon`)
          })
        );

  const validateDefaultGalleryIcon = value =>
    isInteger(parseInt(value.index, 10)) && inRange(value.index, 0, defaultCategoryIcons.length)
      ? Promise.resolve()
      : Promise.reject(
          t(`${i18nPrefix}.categoryCreationModal.errors.fieldIsRequired`, {
            field: t(`${i18nPrefix}.categoryCreationModal.categoryIcon`)
          })
        );

  const categoryIconRules = [
    {
      validator: (_rule, value) =>
        value?.isCustomIcon ? validateCustomIcon(value) : validateDefaultGalleryIcon(value)
    }
  ];

  const allowedFormats = '.jpg, .jpeg, .png, .svg';

  const uploaderProps = {
    accept: allowedFormats,
    listType: 'picture',
    fileList: categoryCustomIcons,
    onChange
  };

  const onIconChange = ev => setSelectedIcon(ev.target.value);

  return (
    <Form.Item name="categoryIcon" rules={categoryIconRules}>
      <Radio.Group onChange={onIconChange}>
        <div className="verticalFlex">
          <img src={uploadCategoryIcon} className="iconSample" alt="upload custom icon" />
          <h3>{t(`${i18nPrefix}.categoryCreationModal.uploadIcon`)}</h3>
          <p>{t(`${i18nPrefix}.categoryCreationModal.allowedFormats`, { allowedFormats })}</p>
          <Form.Item
            name="categoryCustomIcons"
            valuePropName="fileList"
            getValueFromEvent={normFile}
          >
            <Upload
              name="logo"
              action="/upload.do"
              {...uploaderProps}
              itemRender={(_originNode, file) => (
                <IconOption
                  icon={{ file, isCustomIcon: true }}
                  selected={selectedIcon.isCustomIcon && file.uid === selectedIcon.file.uid}
                />
              )}
            >
              <div className="verticalFlex">
                <CustomButton
                  buttonProps={{ className: 'theme-outline' }}
                  buttonText={t(`${i18nPrefix}.categoryCreationModal.uploadButton`)}
                />
                <div className="galleryLabel">
                  <p>{t(`${i18nPrefix}.categoryCreationModal.gallery`)}</p>
                </div>
              </div>
            </Upload>
          </Form.Item>
        </div>
        <div className="imgGalleryContainer">
          {defaultCategoryIcons.map((src, index) => (
            <IconOption
              icon={{ src, index, isCustomIcon: false }}
              selected={!selectedIcon.isCustomIcon && index === selectedIcon.index}
            />
          ))}
        </div>
      </Radio.Group>
    </Form.Item>
  );
};

CategoryIconSelector.propTypes = {
  categoryForm: antdV4FormShape.isRequired
};

export default CategoryIconSelector;
