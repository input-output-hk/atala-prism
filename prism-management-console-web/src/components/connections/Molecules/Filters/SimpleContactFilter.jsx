import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import SearchBar from '../../../common/Atoms/SearchBar/SearchBar';
import './_style.scss';

const SimpleContactFilter = observer(({ filterSortingProps, localStateFilter }) => {
  const { t } = useTranslation();

  const { setFilterValue: setGlobalFilterValue } = filterSortingProps;

  const searchText = localStateFilter?.value;

  const setFilterValue = localStateFilter ? localStateFilter.setValue : setGlobalFilterValue;

  return (
    <div className="FilterControls">
      <div className="ContactFilters">
        <SearchBar
          searchText={searchText}
          setSearchText={textValue => setFilterValue('textFilter', textValue)}
          placeholder={t('contacts.filters.search')}
        />
      </div>
    </div>
  );
});

SimpleContactFilter.defaultProps = {
  filterSortingProps: {},
  localStateFilter: null
};

SimpleContactFilter.propTypes = {
  filterSortingProps: PropTypes.shape({
    setFilterValue: PropTypes.func
  }),
  localStateFilter: PropTypes.shape({
    setValue: PropTypes.func
  })
};

export default SimpleContactFilter;
