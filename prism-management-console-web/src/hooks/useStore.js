import { useContext, useEffect } from 'react';
import { PrismStoreContext } from '../stores/domain/PrismStore';
import { UiStateContext } from '../stores/ui/UiState';

export const useTemplateStore = ({ fetch } = {}) => {
  const { templateStore } = useContext(PrismStoreContext);
  const { fetchTemplates, fetchCategories } = templateStore;

  useEffect(() => {
    if (fetch) {
      fetchTemplates();
      fetchCategories();
    }
  }, [fetch, fetchTemplates, fetchCategories]);

  return templateStore;
};

export const useTemplateUiState = ({ reset } = {}) => {
  const { templateUiState } = useContext(UiStateContext);
  const { resetState } = templateUiState;

  useEffect(() => {
    if (reset) {
      resetState();
    }
  }, [reset, resetState]);

  return templateUiState;
};
