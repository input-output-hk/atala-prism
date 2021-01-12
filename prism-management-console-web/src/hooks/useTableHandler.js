import { useState, useEffect } from 'react';
import { filterByManyFields } from '../helpers/filterHelpers';

export const useTableHandler = (list, filterKeys) => {
  const [filterValue, setFilterValue] = useState('');
  const [filteredList, setFilteredList] = useState([]);
  const [selectedItems, setSelectedItems] = useState([]);

  useEffect(() => {
    setFilteredList(filterByManyFields(list, filterValue, filterKeys));
  }, [list, filterValue]);

  return [filteredList, setFilterValue, selectedItems, setSelectedItems];
};
