import React from 'react';
import { Select } from 'antd';
import { useTranslation } from 'react-i18next';
import { DownOutlined } from '@ant-design/icons';
import { observer } from 'mobx-react-lite';
import CustomInputGroup from '../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import CustomDatePicker from '../../../common/Atoms/CustomDatePicker/CustomDatePicker';
import { useTemplateStore, useTemplateUiState } from '../../../../hooks/useStore';

const ENABLED_STATE = 1;

const TemplateFilters = observer(() => {
  const { t } = useTranslation();
  const { Option } = Select;

  const { templateCategories } = useTemplateStore();
  const { categoryFilter, lastEditedFilter, setFilterValue } = useTemplateUiState();

  const allowedTemplateCategories = templateCategories.filter(
    ({ state }) => state === ENABLED_STATE
  );

  return (
    <div className="FiltersMenu">
      <Select
        id="categoryFilter"
        value={categoryFilter}
        placeholder={t('templates.table.columns.category')}
        allowClear
        onChange={value => setFilterValue('categoryFilter', value)}
      >
        {allowedTemplateCategories.map(category => (
          <Option key={category.id} value={category.id}>
            {category.name}
          </Option>
        ))}
      </Select>
      <CustomInputGroup prefixIcon="calendar">
        <CustomDatePicker
          value={lastEditedFilter}
          placeholder={t('templates.table.columns.lastEdited')}
          suffixIcon={<DownOutlined />}
          onChange={value => setFilterValue('lastEditedFilter', value)}
        />
      </CustomInputGroup>
    </div>
  );
});

export default TemplateFilters;
