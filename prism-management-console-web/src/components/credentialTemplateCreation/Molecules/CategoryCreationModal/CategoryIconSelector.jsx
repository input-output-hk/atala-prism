import React, { useState } from 'react';
import { Form, Radio, Upload } from 'antd';
import { useTranslation } from 'react-i18next';
import { inRange, isInteger, isString } from 'lodash';
import uploadCategoryIcon from '../../../../images/upload-category-icon.svg';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import IconOption from '../../Atoms/CategorySelection/IconOption';
import { defaultCategoryIcons } from '../../../../helpers/templateCategories/categories';
import { antdV4FormShape } from '../../../../helpers/propShapes';

const defaultFileList = defaultCategoryIcons.map((thumbUrl, index) => ({ thumbUrl, uid: index }));
const i18nPrefix = 'credentialTemplateCreation';

const CategoryIconSelector = ({ categoryForm }) => {
  const { t } = useTranslation();
  const { categoryCustomIcons } = categoryForm.getFieldsValue();
  const [selectedIcon, setSelectedIcon] = useState(0);

  const normFile = ({ fileList }) => fileList;

  const onChange = ({ file, fileList }) => {
    const newFile = fileList.find(f => f.uid === file.uid);
    const newFileList = [newFile].concat(fileList.filter(f => f.uid !== file.uid));
    categoryForm.setFieldsValue({
      categoryIcon: { ...newFile, isCustomIcon: true },
      categoryCustomIcons: newFileList
    });
    categoryForm.validateFields();
    setSelectedIcon(newFile.uid);
  };

  const validateCustomIcon = value =>
    isString(value.thumbUrl)
      ? Promise.resolve()
      : Promise.reject(
          t(`${i18nPrefix}.categoryCreationModal.errors.fieldIsRequired`, {
            field: t(`${i18nPrefix}.categoryCreationModal.categoryIcon`)
          })
        );

  const validateDefaultGalleryIcon = value =>
    isInteger(parseInt(value.uid, 10)) && inRange(value.uid, 0, defaultCategoryIcons.length)
      ? Promise.resolve()
      : Promise.reject(
          t(`${i18nPrefix}.categoryCreationModal.errors.fieldIsRequired`, {
            field: t(`${i18nPrefix}.categoryCreationModal.categoryIcon`)
          })
        );

  const categoryIconRules = [
    {
      validator: (_rule, value) =>
        value.isCustomIcon ? validateCustomIcon(value) : validateDefaultGalleryIcon(value)
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
                  icon={{ ...file, isCustomIcon: true }}
                  selected={file.uid === selectedIcon.uid}
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
          </Form.Item>
        </div>
        <div className="imgGalleryContainer">
          {defaultFileList.map(file => (
            <IconOption
              icon={{ ...file, isCustomIcon: false }}
              selected={file.uid === selectedIcon.uid}
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
