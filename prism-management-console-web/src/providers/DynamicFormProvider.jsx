import React, { createContext, useState } from 'react';
import PropTypes from 'prop-types';
import { Form } from 'antd';

/*
  A <DynamicForm /> is composed by one or more <IndividualForm />, so:
  This provider was created with the intention to manage DynamicForm's and make easy to instance them.
  Allows makes operations on it through multiple components, without the need to inherit properties,
  which also has the benefit of no limitations on where the UI elements that interact with it must be.
  Official docs: https://ant.design/components/form/#components-form-demo-dynamic-form-item
  Note: antd docs haven't instructions for manage DynamicForm outside of it (for example add/remove operations)
  for that the next issue are useful: https://github.com/ant-design/ant-design/issues/16404
  Note 2: initially created for avoid the tabkey switching between inputs error
*/

const DynamicFormContext = createContext({});

const DynamicFormProvider = ({ children, formName }) => {
  const [form] = Form.useForm();
  const [saveFormProviderAvailable, setSaveFormProviderAvailable] = useState(false);

  // verifies if rules defined to the form are fulfilled and enables her saving if true
  const checkValidation = async () => {
    try {
      await form.validateFields();
      setSaveFormProviderAvailable(true);
    } catch ({ errorFields }) {
      setSaveFormProviderAvailable(!errorFields.length);
    }
  };

  // add an empty instance of the form, an <IndividualForm />
  const addEntity = () => {
    const { getFieldValue, setFieldsValue } = form;
    const keys = getFieldValue(formName);
    const nextKeys = keys.concat(keys.length);
    setFieldsValue({ [formName]: nextKeys });
  };

  // removes an instance of the form
  const removeEntity = (targetKey, targetIndex) => {
    const { getFieldValue, setFieldsValue } = form;
    const keys = getFieldValue(formName);
    if (keys.length === 1) return;
    setFieldsValue({
      [formName]: keys.filter((key, index) => key !== targetKey && index !== targetIndex)
    });
  };

  return (
    <DynamicFormContext.Provider
      value={{
        form,
        formName,
        addEntity,
        removeEntity,
        saveFormProviderAvailable,
        checkValidation
      }}
    >
      {children}
    </DynamicFormContext.Provider>
  );
};

DynamicFormProvider.propTypes = {
  children: PropTypes.node.isRequired,
  formName: PropTypes.string.isRequired
};

export { DynamicFormContext, DynamicFormProvider };
