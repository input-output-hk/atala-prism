import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Radio, Form } from 'antd';
import { useTranslation } from 'react-i18next';
import { PlusOutlined } from '@ant-design/icons';
import { isInteger } from 'lodash';
import CategoryCreationModal from '../CategoryCreationModal/CategoryCreationModal';
import CategoryCard from '../../Molecules/CategoryStep/CategoryCard';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
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

  const handleAddNewCategory = () => {
    setSelected();
    setShowCategoryCreation(true);
  };

  const onCategoryChange = ev => setSelected(ev.target.value);

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
      <CategoryCreationModal
        visible={showCategoryCreation}
        close={() => setShowCategoryCreation(false)}
        mockCategoriesProps={mockCategoriesProps}
      />
      <div className="selectCategoryHeader">
        <p>{t(`${i18nPrefix}.step1.selectCategory`)}</p>
        <CustomButton
          onClick={handleAddNewCategory}
          buttonText={t(`${i18nPrefix}.actions.addCategory`)}
          theme="theme-link"
          icon={<PlusOutlined />}
        />
      </div>
      <Form.Item name="category" rules={categoryRules}>
        <div className="templateCategory">
          <Radio.Group onChange={onCategoryChange}>
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
  mockCategoriesProps: PropTypes.shape({
    mockedCategories: templateCategoryShape,
    addMockedCategory: PropTypes.func.isRequired
  }).isRequired
};

export default CategorySelector;
