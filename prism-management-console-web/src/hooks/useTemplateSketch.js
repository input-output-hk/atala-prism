import { useContext, useEffect } from 'react';
import { GlobalStateContext } from '../stores';

export const useTemplateSketch = ({ reset } = {}) => {
  const { templateSketchState } = useContext(GlobalStateContext);

  const { resetSketch } = templateSketchState;

  useEffect(() => {
    if (reset) resetSketch();
  }, [reset, resetSketch]);

  return templateSketchState;
};
