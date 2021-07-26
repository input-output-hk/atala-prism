import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Form, Input, message, Modal, Radio, Upload } from 'antd';
import { useTranslation } from 'react-i18next';
import { defaultCategoryIcons } from '../../../../helpers/templateCategories/categories';
import uploadCategoryIcon from '../../../../images/upload-category-icon.svg';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import IconOption from '../../Atoms/CategorySelection/IconOption';
import { useDebounce } from '../../../../hooks/useDebounce';
import { useTemplateCategories } from '../../../../hooks/useCredentialTypes';
import { exactValueExists } from '../../../../helpers/filterHelpers';
import Logger from '../../../../helpers/Logger';
import { withApi } from '../../../providers/withApi';
import { credentialTypesManagerShape } from '../../../../helpers/propShapes';
import './_style.scss';

const defaultFileList = defaultCategoryIcons.map((thumbUrl, index) => ({ thumbUrl, uid: index }));
const i18nPrefix = 'credentialTemplateCreation';

const CategoryCreationModal = ({ api, visible, close }) => {
  const { t } = useTranslation();
  const { getTemplateCategories } = useTemplateCategories(api.credentialTypesManager);
  const [categoryForm] = Form.useForm();

  const [selectedIcon, setSelectedIcon] = useState(defaultFileList[0].uid);
  const [categoryCustomIcons, setSelectedCustomIcons] = useState([]);

  const normFile = ({ file, fileList }) => {
    const newFileList = [file].concat(fileList.filter(f => f.uid !== file.uid));

    return newFileList;
  };

  const onChange = ({ fileList }) => {
    setSelectedCustomIcons(fileList);
  };

  const allowedFormats = '.jpg, .jpeg, .png, .svg';

  const uploaderProps = {
    accept: allowedFormats,
    listType: 'picture',
    fileList: categoryCustomIcons,
    onChange
  };

  const categoryFields = ['categoryName', 'categoryIcon'];

  const validateNewCategory = () =>
    categoryForm.validateFields().catch(({ errorFields, values }) => {
      const partialErrorsFields = errorFields.filter(errField =>
        categoryFields.includes(...errField.name)
      );
      return { errors: partialErrorsFields.map(errorField => errorField.errors), values };
    });

  const handleCategorySubmit = async () => {
    const { errors, values } = await validateNewCategory();
    const isPartiallyValid = !errors?.length;
    if (!isPartiallyValid) return errors.map(msg => message.error(t(msg)));

    message.info(JSON.stringify(values));
    close();
  };

  const categoryExists = (_rules, value, cb) =>
    getTemplateCategories()
      .then(templateCategories => {
        if (exactValueExists(templateCategories, value, 'name')) {
          const errMessage = t(`${i18nPrefix}.categoryCreationModal.errors.preExisting`, { value });
          cb(errMessage);
        } else cb();
      })
      .catch(error => {
        Logger.error('[CredentialTypes.getTemplateCategories] Error: ', error);
        message.error(
          t('errors.errorGetting', {
            model: t('credentialTemplateCreation.categoryCreationModal.title')
          })
        );
      });

  const checkExistence = useDebounce(categoryExists);

  // Nested form items cause an issue with the error message.
  // This function manually validates and generates the error messages.
  const validateCategoryIcon = (_rules, value, cb) => {
    if (value === undefined) {
      const errMessage = t(`${i18nPrefix}.categoryCreationModal.errors.fieldIsRequired`, {
        field: t(`${i18nPrefix}.categoryCreationModal.categoryIcon`)
      });
      cb(errMessage);
    } else cb();
  };

  const validateMessages = {
    required: t('credentialTemplateCreation.errors.required')
  };

  const onIconChange = ev => {
    setSelectedIcon(ev.target.value);
  };

  return (
    <Form
      form={categoryForm}
      name="category-form"
      requiredMark={false}
      layout="vertical"
      validateMessages={validateMessages}
    >
      <Modal
        className="templateModal"
        visible={visible}
        onCancel={close}
        title={t(`${i18nPrefix}.categoryCreationModal.title`)}
        destroyOnClose
        footer={null}
      >
        <div className="inputContainer">
          <Form.Item
            name="categoryName"
            label={t(`${i18nPrefix}.categoryCreationModal.categoryNameLabel`)}
            hasFeedback
            rules={[{ required: true }, { validator: checkExistence }]}
          >
            <Input placeholder={t(`${i18nPrefix}.categoryCreationModal.categoryNamePlaceholder`)} />
          </Form.Item>
        </div>
        <Form.Item name="categoryIcon" rules={[{ validator: validateCategoryIcon }]}>
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
                  <p>
                    {t(`${i18nPrefix}.categoryCreationModal.allowedFormats`, { allowedFormats })}
                  </p>
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
        <div className="buttonSection">
          <CustomButton
            className="theme-secondary"
            buttonText={t(`${i18nPrefix}.categoryCreationModal.save`)}
            buttonProps={{
              onClick: handleCategorySubmit
            }}
          />
        </div>
      </Modal>
    </Form>
  );
};

CategoryCreationModal.propTypes = {
  api: PropTypes.shape({
    credentialTypesManager: credentialTypesManagerShape.isRequired
  }).isRequired,
  visible: PropTypes.bool.isRequired,
  close: PropTypes.func.isRequired
};

export default withApi(CategoryCreationModal);
