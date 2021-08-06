import React from 'react';
import PropTypes from 'prop-types';
import { Form, Input, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { debounce } from 'lodash';
import { exactValueExists } from '../../../../helpers/filterHelpers';
import { SEARCH_DELAY_MS } from '../../../../helpers/constants';
import Logger from '../../../../helpers/Logger';
import { refShape, templateCategoryShape } from '../../../../helpers/propShapes';

const i18nPrefix = 'credentialTemplateCreation';

const normalize = input => input.trim();

const CategoryNameInput = ({ inputRef, getTemplateCategories, mockedCategories }) => {
  const { t } = useTranslation();

  const categoryExists = async (_rule, value, callback) => {
    try {
      const normalizedValue = normalize(value);
      const templateCategories = await getTemplateCategories();
      // TODO: remove when backend implements template categories
      const allCategories = templateCategories.concat(mockedCategories);

      if (exactValueExists(allCategories, normalizedValue, 'name')) {
        callback(
          t(`${i18nPrefix}.categoryCreationModal.errors.preExisting`, { value: normalizedValue })
        );
      } else callback();
    } catch (error) {
      Logger.error('[CredentialTypes.getTemplateCategories] Error: ', error);
      const errorMessage = t('errors.errorGetting', {
        model: t('credentialTemplateCreation.categoryCreationModal.title')
      });
      message.error(errorMessage);
      callback(errorMessage);
    }
  };

  const checkExistence = debounce(categoryExists, SEARCH_DELAY_MS);

  return (
    <div className="inputContainer">
      <Form.Item
        name="categoryName"
        label={t(`${i18nPrefix}.categoryCreationModal.categoryNameLabel`)}
        hasFeedback
        rules={[{ required: true }, { validator: checkExistence }]}
      >
        <Input
          ref={inputRef}
          placeholder={t(`${i18nPrefix}.categoryCreationModal.categoryNamePlaceholder`)}
        />
      </Form.Item>
    </div>
  );
};

CategoryNameInput.propTypes = {
  inputRef: refShape.isRequired,
  getTemplateCategories: PropTypes.func.isRequired,
  mockedCategories: templateCategoryShape.isRequired
};

export default CategoryNameInput;
