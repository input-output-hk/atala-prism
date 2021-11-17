import { useContext, useEffect } from 'react';
import { GlobalStateContext } from '../stores';

export const useTemplateStore = ({ fetch } = { fetch: false }) => {
  const { templateStore } = useContext(GlobalStateContext);
  const { fetchTemplates, fetchCategories } = templateStore;

  useEffect(() => {
    if (fetch) {
      fetchTemplates();
      fetchCategories();
    }
  }, [fetch, fetchTemplates, fetchCategories]);

  return templateStore;
};

export const useTemplateUiState = ({ reset } = { reset: false }) => {
  const { templateUiState } = useContext(GlobalStateContext);
  const { resetState } = templateUiState;

  useEffect(() => {
    if (reset) {
      resetState();
    }
  }, [reset, resetState]);

  return templateUiState;
};
