import React from 'react';
import PropTypes from 'prop-types';
import { Select } from 'antd';
import { useTranslation } from 'react-i18next';
import { DownOutlined } from '@ant-design/icons';
import CustomInputGroup from '../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import CustomDatePicker from '../../../common/Atoms/CustomDatePicker/CustomDatePicker';
import { templateCategoryShape, templateFiltersShape } from '../../../../helpers/propShapes';

const ENABLED_STATE = 1;

const TemplateFilters = ({ filterProps, templateCategories }) => {
  const { t } = useTranslation();
  const { Option } = Select;

  const allowedTemplateCategories = templateCategories.filter(
    ({ state }) => state === ENABLED_STATE
  );

  return (
    <div className="FiltersMenu">
      <Select
        id="categoryFilter"
        value={filterProps.category}
        placeholder={t('templates.table.columns.category')}
        allowClear
        onChange={filterProps.setCategory}
      >
        {allowedTemplateCategories.map(category => (
          <Option key={category.id} value={category.id}>
            {category.name}
          </Option>
        ))}
      </Select>
      <CustomInputGroup prefixIcon="calendar">
        <CustomDatePicker
          placeholder={t('templates.table.columns.lastEdited')}
          suffixIcon={<DownOutlined />}
          onChange={(_, dateString) => filterProps.setLastEdited(dateString)}
        />
      </CustomInputGroup>
    </div>
  );
};
TemplateFilters.propTypes = {
  filterProps: PropTypes.shape(templateFiltersShape).isRequired,
  templateCategories: PropTypes.arrayOf(templateCategoryShape).isRequired
};

export default TemplateFilters;
