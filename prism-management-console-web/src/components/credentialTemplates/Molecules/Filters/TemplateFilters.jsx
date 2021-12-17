import React from 'react';
import { PropTypes } from 'prop-types';
import { Select } from 'antd';
import { useTranslation } from 'react-i18next';
import { DownOutlined } from '@ant-design/icons';
import CustomInputGroup from '../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import CustomDatePicker from '../../../common/Atoms/CustomDatePicker/CustomDatePicker';
import { templateCategoryShape } from '../../../../helpers/propShapes';
import { CREDENTIAL_TYPE_CATEGORY_STATUSES } from '../../../../helpers/constants';

const { READY } = CREDENTIAL_TYPE_CATEGORY_STATUSES;

const TemplateFilters = ({
  templateCategories,
  categoryFilter,
  lastEditedFilter,
  setFilterValue,
  showDateFilter
}) => {
  const { t } = useTranslation();
  const { Option } = Select;

  const allowedTemplateCategories = templateCategories.filter(({ state }) => state === READY);

  return (
    <div className="FiltersMenu">
      <div className="selectLabel">
        {/* TODO: add i18n */}
        <p>Filter by Category</p>
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
      </div>
      {showDateFilter && (
        <CustomInputGroup prefixIcon="calendar">
          <CustomDatePicker
            value={lastEditedFilter}
            placeholder={t('templates.table.columns.lastEdited')}
            suffixIcon={<DownOutlined />}
            onChange={value => setFilterValue('lastEditedFilter', value)}
          />
        </CustomInputGroup>
      )}
    </div>
  );
};

TemplateFilters.defaultProps = {
  showDateFilter: true
};

TemplateFilters.propTypes = {
  templateCategories: PropTypes.arrayOf(templateCategoryShape).isRequired,
  categoryFilter: PropTypes.string.isRequired,
  lastEditedFilter: PropTypes.string.isRequired,
  setFilterValue: PropTypes.func.isRequired,
  showDateFilter: PropTypes.bool
};

export default TemplateFilters;
