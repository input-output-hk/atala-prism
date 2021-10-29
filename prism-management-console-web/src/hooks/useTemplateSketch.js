import { useContext, useEffect } from 'react';
import { UiStateContext } from '../stores/ui/UiState';

export const useTemplateSketch = ({ reset } = {}) => {
  const { templateSketchState } = useContext(UiStateContext);

  const { resetSketch } = templateSketchState;

  useEffect(() => {
    if (reset) resetSketch();
  }, [reset, resetSketch]);

  return templateSketchState;
};
