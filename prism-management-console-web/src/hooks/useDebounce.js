import { useCallback } from 'react';
import { debounce } from 'lodash';
import { SEARCH_DELAY_MS } from '../helpers/constants';

export const useDebounce = (functionToExecute, dependencies = [], delay = SEARCH_DELAY_MS) =>
  useCallback(debounce(functionToExecute, delay), dependencies);
