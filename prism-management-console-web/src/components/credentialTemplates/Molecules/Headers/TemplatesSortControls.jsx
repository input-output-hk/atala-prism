import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { TEMPLATES_SORTING_KEYS } from '../../../../helpers/constants';
import SortControls from '../../../common/Molecules/Sorting/SortControls';

const TemplatesSortControls = ({ filterSortingProps }) => {
  const { t } = useTranslation();

  const sortingOptions = Object.keys(TEMPLATES_SORTING_KEYS).map(column => ({
    key: column,
    label: t(`templates.table.columns.${column}`)
  }));

  return <SortControls options={sortingOptions} {...filterSortingProps} />;
};

TemplatesSortControls.propTypes = {
  filterSortingProps: PropTypes.shape({
    sortDirection: PropTypes.string.isRequired,
    toggleSortDirection: PropTypes.func.isRequired,
    sortingBy: PropTypes.string.isRequired,
    setSortingBy: PropTypes.func.isRequired
  }).isRequired
};

export default TemplatesSortControls;
