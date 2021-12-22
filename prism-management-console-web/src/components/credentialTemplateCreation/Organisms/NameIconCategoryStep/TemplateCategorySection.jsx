import React, { useState } from 'react';
import { observer } from 'mobx-react-lite';
import { Form, message, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import { PlusOutlined } from '@ant-design/icons';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { useTemplateCreationStore } from '../../../../hooks/useTemplatesPageStore';
import Logger from '../../../../helpers/Logger';
import { CREDENTIAL_TYPE_CATEGORY_STATUSES } from '../../../../helpers/constants';

import './_style.scss';

const { READY } = CREDENTIAL_TYPE_CATEGORY_STATUSES;

const { Option } = Select;

const normalize = input => input.trim();

const i18nPrefix = 'credentialTemplateCreation.templateCategory';

const TemplateCategorySection = observer(() => {
  const { t } = useTranslation();

  const {
    templateCategories,
    isLoadingCategories,
    createTemplateCategory,
    templateSketch
  } = useTemplateCreationStore();
  const [open, setOpen] = useState(false);

  const handleCreateCategory = async newCategoryName => {
    const categoryName = normalize(newCategoryName);
    try {
      await createTemplateCategory({ categoryName });
    } catch (error) {
      Logger.error(
        '[templateStore.createTemplateCategory] Error while saving Template Category',
        error
      );
      message.error(t('errors.saving', { model: t('templates.table.columns.category') }));
    }
  };

  const categories = templateCategories.filter(({ state }) => state === READY);

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
            <Option key={cat.id} value={cat.id} label={cat.name}>
              {cat.name}
            </Option>
          ))}
        </Select>
      </Form.Item>
    </div>
  );
});

export default TemplateCategorySection;
