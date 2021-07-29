import React from 'react';
import PropTypes from 'prop-types';
import { Form, Input, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { useDebounce } from '../../../../hooks/useDebounce';
import { exactValueExists } from '../../../../helpers/filterHelpers';
import Logger from '../../../../helpers/Logger';

const i18nPrefix = 'credentialTemplateCreation';

const CategoryNameInput = ({ getTemplateCategories }) => {
  const { t } = useTranslation();

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

  return (
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
  );
};

CategoryNameInput.propTypes = {
  getTemplateCategories: PropTypes.func.isRequired
};

export default CategoryNameInput;
