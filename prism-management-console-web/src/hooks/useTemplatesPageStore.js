import { useContext } from 'react';
import { GlobalStateContext } from '../stores';

/**
 * 
 * @returns {TemplatesPageStore}
 */
export const useTemplatePageStore = () => {
  const { templatesPageStore } = useContext(GlobalStateContext);

  return templatesPageStore;
};
