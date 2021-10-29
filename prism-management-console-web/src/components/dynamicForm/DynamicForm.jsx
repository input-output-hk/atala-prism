import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import { Form, Row } from 'antd';
import { DynamicFormContext } from '../providers/DynamicFormProvider';
import IndividualForm from './IndividualForm/IndividualForm';
import { DEFAULT_WIDTH_INPUT, IMPORT_CONTACTS } from '../../helpers/constants';
import { columnShape, importUseCasePropType, skeletonShape } from '../../helpers/propShapes';
import { useDebounce } from '../../hooks/useDebounce';

import './_style.scss';

const DynamicForm = ({ columns, skeleton, initialValues, useCase }) => {
  const { form, removeEntity, formName, checkValidation } = useContext(DynamicFormContext);

  const lastColumn = columns.pop();

  const handleValuesChange = useDebounce(checkValidation);

  return (
    <div
      className={`DynamicFormContainer ${
        useCase === IMPORT_CONTACTS ? 'ContactsImport' : 'CredentialsImport'
      }`}
    >
      <Row>
        <Form
          form={form}
          name="dynamic_form"
          className="DynamicForm"
          autoComplete="off"
          onValuesChange={handleValuesChange}
        >
          <Form.List name={formName} initialValue={initialValues}>
            {fields => (
              <>
                <Row className="HeaderRow">
                  {columns.map(col => (
                    <span style={{ width: col.width || DEFAULT_WIDTH_INPUT }} className="HeaderCol">
                      {col.label}
                    </span>
                  ))}
                  <span className="HeaderCol">{lastColumn.label}</span>
                </Row>
                {fields.map((field, index) => (
                  <IndividualForm
                    field={field}
                    skeleton={skeleton}
                    columns={columns}
                    onRemove={() => removeEntity(field.key, index)}
                    useCase={useCase}
                  />
                ))}
              </>
            )}
          </Form.List>
        </Form>
      </Row>
    </div>
  );
};

DynamicForm.defaultProps = {};

DynamicForm.propTypes = {
  initialValues: PropTypes.shape({}).isRequired,
  columns: columnShape.isRequired,
  skeleton: skeletonShape.isRequired,
  useCase: importUseCasePropType.isRequired
};

export default DynamicForm;
