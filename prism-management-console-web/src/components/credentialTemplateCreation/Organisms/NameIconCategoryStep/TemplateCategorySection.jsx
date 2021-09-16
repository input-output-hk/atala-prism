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

const TemplateCategorySection = observer(() => {
  const { t } = useTranslation();
  const { templateCategories, isLoadingCategories, createTemplateCategory } = useTemplateStore();
  const { templateSketch } = useTemplateSketch();
  const [open, setOpen] = useState(false);

  const i18nPrefix = 'credentialTemplateCreation';

  const handleCreateCategory = newCategoryName => {
    const categoryName = normalize(newCategoryName);
    createTemplateCategory({ categoryName });
  };

  const categories = templateCategories.filter(({ state }) => state === ENABLED_STATE);

  const hasPickableOption = ({ options, searchValue }) =>
    options.filter(op => handleFilter(searchValue, op.label)).length !== 0;

  const handleFilter = (input, optionLabel) =>
    optionLabel?.toLowerCase().includes(input?.toLowerCase());

  const renderCreateCategoryButton = ({ searchValue }) => (
    <CustomButton
      buttonProps={{
        className: 'theme-link',
        icon: <PlusOutlined />,
        onClick: () => handleCreateCategory(searchValue),
        loading: isLoadingCategories
      }}
      // TODO: add i18n
      buttonText={`Add ${searchValue}`}
    />
  );

  const dropdownRender = menu => {
    const { searchValue } = menu.props;
    const showAddButton = normalize(searchValue) && !categories.find(c => c.name === searchValue);

    return (
      <>
        {hasPickableOption(menu.props) && menu}
        {showAddButton && renderCreateCategoryButton(menu.props)}
      </>
    );
  };

  return (
    <div className="TemplateCategorySection">
      <p>{t(`${i18nPrefix}.step1.selectCategory`)}</p>
      <Form.Item
        name="category"
        // TODO: add i18n
        label="Once selected click next on the upper right side of the page."
        rules={[{ required: true }]}
      >
        <Select
          open={open}
          showSearch
          allowClear
          placeholder="Category"
          style={{ width: 200 }}
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
