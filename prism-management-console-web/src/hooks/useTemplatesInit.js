import { useContext, useEffect } from 'react';
import { PrismStoreContext } from '../stores/domain/PrismStore';
import { UiStateContext } from '../stores/ui/UiState';

export const useTemplatesInit = () => {
  const { templateStore } = useContext(PrismStoreContext);
  const { templateUiState } = useContext(UiStateContext);
  const { fetchTemplates, fetchCategories } = templateStore;
  const { resetState } = templateUiState;

  useEffect(() => {
    resetState();
    fetchTemplates();
    fetchCategories();
  }, [resetState, fetchTemplates, fetchCategories]);

  return undefined;
};
