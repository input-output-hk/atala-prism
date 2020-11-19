import { useCallback } from 'react';
import { debounce } from 'lodash';
import { SEARCH_DELAY_MS } from '../helpers/constants';

export const useDebounce = functionToExecute => {
  const debouncedFunction = useCallback(debounce(functionToExecute, SEARCH_DELAY_MS), []);

  return debouncedFunction;
};
