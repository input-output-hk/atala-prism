import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { Radio, Form } from 'antd';
import { useTranslation } from 'react-i18next';
import { PlusOutlined } from '@ant-design/icons';
import { isString } from 'lodash';
import CategoryCreationModal from '../CategoryCreationModal/CategoryCreationModal';
import CategoryCard from '../../Molecules/CategoryStep/CategoryCard';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { templateCategoryShape } from '../../../../helpers/propShapes';
import { useTemplateSketch } from '../../../../hooks/useTemplateSketch';
import './_style.scss';

const ENABLED_STATE = 1;

const CategorySelector = observer(({ templateCategories }) => {
  const { t } = useTranslation();
  const { templateSketch } = useTemplateSketch();
  const [selected, setSelected] = useState(templateSketch.category);
  const [showCategoryCreation, setShowCategoryCreation] = useState(false);
  const i18nPrefix = 'credentialTemplateCreation';

  const categories = templateCategories.filter(({ state }) => state === ENABLED_STATE);

  const openNewCategoryModal = () => {
    setSelected();
    setShowCategoryCreation(true);
  };

  const onCategoryChange = ev => setSelected(ev.target.value);

  const categoryRules = [
    {
      validator: ({ field }, value) =>
        isString(value)
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
      />
      <div className="selectCategoryHeader">
        <p>{t(`${i18nPrefix}.step1.selectCategory`)}</p>
        <CustomButton
          onClick={openNewCategoryModal}
          buttonText={t(`${i18nPrefix}.actions.addCategory`)}
          theme="theme-link"
          icon={<PlusOutlined />}
        />
      </div>
      <Form.Item name="category" rules={categoryRules}>
        <div className="templateCategory">
          <Radio.Group onChange={onCategoryChange}>
            {categories.map(category => (
              <Radio key={category.id} value={category.id}>
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
});

CategorySelector.propTypes = {
  templateCategories: PropTypes.arrayOf(templateCategoryShape).isRequired
};

export default CategorySelector;
