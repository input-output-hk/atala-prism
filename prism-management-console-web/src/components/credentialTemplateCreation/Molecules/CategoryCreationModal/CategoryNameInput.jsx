import React from 'react';
import PropTypes from 'prop-types';
import { Form, Input } from 'antd';
import { useTranslation } from 'react-i18next';
import { exactValueExists } from '../../../../helpers/filterHelpers';
import { refShape, templateCategoryShape } from '../../../../helpers/propShapes';

const i18nPrefix = 'credentialTemplateCreation';

const normalize = input => input.trim();

const CategoryNameInput = ({ inputRef, templateCategories, mockedCategories }) => {
  const { t } = useTranslation();

  const categoryExists = async (_rule, value) => {
    const normalizedValue = normalize(value);
    // TODO: remove when backend implements template categories
    const allCategories = templateCategories.concat(mockedCategories);

    if (exactValueExists(allCategories, normalizedValue, 'name')) {
      const errorMessage = t(`${i18nPrefix}.categoryCreationModal.errors.preExisting`, {
        value: normalizedValue
      });
      throw new Error(errorMessage);
    }
  };

  return (
    <div className="inputContainer">
      <Form.Item
        name="categoryName"
        label={t(`${i18nPrefix}.categoryCreationModal.categoryNameLabel`)}
        hasFeedback
        rules={[{ required: true }, { validator: categoryExists }]}
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
  templateCategories: PropTypes.arrayOf(templateCategoryShape).isRequired,
  mockedCategories: PropTypes.arrayOf(templateCategoryShape).isRequired
};

export default CategoryNameInput;
