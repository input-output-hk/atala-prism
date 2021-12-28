import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Button, Select } from 'antd';
import { SortAscendingOutlined, SortDescendingOutlined } from '@ant-design/icons';
import { SORTING_DIRECTIONS } from '../../../../helpers/constants';

import './_style.scss';

const { Option } = Select;
const { ascending } = SORTING_DIRECTIONS;

const SortControls = ({ options, sortDirection, toggleSortDirection, setSortingBy }) => {
  const { t } = useTranslation();

  const sortAscending = sortDirection === ascending;

  return (
    <div className="SortControlContainer">
      <Button
        className="TableOptionButton no-border"
        onClick={toggleSortDirection}
        icon={
          sortAscending ? (
            <SortAscendingOutlined style={{ fontSize: '16px' }} />
          ) : (
            <SortDescendingOutlined style={{ fontSize: '16px' }} />
          )
        }
      />
      <Select
        placeholder={t('actions.sortBy')}
        optionLabelProp="labelWhenSelected"
        onChange={setSortingBy}
        bordered={false}
        dropdownMatchSelectWidth={false}
        allowClear
      >
        {options.map(op => (
          <Option value={op.key} labelWhenSelected={t('actions.sortingBy', { column: op.label })}>
            {op.label}
          </Option>
        ))}
      </Select>
    </div>
  );
};

SortControls.defaultProps = {
  options: []
};

SortControls.propTypes = {
  options: PropTypes.arrayOf(
    PropTypes.shape({
      key: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired
    })
  ),
  sortDirection: PropTypes.string.isRequired,
  toggleSortDirection: PropTypes.func.isRequired,
  setSortingBy: PropTypes.func.isRequired
};

export default SortControls;
