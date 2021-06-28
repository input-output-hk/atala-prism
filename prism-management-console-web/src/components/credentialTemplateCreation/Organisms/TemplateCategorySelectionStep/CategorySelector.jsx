import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { message, Radio, Form } from 'antd';
import { useTranslation } from 'react-i18next';
import AddNewCategory from '../../Molecules/TemplateCategorySelectionStep/AddNewCategory';
import CategoryCard from '../../Molecules/TemplateCategorySelectionStep/CategoryCard';
import { antdV4FormShape, templateCategoryShape } from '../../../../helpers/propShapes';
import './_style.scss';

const ENABLED_STATE = 1;
const ADD_NEW_CATEGORY_KEY = 'ADD_NEW_CATEGORY_KEY';

const CategorySelector = ({ templateCategories, form }) => {
  const { t } = useTranslation();
  const [selected, setSelected] = useState();

  const categories = templateCategories.filter(({ state }) => state === ENABLED_STATE);

  const handleAddNewCategory = () => {
    // TODO: implement add new category functionality
    message.warn(t('errors.notImplementedYet'));
    form.resetFields(['templateCategory']);
  };

  const onCategoryChange = ev => {
    setSelected(ev.target.value);
    if (ev.target.value === ADD_NEW_CATEGORY_KEY) handleAddNewCategory();
  };

  return (
    <div className="flex selectCathegory">
      <Form.Item
        name="templateCategory"
        label={t('credentialTemplateCreation.step1.selectCategory')}
        rules={[
          {
            required: true
          }
        ]}
      >
        <div className="templateCategory">
          <Radio.Group onChange={onCategoryChange}>
            <Radio value={ADD_NEW_CATEGORY_KEY}>
              <AddNewCategory />
            </Radio>
            {categories.map(category => (
              <Radio value={category.id}>
                <CategoryCard
                  category={category}
                  typeKey={category.id}
                  key={category.id}
                  isSelected={selected === category.id}
                />
              </Radio>
            ))}
          </Radio.Group>
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
  form: antdV4FormShape.isRequired
};

export default CategorySelector;
