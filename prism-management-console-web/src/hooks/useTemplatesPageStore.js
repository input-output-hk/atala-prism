import { useContext } from 'react';
import { GlobalStateContext } from '../stores';

/**
 *
 * @returns {TemplatesPageStore}
 */
export const useTemplatesPageStore = () => {
  const { templatesPageStore } = useContext(GlobalStateContext);

  return templatesPageStore;
};

/**
 *
 * @returns {TemplatesByCategoryStore}
 */
export const useTemplatesByCategoryStore = () => {
  const { templatesByCategoryStore } = useContext(GlobalStateContext);

  return templatesByCategoryStore;
};
