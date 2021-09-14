import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Select, Form, Checkbox, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { isInteger } from 'lodash';
import { templateCategoryShape } from '../../../../helpers/propShapes';
import { useTemplateContext } from '../../../providers/TemplateContext';
import './_style.scss';

const ENABLED_STATE = 1;

const CategorySelector = ({ templateCategories, mockCategoriesProps }) => {
  const { t } = useTranslation();
  const { templateSettings } = useTemplateContext();
  const initialSelection = parseInt(templateSettings.category, 10) || undefined;
  const [selected, setSelected] = useState(initialSelection);
  const [showCategoryCreation, setShowCategoryCreation] = useState(false);
  const i18nPrefix = 'credentialTemplateCreation';

  const categories = templateCategories.filter(({ state }) => state === ENABLED_STATE);

  const { Option } = Select;

  const categoryRules = [
    {
      validator: ({ field }, value) =>
        isInteger(parseInt(value, 10))
          ? Promise.resolve()
          : Promise.reject(
              t('credentialTemplateCreation.errors.fieldIsRequired', {
                field: t(`credentialTemplateCreation.fields.${field}`)
              })
            )
    }
  ];

  return (
    <div className="selectCategory">
      <div className="selectCategoryHeader">
        <p>{t(`${i18nPrefix}.step1.selectCategory`)}</p>
        <p className="greyText">Once selected click next on the upper right side of the page.</p>
      </div>
      <Form.Item name="category" rules={categoryRules}>
        <div className="templateCategory">
          <Select placeholder="Category" >
            <Option>category</Option>
            <Option>category</Option>
          </Select>
        </div>
      </Form.Item>
    </div>
  );
};

CategorySelector.defaultProps = {
  templateCategories: []
};

CategorySelector.propTypes = {
  templateCategories: PropTypes.arrayOf(templateCategoryShape),
  mockCategoriesProps: PropTypes.shape({
    mockedCategories: templateCategoryShape,
    addMockedCategory: PropTypes.func.isRequired
  }).isRequired
};

export default CategorySelector;
