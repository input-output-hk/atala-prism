import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { Form, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import { PlusOutlined } from '@ant-design/icons';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { templateCategoryShape } from '../../../../helpers/propShapes';
import { useTemplateStore } from '../../../../hooks/useTemplateStore';
import { useTemplateSketch } from '../../../../hooks/useTemplateSketch';
import './_style.scss';

const ENABLED_STATE = 1;

const { Option } = Select;

const normalize = input => input.trim();

const i18nPrefix = 'credentialTemplateCreation.templateCategory';

const TemplateCategorySection = observer(() => {
  const { t } = useTranslation();
  const { templateCategories, isLoadingCategories, createTemplateCategory } = useTemplateStore();
  const { templateSketch } = useTemplateSketch();
  const [open, setOpen] = useState(false);

  const handleCreateCategory = newCategoryName => {
    const categoryName = normalize(newCategoryName);
    createTemplateCategory({ categoryName });
  };

  const categories = templateCategories.filter(({ state }) => state === ENABLED_STATE);

  const hasPickableOption = ({ options, searchValue }) =>
    Boolean(options.filter(op => handleFilter(searchValue, op.label)).length);

  const handleFilter = (input, optionLabel) =>
    optionLabel?.toLowerCase().includes(input?.toLowerCase());

  // eslint-disable-next-line react/prop-types
  const renderCreateCategoryButton = ({ searchValue }) => (
    <CustomButton
      overrideClassName="theme-link AddCategoryButton"
      buttonProps={{
        icon: <PlusOutlined />,
        onClick: () => handleCreateCategory(searchValue),
        loading: isLoadingCategories
      }}
      buttonText={t(`${i18nPrefix}.addCategoryButton`, { searchValue })}
    />
  );

  const dropdownRender = menu => {
    const { searchValue } = menu.props;
    const showAddButton = normalize(searchValue) && !categories.find(c => c.name === searchValue);

    return (
      <div className="CategoriesDropDown">
        {hasPickableOption(menu.props) && menu}
        {showAddButton && renderCreateCategoryButton(menu.props)}
      </div>
    );
  };

  return (
    <div className="TemplateCategorySection">
      <p className="TitleSmall">{t(`${i18nPrefix}.title`)}</p>
      <p className="SubtitleGray">{t(`${i18nPrefix}.info`)}</p>
      <Form.Item name="category" label={t(`${i18nPrefix}.label`)} rules={[{ required: true }]}>
        <Select
          open={open}
          showSearch
          allowClear
          placeholder={t(`${i18nPrefix}.placeholder`)}
          optionFilterProp="label"
          value={templateSketch.category}
          onDropdownVisibleChange={setOpen}
          dropdownRender={dropdownRender}
        >
          {categories.map(cat => (
            <Option value={cat.id} label={cat.name}>
              {cat.name}
            </Option>
          ))}
        </Select>
      </Form.Item>
    </div>
  );
});

TemplateCategorySection.propTypes = {
  templateCategories: PropTypes.arrayOf(templateCategoryShape).isRequired
};

export default TemplateCategorySection;
