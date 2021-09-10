import { useContext, useEffect } from 'react';
import { Form } from 'antd';
import { UiStateContext } from '../stores/ui/UiState';

export const useTemplateSketch = ({ reset } = {}) => {
  const { templateSketchState } = useContext(UiStateContext);
  const { resetSketch, setForm } = templateSketchState;

  const [form] = Form.useForm();
  setForm(form);

  useEffect(() => {
    if (reset) resetSketch();
  }, [reset, resetSketch]);

  return templateSketchState;
};
