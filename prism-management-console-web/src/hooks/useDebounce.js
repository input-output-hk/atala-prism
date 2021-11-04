import { useCallback } from 'react';
import { debounce } from 'lodash';
import { SEARCH_DELAY_MS } from '../helpers/constants';

/**
 * TODO: Let's revisit later this ESLint warning for unknown dependencies. It might hide some other
 * issues in other places where useDebounce is used, because dependency warnings are not possible
 * this way.
 *
 * It looks like useCallback must be used outside of useDebounce.
 *
 * Some links with hints how to solve it:
 * https://kyleshevlin.com/debounce-and-throttle-callbacks-with-react-hooks
 * https://github.com/xnimorz/use-debounce (see useDebouncedCallback)
 * https://github.com/facebook/react/issues/19240
 */
export function useDebounce(functionToExecute, dependencies = [], delay = SEARCH_DELAY_MS) {
  // eslint-disable-next-line
  return useCallback(debounce(functionToExecute, delay), dependencies);
}
