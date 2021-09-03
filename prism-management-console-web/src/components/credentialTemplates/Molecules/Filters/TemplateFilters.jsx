import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import { Select } from 'antd';
import { useTranslation } from 'react-i18next';
import { DownOutlined } from '@ant-design/icons';
import { observer } from 'mobx-react-lite';
import CustomInputGroup from '../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import CustomDatePicker from '../../../common/Atoms/CustomDatePicker/CustomDatePicker';
import { templateCategoryShape } from '../../../../helpers/propShapes';
import { UiStateContext } from '../../../../stores/ui/UiState';

const ENABLED_STATE = 1;

const TemplateFilters = observer(({ templateCategories }) => {
  const { t } = useTranslation();
  const { Option } = Select;
  const { templateUiState } = useContext(UiStateContext);
  const { categoryFilter, lastEditedFilter, setFilterValue } = templateUiState;

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

TemplateFilters.propTypes = {
  templateCategories: PropTypes.arrayOf(templateCategoryShape).isRequired
};

export default TemplateFilters;
