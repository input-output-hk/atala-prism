import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Radio, Form } from 'antd';
import { useTranslation } from 'react-i18next';
import { isInteger } from 'lodash';
import AddNewCategory from '../../Molecules/CategoryStep/AddNewCategory';
import CategoryCreationModal from '../CategoryCreationModal/CategoryCreationModal';
import CategoryCard from '../../Molecules/CategoryStep/CategoryCard';
import { templateCategoryShape } from '../../../../helpers/propShapes';
import { useTemplateContext } from '../../../providers/TemplateContext';
import './_style.scss';

const ENABLED_STATE = 1;

const CategorySelector = ({ templateCategories }) => {
  const { t } = useTranslation();
  const { templateSettings } = useTemplateContext();
  const initialSelection = parseInt(templateSettings.category, 10) || undefined;
  const [selected, setSelected] = useState(initialSelection);
  const [showCategoryCreation, setShowCategoryCreation] = useState(false);
  const i18nPrefix = 'credentialTemplateCreation';

  const categories = templateCategories.filter(({ state }) => state === ENABLED_STATE);

  const handleAddNewCategory = () => {
    setSelected();
    setShowCategoryCreation(true);
  };

  const onCategoryChange = ev => setSelected(ev.target.value);

  const categoryRules = [
    {
      validator: ({ field }, value) =>
        isInteger(value)
          ? Promise.resolve()
          : Promise.reject(
              t('credentialTemplateCreation.errors.fieldIsRequired', {
                field: t(`credentialTemplateCreation.fields.${field}`)
              })
            )
    }
  ];

  return (
    <div className="flex selectCategory">
      <CategoryCreationModal
        visible={showCategoryCreation}
        close={() => setShowCategoryCreation(false)}
      />
      <Form.Item
        name="category"
        label={t(`${i18nPrefix}.step1.selectCategory`)}
        rules={categoryRules}
      >
        <div className="templateCategory">
          <Radio.Group onChange={onCategoryChange}>
            <AddNewCategory onClick={handleAddNewCategory} />
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
  templateCategories: PropTypes.arrayOf(templateCategoryShape)
};

export default CategorySelector;
